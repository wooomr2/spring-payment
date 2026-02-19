package woo.paymentservice.payment.application.port.`in`

import reactor.core.publisher.Mono
import woo.paymentservice.payment.domain.PaymentConfirmResult

interface PaymentConfirmUseCase {

    fun confirm(command: PaymentConfirmCommand): Mono<PaymentConfirmResult>
}