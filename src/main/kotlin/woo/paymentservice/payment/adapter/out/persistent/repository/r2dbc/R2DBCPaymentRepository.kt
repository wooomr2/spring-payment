package woo.paymentservice.payment.adapter.out.persistent.repository.r2dbc

import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Repository
import org.springframework.transaction.reactive.TransactionalOperator
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import woo.paymentservice.payment.adapter.out.persistent.repository.PaymentRepository
import woo.paymentservice.payment.domain.PaymentEvent
import java.math.BigInteger

@Repository
class R2DBCPaymentRepository(
    private val databaseClient: DatabaseClient,
    private val transactionalOperator: TransactionalOperator
) : PaymentRepository {

    override fun save(paymentEvent: PaymentEvent): Mono<Void> {
        return insertPaymentEvent(paymentEvent)
            .flatMap { selectPaymentEventId() }
            .flatMap { paymentEventId -> insertPaymentOrders(paymentEvent, paymentEventId) }
            .`as`(transactionalOperator::transactional)
            .then()
    }

    private fun insertPaymentEvent(paymentEvent: PaymentEvent): Mono<Long> {
        return databaseClient.sql(INSERT_PAYMENT_EVENT_QUERY)
            .bind("buyerId", paymentEvent.buyerId)
            .bind("orderName", paymentEvent.orderName)
            .bind("orderId", paymentEvent.orderId)
            .fetch()
            .rowsUpdated()
    }

    private fun selectPaymentEventId(): Mono<Long> = databaseClient.sql(SELECT_LAST_INSERT_ID_QUERY)
        .fetch()
        .first()
        .map { (it["LAST_INSERT_ID()"] as BigInteger).toLong() }

    private fun insertPaymentOrders(paymentEvent: PaymentEvent, paymentEventId: Long): Mono<Long> {

        // TODO("BULK INSERT로 변경 필요")
        val inserts: List<Mono<Long>> = paymentEvent.paymentOrders.map { paymentOrder ->
            databaseClient.sql(INSERT_PAYMENT_ORDER_QUERY)
                .bind("paymentEventId", paymentEventId)
                .bind("sellerId", paymentOrder.sellerId)
                .bind("orderId", paymentOrder.orderId)
                .bind("productId", paymentOrder.productId)
                .bind("amount", paymentOrder.amount)
                .bind("paymentStatus", paymentOrder.paymentStatus)
                .fetch()
                .rowsUpdated()
        }

        return Flux.concat(inserts).reduce(0) { acc, v -> acc + v }
    }

    companion object {
        val INSERT_PAYMENT_EVENT_QUERY = """
            INSERT INTO payment_events (buyer_id, order_name, order_id)
            VALUES (:buyerId, :orderName, :orderId)
        """.trimIndent()

        val SELECT_LAST_INSERT_ID_QUERY = """
            SELECT LAST_INSERT_ID()
        """.trimIndent()

        val INSERT_PAYMENT_ORDER_QUERY = """
            INSERT INTO payment_orders(payment_event_id, seller_id, order_id, product_id, amount, payment_order_status)
            VALUES (:paymentEventId, :sellerId, :orderId, :productId, :amount, :paymentStatus)
        """.trimIndent()
    }
}