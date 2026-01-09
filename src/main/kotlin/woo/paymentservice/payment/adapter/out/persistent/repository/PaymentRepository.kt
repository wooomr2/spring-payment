package woo.paymentservice.payment.adapter.out.persistent.repository

import reactor.core.publisher.Mono
import woo.paymentservice.payment.domain.PaymentEvent

interface PaymentRepository {

    fun save(paymentEvent: PaymentEvent): Mono<Void>
}