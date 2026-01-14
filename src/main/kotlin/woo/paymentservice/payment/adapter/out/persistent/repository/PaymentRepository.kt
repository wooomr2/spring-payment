package woo.paymentservice.payment.adapter.out.persistent.repository

import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import woo.paymentservice.payment.domain.PaymentEvent
import woo.paymentservice.payment.domain.PendingPaymentEvent
import woo.paymentservice.payment.domain.PendingPaymentOrder

interface PaymentRepository {

    fun save(paymentEvent: PaymentEvent): Mono<Void>

    fun getPendingPayments(): Flux<PendingPaymentEvent>
}