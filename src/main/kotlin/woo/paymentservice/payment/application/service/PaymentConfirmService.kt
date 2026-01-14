package woo.paymentservice.payment.application.service

import io.netty.handler.timeout.TimeoutException
import reactor.core.publisher.Mono
import woo.paymentservice.common.annotation.UseCase
import woo.paymentservice.payment.adapter.out.persistent.exception.PaymentAlreadyProcessedException
import woo.paymentservice.payment.adapter.out.persistent.exception.PaymentValidationException
import woo.paymentservice.payment.adapter.out.web.toss.exception.PSPConfirmationException
import woo.paymentservice.payment.application.port.`in`.PaymentConfirmCommand
import woo.paymentservice.payment.application.port.`in`.PaymentConfirmUseCase
import woo.paymentservice.payment.application.port.out.PaymentExecutorPort
import woo.paymentservice.payment.application.port.out.PaymentStatusUpdateCommand
import woo.paymentservice.payment.application.port.out.PaymentStatusUpdatePort
import woo.paymentservice.payment.application.port.out.PaymentValidationPort
import woo.paymentservice.payment.domain.PaymentConfirmResult
import woo.paymentservice.payment.domain.PaymentFailure
import woo.paymentservice.payment.domain.PaymentStatus

@UseCase
class PaymentConfirmService(
    private val paymentStatusUpdatePort: PaymentStatusUpdatePort,
    private val paymentValidationPort: PaymentValidationPort,
    private val paymentExecutorPort: PaymentExecutorPort,
) : PaymentConfirmUseCase {

    override fun confirm(command: PaymentConfirmCommand): Mono<PaymentConfirmResult> {
        return paymentStatusUpdatePort.updatePaymentStatusToExecuting(command.orderId, command.paymentKey)
            .filterWhen { paymentValidationPort.isValid(command.orderId, command.amount) }
            .flatMap { paymentExecutorPort.execute(command) }
            .flatMap {
                paymentStatusUpdatePort.updatePaymentStatus(
                    PaymentStatusUpdateCommand(
                        paymentKey = it.paymentKey,
                        orderId = it.orderId,
                        status = it.paymentStatus(),
                        extraDetails = it.extraDetails,
                        failure = it.failure
                    )
                ).thenReturn(it)

            }
            .map {
                PaymentConfirmResult(
                    status = it.paymentStatus(),
                    failure = it.failure
                )
            }
            .onErrorResume { error -> handlePaymentError(error, command) }
    }

    private fun handlePaymentError(error: Throwable, command: PaymentConfirmCommand): Mono<PaymentConfirmResult> {
        val (status, failure) = when (error) {
            is PSPConfirmationException ->
                Pair(
                    error.paymentStatus(),
                    PaymentFailure(
                        code = error.errorCode,
                        message = error.errorMessage
                    )
                )

            is PaymentValidationException ->
                Pair(
                    PaymentStatus.FAILURE,
                    PaymentFailure(
                        code = error::class.simpleName ?: "",
                        message = error.message ?: ""
                    )
                )

            is PaymentAlreadyProcessedException ->
                return Mono.just(
                    PaymentConfirmResult(
                        status = error.status,
                        failure = PaymentFailure(
                            code = error::class.simpleName ?: "",
                            message = error.message ?: ""
                        )
                    )
                )

            is TimeoutException ->
                Pair(
                    PaymentStatus.UNKNOWN,
                    PaymentFailure(
                        code = error::class.simpleName ?: "",
                        message = error.message ?: ""
                    )
                )

            else ->
                Pair(
                    PaymentStatus.UNKNOWN,
                    PaymentFailure(
                        code = error::class.simpleName ?: "",
                        message = error.message ?: ""
                    )
                )
        }

        val paymentStatusUpdateCommand = PaymentStatusUpdateCommand(
            paymentKey = command.paymentKey,
            orderId = command.orderId,
            status = status,
            failure = failure
        )

        return paymentStatusUpdatePort.updatePaymentStatus(paymentStatusUpdateCommand)
            .map { PaymentConfirmResult(status, failure) }
    }
}