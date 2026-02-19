package woo.paymentservice.payment.adapter.out.persistent.repository.r2dbc

import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import woo.paymentservice.common.util.objectMapper
import woo.paymentservice.payment.adapter.out.persistent.repository.PaymentOutboxRepository
import woo.paymentservice.payment.adapter.out.persistent.util.MySQLDateTimeFormatter
import woo.paymentservice.payment.adapter.out.stream.util.PartitionKeyUtil
import woo.paymentservice.payment.application.port.out.PaymentStatusUpdateCommand
import woo.paymentservice.payment.application.stream.PaymentEventMessage
import woo.paymentservice.payment.application.stream.PaymentEventMessageType
import woo.paymentservice.payment.domain.PaymentStatus
import java.time.LocalDateTime
import com.fasterxml.jackson.module.kotlin.readValue

@Repository
class R2DBCPaymentOutboxRepository(
    private val databaseClient: DatabaseClient,
    private val partitionKeyUtil: PartitionKeyUtil,
) : PaymentOutboxRepository {

    override fun insertOutbox(command: PaymentStatusUpdateCommand): Mono<PaymentEventMessage> {
        require(command.status == PaymentStatus.SUCCESS)

        val paymentEventMessage = createPaymentEventMessage(command)

        return databaseClient.sql(INSERT_OUTBOX_QUERY)
            .bind(
                "idempotencyKey",
                paymentEventMessage.payload["orderId"] ?: throw IllegalStateException("orderId is required")
            )
            .bind("type", paymentEventMessage.type.name)
            .bind("partitionKey", paymentEventMessage.metadata["partitionKey"] ?: 0)
            .bind("payload", objectMapper.writeValueAsString(paymentEventMessage.payload))
            .bind("metadata", objectMapper.writeValueAsString(paymentEventMessage.metadata))
            .fetch()
            .rowsUpdated()
            .thenReturn(paymentEventMessage)
    }

    override fun markMessageAsSent(idempodencyKey: String, type: PaymentEventMessageType): Mono<Boolean> {
        return databaseClient.sql(UPDATE_OUTBOX_MESSAGE_AS_SENT_QUERY)
            .bind("idempotencyKey", idempodencyKey)
            .bind("type", type.name)
            .fetch()
            .rowsUpdated()
            .map { it > 0 }
    }

    override fun markMessageAsFailed(idempodencyKey: String, type: PaymentEventMessageType): Mono<Boolean> {
        return databaseClient.sql(UPDATE_OUTBOX_MESSAGE_AS_FAILED_QUERY)
            .bind("idempotencyKey", idempodencyKey)
            .bind("type", type.name)
            .fetch()
            .rowsUpdated()
            .map { it > 0 }
    }

    override fun getPendingOutboxes(): Flux<PaymentEventMessage> {
        return databaseClient.sql(SELECT_PENDING_PAYMENT_OUTBOX_QUERY)
            .bind("createdAt", LocalDateTime.now().format(MySQLDateTimeFormatter))
            .fetch()
            .all()
            .map {
                PaymentEventMessage(
                    type = PaymentEventMessageType.PAYMENT_CONFIRMATION_SUCCESS,
                    payload = objectMapper.readValue<Map<String, Any>>(it["payload"] as String),
                    metadata = objectMapper.readValue<Map<String, Any>>(it["metadata"] as String)
                )
            }
    }

    private fun createPaymentEventMessage(command: PaymentStatusUpdateCommand): PaymentEventMessage {
        return PaymentEventMessage(
            type = PaymentEventMessageType.PAYMENT_CONFIRMATION_SUCCESS,
            payload = mapOf(
                "orderId" to command.orderId,
            ),
            metadata = mapOf(
                "partitionKey" to partitionKeyUtil.createPartitionKey(command.orderId.hashCode())
            )
        )
    }

    companion object {
        val INSERT_OUTBOX_QUERY = """
            INSERT INTO outboxes (idempotency_key, type, partition_key, payload, metadata) 
            VALUES (:idempotencyKey, :type, :partitionKey, :payload, :metadata)
        """.trimIndent()

        val UPDATE_OUTBOX_MESSAGE_AS_SENT_QUERY = """
            UPDATE outboxes 
            SET status = 'SUCCESS' 
            WHERE idempotency_key = :idempotencyKey
             AND type = :type
        """.trimIndent()

        val UPDATE_OUTBOX_MESSAGE_AS_FAILED_QUERY = """
            UPDATE outboxes 
            SET status = 'FAILURE' 
            WHERE idempotency_key = :idempotencyKey
             AND type = :type
        """.trimIndent()

        val SELECT_PENDING_PAYMENT_OUTBOX_QUERY = """
            SELECT *
            FROM outboxes
            WHERE (status = 'INIT' OR status = 'FAILURE')
            AND created_at <= :createdAt - INTERVAL 1 MINUTE
            AND type = 'PAYMENT_CONFIRMATION_SUCCESS'
        """.trimIndent()
    }
}