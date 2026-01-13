package woo.paymentservice.payment.adapter.out.persistent

import reactor.core.publisher.Mono
import woo.paymentservice.common.annotation.PersistentAdapter
import woo.paymentservice.payment.adapter.out.persistent.repository.PaymentRepository
import woo.paymentservice.payment.adapter.out.persistent.repository.PaymentStatusUpdateRepository
import woo.paymentservice.payment.adapter.out.persistent.repository.PaymentValidationRepository
import woo.paymentservice.payment.application.port.out.PaymentStatusUpdateCommand
import woo.paymentservice.payment.application.port.out.PaymentStatusUpdatePort
import woo.paymentservice.payment.application.port.out.PaymentValidationPort
import woo.paymentservice.payment.application.port.out.SavePaymentPort
import woo.paymentservice.payment.domain.PaymentEvent

@PersistentAdapter
class PaymentPersistentAdapter(
    private val paymentRepository: PaymentRepository,
    private val paymentStatusUpdateRepository: PaymentStatusUpdateRepository,
    private val paymentValidationRepository: PaymentValidationRepository
) : SavePaymentPort, PaymentStatusUpdatePort, PaymentValidationPort {

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
}