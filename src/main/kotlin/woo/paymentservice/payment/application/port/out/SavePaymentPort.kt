package woo.paymentservice.payment.application.port.out

import reactor.core.publisher.Mono
import woo.paymentservice.payment.domain.PaymentEvent

interface SavePaymentPort {

    fun save(paymentEvent: PaymentEvent): Mono<Void>
}