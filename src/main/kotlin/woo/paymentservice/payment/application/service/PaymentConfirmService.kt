package woo.paymentservice.payment.application.service

import reactor.core.publisher.Mono
import woo.paymentservice.common.annotation.UseCase
import woo.paymentservice.payment.application.port.`in`.PaymentConfirmCommand
import woo.paymentservice.payment.application.port.`in`.PaymentConfirmUseCase
import woo.paymentservice.payment.application.port.out.PaymentExecutorPort
import woo.paymentservice.payment.application.port.out.PaymentStatusUpdateCommand
import woo.paymentservice.payment.application.port.out.PaymentStatusUpdatePort
import woo.paymentservice.payment.application.port.out.PaymentValidationPort
import woo.paymentservice.payment.domain.PaymentConfirmResult

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
    }
}