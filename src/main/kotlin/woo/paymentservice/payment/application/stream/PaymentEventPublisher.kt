package woo.paymentservice.payment.application.stream

import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import org.springframework.transaction.reactive.TransactionalEventPublisher
import reactor.core.publisher.Mono

@Component
class PaymentEventPublisher(
    publisher: ApplicationEventPublisher
) {
    /**
     *  db commit 후에 이벤트가 발행되도록 TransactionalEventPublisher 사용
     */
    private val transactionalEventPublisher = TransactionalEventPublisher(publisher)

    fun publishEvent(paymentEventMessage: PaymentEventMessage): Mono<PaymentEventMessage> {
        return transactionalEventPublisher.publishEvent(paymentEventMessage)
            .thenReturn(paymentEventMessage)
    }
}