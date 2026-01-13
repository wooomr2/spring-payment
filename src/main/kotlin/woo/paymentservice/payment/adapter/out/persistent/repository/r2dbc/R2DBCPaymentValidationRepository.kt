package woo.paymentservice.payment.adapter.out.persistent.repository.r2dbc

import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Repository
import reactor.core.publisher.Mono
import woo.paymentservice.payment.adapter.out.persistent.exception.PaymentValidationException
import woo.paymentservice.payment.adapter.out.persistent.repository.PaymentValidationRepository

@Repository
class R2DBCPaymentValidationRepository(
    private val databaseClient: DatabaseClient,
) : PaymentValidationRepository {

    override fun isValid(orderId: String, amount: Long): Mono<Boolean> {
        return databaseClient.sql(SELECT_PAYMENT_TOTAL_AMOUNT_QUERY)
            .bind("orderId", orderId)
            .fetch()
            .first()
            .handle { row, sink ->
                val totalAmount = (row["total_amount"] as? Number)?.toLong() ?: 0L
                if (totalAmount == amount) {
                    sink.next(true)
                } else {
                    sink.error(PaymentValidationException("결제 (orderId: $orderId)에서 금액 (amount: $amount) 불일치"))
                }
            }
    }

    companion object {
        val SELECT_PAYMENT_TOTAL_AMOUNT_QUERY = """
            SELECT SUM(amount) AS total_amount
            FROM payment_orders
            WHERE order_id = :orderId
        """.trimIndent()
    }
}