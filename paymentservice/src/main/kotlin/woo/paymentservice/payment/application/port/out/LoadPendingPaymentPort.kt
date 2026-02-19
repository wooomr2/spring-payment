package woo.paymentservice.payment.application.port.out

import reactor.core.publisher.Flux
import woo.paymentservice.payment.domain.PendingPaymentEvent

interface LoadPendingPaymentPort {

    fun getPendingPayments(): Flux<PendingPaymentEvent>
}