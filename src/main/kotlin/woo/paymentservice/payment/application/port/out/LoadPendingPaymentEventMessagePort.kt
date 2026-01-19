package woo.paymentservice.payment.application.port.out

import reactor.core.publisher.Flux
import woo.paymentservice.payment.application.stream.PaymentEventMessage

interface LoadPendingPaymentEventMessagePort {

    fun getPendingPaymentEventMessage(): Flux<PaymentEventMessage>
}