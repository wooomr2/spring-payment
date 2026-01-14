package woo.paymentservice.payment.application.service

import io.netty.handler.timeout.TimeoutException
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import woo.paymentservice.payment.adapter.out.persistent.exception.PaymentAlreadyProcessedException
import woo.paymentservice.payment.adapter.out.persistent.exception.PaymentValidationException
import woo.paymentservice.payment.adapter.out.web.toss.exception.PSPConfirmationException
import woo.paymentservice.payment.application.port.`in`.PaymentConfirmCommand
import woo.paymentservice.payment.application.port.out.PaymentStatusUpdateCommand
import woo.paymentservice.payment.application.port.out.PaymentStatusUpdatePort
import woo.paymentservice.payment.domain.PaymentConfirmResult
import woo.paymentservice.payment.domain.PaymentFailure
import woo.paymentservice.payment.domain.PaymentStatus

@Service
class PaymentErrorHandler(
    private val paymentStatusUpdatePort: PaymentStatusUpdatePort
) {

    fun handlePaymentConfirmError(error: Throwable, command: PaymentConfirmCommand): Mono<PaymentConfirmResult> {
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