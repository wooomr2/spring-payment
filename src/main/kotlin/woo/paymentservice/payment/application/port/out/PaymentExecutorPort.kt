package woo.paymentservice.payment.application.port.out

import reactor.core.publisher.Mono
import woo.paymentservice.payment.application.port.`in`.PaymentConfirmCommand
import woo.paymentservice.payment.domain.PaymentExecutionResult

interface PaymentExecutorPort {

    fun execute(command: PaymentConfirmCommand): Mono<PaymentExecutionResult>
}