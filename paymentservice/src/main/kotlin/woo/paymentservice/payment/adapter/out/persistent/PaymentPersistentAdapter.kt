package woo.paymentservice.payment.adapter.out.persistent

import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import woo.paymentservice.common.annotation.PersistentAdapter
import woo.paymentservice.payment.adapter.out.persistent.repository.PaymentOutboxRepository
import woo.paymentservice.payment.adapter.out.persistent.repository.PaymentRepository
import woo.paymentservice.payment.adapter.out.persistent.repository.PaymentStatusUpdateRepository
import woo.paymentservice.payment.adapter.out.persistent.repository.PaymentValidationRepository
import woo.paymentservice.payment.application.port.out.*
import woo.paymentservice.payment.application.stream.PaymentEventMessage
import woo.paymentservice.payment.domain.PaymentEvent
import woo.paymentservice.payment.domain.PendingPaymentEvent

@PersistentAdapter
class PaymentPersistentAdapter(
    private val paymentRepository: PaymentRepository,
    private val paymentStatusUpdateRepository: PaymentStatusUpdateRepository,
    private val paymentValidationRepository: PaymentValidationRepository,
    private val paymentOutboxRepository: PaymentOutboxRepository
) : SavePaymentPort, PaymentStatusUpdatePort, PaymentValidationPort, LoadPendingPaymentPort,
    LoadPendingPaymentEventMessagePort {

    override fun save(paymentEvent: PaymentEvent): Mono<Void> {
        return paymentRepository.save(paymentEvent)
    }

    override fun updatePaymentStatusToExecuting(orderId: String, paymentKey: String): Mono<Boolean> {
        return paymentStatusUpdateRepository.updatePaymentStatusToExecuting(orderId, paymentKey)
    }

    override fun isValid(orderId: String, amount: Long): Mono<Boolean> {
        return paymentValidationRepository.isValid(orderId, amount)
    }

    override fun updatePaymentStatus(command: PaymentStatusUpdateCommand): Mono<Boolean> {
        return paymentStatusUpdateRepository.updatePaymentStatus(command)
    }

    override fun getPendingPayments(): Flux<PendingPaymentEvent> {
        return paymentRepository.getPendingPayments()
    }

    override fun getPendingPaymentEventMessage(): Flux<PaymentEventMessage> {
        return paymentOutboxRepository.getPendingOutboxes()
    }
}