package woo.paymentservice.payment.adapter.out.persistent

import reactor.core.publisher.Mono
import woo.paymentservice.common.annotation.PersistentAdapter
import woo.paymentservice.payment.adapter.out.persistent.repository.PaymentRepository
import woo.paymentservice.payment.application.port.out.SavePaymentPort
import woo.paymentservice.payment.domain.PaymentEvent

@PersistentAdapter
class PaymentPersistentAdapter(
    private val paymentRepository: PaymentRepository
) : SavePaymentPort {
    
    override fun save(paymentEvent: PaymentEvent): Mono<Void> {
        return paymentRepository.save(paymentEvent)
    }
}