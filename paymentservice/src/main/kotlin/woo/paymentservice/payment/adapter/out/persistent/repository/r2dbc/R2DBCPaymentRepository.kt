package woo.paymentservice.payment.adapter.out.persistent.repository.r2dbc

import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Repository
import org.springframework.transaction.reactive.TransactionalOperator
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import woo.paymentservice.payment.adapter.out.persistent.repository.PaymentRepository
import woo.paymentservice.payment.adapter.out.persistent.util.MySQLDateTimeFormatter
import woo.paymentservice.payment.domain.PaymentEvent
import woo.paymentservice.payment.domain.PaymentStatus
import woo.paymentservice.payment.domain.PendingPaymentEvent
import woo.paymentservice.payment.domain.PendingPaymentOrder
import java.math.BigInteger
import java.time.LocalDateTime

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

    override fun getPendingPayments(): Flux<PendingPaymentEvent> {
        return databaseClient.sql(SELECT_PENDING_PAYMENT_QUERY)
            .bind("updatedAt", LocalDateTime.now().format(MySQLDateTimeFormatter))
            .fetch()
            .all()
            .groupBy { it["payment_event_id"] as Long }
            .flatMap { groupedFlux ->
                groupedFlux.collectList().map { results ->
                    PendingPaymentEvent(
                        paymentEventId = groupedFlux.key(),
                        paymentKey = results.first()["payment_key"] as String,
                        orderId = results.first()["order_id"] as String,
                        pendingPaymentOrders = results.map {
                            PendingPaymentOrder(
                                paymentOrderId = it["payment_order_id"] as Long,
                                status = PaymentStatus.get(it["payment_order_status"] as String),
                                amount = (it["amount"] as Number).toLong(),
                                failCount = it["fail_count"] as Byte,
                                threshold = it["threshold"] as Byte
                            )
                        }
                    )
                }
            }
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

        val SELECT_PENDING_PAYMENT_QUERY = """
            SELECT pe.id as payment_event_id, pe.payment_key, pe.order_id,
             po.id as payment_order_id, po.payment_order_status, po.amount, po.fail_count, po.threshold
            FROM payment_events pe
            INNER JOIN payment_orders po ON po.payment_event_id = pe.id
            WHERE (po.payment_order_status = 'UNKNOWN' OR (po.payment_order_status = 'EXECUTING' AND po.updated_at <= :updatedAt - INTERVAL 3 MINUTE))
            AND po.fail_count < po.threshold
            LIMIT 10
        """.trimIndent()
    }
}