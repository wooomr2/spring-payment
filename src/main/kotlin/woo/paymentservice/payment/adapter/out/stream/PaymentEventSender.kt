package woo.paymentservice.payment.adapter.out.stream

import jakarta.annotation.PostConstruct
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.integration.IntegrationMessageHeaderAccessor
import org.springframework.integration.annotation.ServiceActivator
import org.springframework.integration.channel.FluxMessageChannel
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.Message
import org.springframework.messaging.support.MessageBuilder
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks
import reactor.core.scheduler.Schedulers
import reactor.kafka.sender.SenderResult
import woo.paymentservice.common.annotation.StreamAdapter
import woo.paymentservice.common.util.Logger
import woo.paymentservice.payment.adapter.out.persistent.repository.PaymentOutboxRepository
import woo.paymentservice.payment.application.stream.PaymentEventMessage
import woo.paymentservice.payment.application.stream.PaymentEventMessageType
import java.util.function.Supplier

/**
 *  발행된 이벤트를 외부 메세징 시스템(카프카,rabbitMQ..)으로 전송
 * */
@StreamAdapter
@Configuration
class PaymentEventSender(
    private val paymentOutboxRepository: PaymentOutboxRepository
) {

    private val sender = Sinks.many().unicast().onBackpressureBuffer<Message<PaymentEventMessage>>()
    private val sendResult = Sinks.many().unicast().onBackpressureBuffer<SenderResult<String>>()


    @Bean
    fun send(): Supplier<Flux<Message<PaymentEventMessage>>> {
        return Supplier {
            sender.asFlux().onErrorContinue { err, _ ->
                Logger.error("sendEventMessage", err.message ?: "failed to send event message", err)
            }
        }
    }

    @Bean(name = ["payment-result"])
    fun sendResultChannel(): FluxMessageChannel {
        return FluxMessageChannel()
    }

    @ServiceActivator(inputChannel = "payment-result")
    fun receiveSendResult(result: SenderResult<String>) {
        if (result.exception() != null) {
            Logger.error(
                "receiveSendResult",
                "Failed to send message: ${result.exception().message}",
                result.exception()
            )
        }

        sendResult.emitNext(result, Sinks.EmitFailureHandler.FAIL_FAST)
    }

    @PostConstruct
    fun handleSendResult() {
        sendResult.asFlux()
            .flatMap {
                when (it.recordMetadata() != null) {
                    true -> paymentOutboxRepository.markMessageAsSent(
                        it.correlationMetadata(),
                        PaymentEventMessageType.PAYMENT_CONFIRMATION_SUCCESS
                    )

                    false -> paymentOutboxRepository.markMessageAsFailed(
                        it.correlationMetadata(),
                        PaymentEventMessageType.PAYMENT_CONFIRMATION_SUCCESS
                    )
                }
            }
            .onErrorContinue { err, _ ->
                Logger.error(
                    "handleSendResult",
                    err.message ?: "failed to mark the outbox message",
                    err
                )
            }
            .subscribeOn(Schedulers.boundedElastic())
            .subscribe()
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun dispatchAfterCommit(paymentEventMessage: PaymentEventMessage) {
        sender.emitNext(createEventMessage(paymentEventMessage), Sinks.EmitFailureHandler.FAIL_FAST)
    }

    fun dispatch(paymentEventMessage: PaymentEventMessage) {
        sender.emitNext(createEventMessage(paymentEventMessage), Sinks.EmitFailureHandler.FAIL_FAST)
    }

    private fun createEventMessage(paymentEventMessage: PaymentEventMessage): Message<PaymentEventMessage> {
        return MessageBuilder.withPayload(paymentEventMessage)
            .setHeader(IntegrationMessageHeaderAccessor.CORRELATION_ID, paymentEventMessage.payload["orderId"])
            .setHeader(KafkaHeaders.PARTITION, paymentEventMessage.metadata["partitionKey"] ?: 0)
            .build()
    }
}