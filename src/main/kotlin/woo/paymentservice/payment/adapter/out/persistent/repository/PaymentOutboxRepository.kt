package woo.paymentservice.payment.adapter.out.persistent.repository

import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import woo.paymentservice.payment.application.port.out.PaymentStatusUpdateCommand
import woo.paymentservice.payment.application.stream.PaymentEventMessage
import woo.paymentservice.payment.application.stream.PaymentEventMessageType

interface PaymentOutboxRepository {

    fun insertOutbox(command: PaymentStatusUpdateCommand): Mono<PaymentEventMessage>
    fun markMessageAsSent(idempodencyKey: String, type: PaymentEventMessageType): Mono<Boolean>
    fun markMessageAsFailed(idempodencyKey: String, type: PaymentEventMessageType): Mono<Boolean>
    fun getPendingOutboxes(): Flux<PaymentEventMessage>
}