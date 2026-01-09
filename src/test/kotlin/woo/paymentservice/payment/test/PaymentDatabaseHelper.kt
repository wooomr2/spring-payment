package woo.paymentservice.payment.test

import reactor.core.publisher.Mono
import woo.paymentservice.payment.domain.PaymentEvent

interface PaymentDatabaseHelper {

    fun getPayments(orderId: String): PaymentEvent?
    fun clean(): Mono<Void>
}