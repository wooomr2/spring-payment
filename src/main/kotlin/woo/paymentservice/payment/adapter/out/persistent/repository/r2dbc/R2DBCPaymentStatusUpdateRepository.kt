package woo.paymentservice.payment.adapter.out.persistent.repository.r2dbc

import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Repository
import org.springframework.transaction.reactive.TransactionalOperator
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import woo.paymentservice.payment.adapter.out.persistent.exception.PaymentAlreadyProcessedException
import woo.paymentservice.payment.adapter.out.persistent.repository.PaymentStatusUpdateRepository
import woo.paymentservice.payment.application.port.out.PaymentStatusUpdateCommand
import woo.paymentservice.payment.domain.PaymentExtraDetails
import woo.paymentservice.payment.domain.PaymentStatus

@Repository
class R2DBCPaymentStatusUpdateRepository(
    private val databaseClient: DatabaseClient,
    private val transactionalOperator: TransactionalOperator
) : PaymentStatusUpdateRepository {

    override fun updatePaymentStatusToExecuting(orderId: String, paymentKey: String): Mono<Boolean> {
        return checkPreviousPaymentOrderStatus(orderId)
            .flatMap { insertPaymentHistory(it, PaymentStatus.EXECUTING, "PAYMENT_CONFIRMATION_START") }
            .flatMap { updatePaymentStatus(orderId, PaymentStatus.EXECUTING) }
            .flatMap { updatePaymentKey(orderId, paymentKey) }
            .`as`(transactionalOperator::transactional)
            .thenReturn(true)
    }

    override fun updatePaymentStatus(command: PaymentStatusUpdateCommand): Mono<Boolean> {
        return when (command.status) {
            PaymentStatus.SUCCESS -> updatePaymentStatusToSuccess(command)
            PaymentStatus.FAILURE -> updatePaymentStatusToFailure(command)
            PaymentStatus.UNKNOWN -> updatePaymentStatusToUnKnown(command)
            else -> error("지원하지 않는 결제 상태 변경입니다. status: ${command.status}")
        }
    }

    private fun updatePaymentStatusToSuccess(command: PaymentStatusUpdateCommand): Mono<Boolean> {
        return selectPaymentOrderStatus(command.orderId)
            .collectList()
            .flatMap { insertPaymentHistory(it, command.status, "PAYMENT_CONFIRMATION_DONE") }
            .flatMap { updatePaymentStatus(command.orderId, command.status) }
            .flatMap { updatePaymentEventExtraDetails(command) }
            .`as`(transactionalOperator::transactional)
            .thenReturn(true)
    }

    private fun updatePaymentEventExtraDetails(command: PaymentStatusUpdateCommand): Mono<Long> {
        val extraDetails: PaymentExtraDetails = command.extraDetails!!

        return databaseClient.sql(UPDATE_PAYMENT_EVENT_EXTRA_DETAILS_QUERY)
            .bind("orderName", extraDetails.orderName)
            .bind("method", extraDetails.method.name)
            .bind("approvedAt", extraDetails.approvedAt.toString())
            .bind("type", extraDetails.type)
            .bind("orderId", command.orderId)
            .fetch()
            .rowsUpdated()
    }

    private fun updatePaymentStatusToFailure(command: PaymentStatusUpdateCommand): Mono<Boolean> {
        return selectPaymentOrderStatus(command.orderId)
            .collectList()
            .flatMap { insertPaymentHistory(it, command.status, command.failure.toString()) }
            .flatMap { updatePaymentStatus(command.orderId, command.status) }
            .`as`(transactionalOperator::transactional)
            .thenReturn(true)
    }

    private fun updatePaymentStatusToUnKnown(command: PaymentStatusUpdateCommand): Mono<Boolean> {
        return selectPaymentOrderStatus(command.orderId)
            .collectList()
            .flatMap { insertPaymentHistory(it, command.status, "UNKNOWN") }
            .flatMap { updatePaymentStatus(command.orderId, command.status) }
            .flatMap { incrementPaymeentOrderFailCount(command) }
            .`as`(transactionalOperator::transactional)
            .thenReturn(true)
    }

    private fun incrementPaymeentOrderFailCount(command: PaymentStatusUpdateCommand): Mono<Long> {
        return databaseClient.sql(INCREMENT_PAYMENT_ORDER_FAIL_COUNT_QUERY)
            .bind("orderId", command.orderId)
            .fetch()
            .rowsUpdated()
    }

    private fun checkPreviousPaymentOrderStatus(orderId: String): Mono<List<Pair<Long, String>>> {
        return selectPaymentOrderStatus(orderId).handle { paymentOrder, sink ->
            when (paymentOrder.second) {

                PaymentStatus.NOT_STARTED.name,
                PaymentStatus.UNKNOWN.name,
                PaymentStatus.EXECUTING.name -> {
                    sink.next(paymentOrder)
                }

                PaymentStatus.SUCCESS.name -> {
                    sink.error(
                        PaymentAlreadyProcessedException(
                            status = PaymentStatus.SUCCESS,
                            message = "이미 처리성공한 결제입니다. orderId: $orderId"
                        )
                    )
                }

                PaymentStatus.FAILURE.name -> {
                    sink.error(
                        PaymentAlreadyProcessedException(
                            status = PaymentStatus.FAILURE,
                            message = "이미 처리실패한 결제입니다. orderId: $orderId"
                        )
                    )
                }
            }
        }.collectList()
    }

    private fun selectPaymentOrderStatus(orderId: String): Flux<Pair<Long, String>> {
        return databaseClient.sql(SELECT_PAYMENT_ORDER_STATUS_QUERY)
            .bind("orderId", orderId)
            .fetch()
            .all()
            .map { Pair(it["id"] as Long, (it["payment_order_status"] as String)) }
    }

    private fun insertPaymentHistory(
        paymentOrderIdStatusPairList: List<Pair<Long, String>>,
        newStatus: PaymentStatus,
        reason: String
    ): Mono<Long> {
        if (paymentOrderIdStatusPairList.isEmpty()) return Mono.empty()

        // TODO("BULK INSERT로 변경 필요")
        val inserts: List<Mono<Long>> = paymentOrderIdStatusPairList.map { paymentOrderIdStatusPair ->
            databaseClient.sql(INSERT_PAYMENT_ORDER_HISTORY_QUERY)
                .bind("paymentOrderId", paymentOrderIdStatusPair.first)
                .bind("previousStatus", paymentOrderIdStatusPair.second)
                .bind("newStatus", newStatus.name)
                .bind("reason", reason)
                .fetch()
                .rowsUpdated()
        }

        return Flux.concat(inserts).reduce { acc, v -> acc + v }
    }

    private fun updatePaymentStatus(orderId: String, status: PaymentStatus): Mono<Long> {
        return databaseClient.sql(UPDATE_PAYMENT_ORDER_STATUS_QUERY)
            .bind("status", status.name)
            .bind("orderId", orderId)
            .fetch()
            .rowsUpdated()
    }

    private fun updatePaymentKey(orderId: String, paymentKey: String): Mono<Long> {
        return databaseClient.sql(UPDATE_PAYMENT_EVENT_PAYMENT_KEY_QUERY)
            .bind("paymentKey", paymentKey)
            .bind("orderId", orderId)
            .fetch()
            .rowsUpdated()
    }

    companion object {
        val SELECT_PAYMENT_ORDER_STATUS_QUERY = """
            SELECT id, payment_order_status
            FROM payment_orders
            WHERE order_id = :orderId
        """.trimIndent()

        val INSERT_PAYMENT_ORDER_HISTORY_QUERY = """
            INSERT INTO payment_order_histories (payment_order_id, previous_status, new_status, reason)
            VALUES (:paymentOrderId, :previousStatus, :newStatus, :reason)
        """.trimIndent()

        val UPDATE_PAYMENT_ORDER_STATUS_QUERY = """
            UPDATE payment_orders
            SET
                payment_order_status = :status,
                updated_at = CURRENT_TIMESTAMP
            WHERE order_id = :orderId
        """.trimIndent()

        val UPDATE_PAYMENT_EVENT_PAYMENT_KEY_QUERY = """
            UPDATE payment_events
            SET payment_key = :paymentKey
            WHERE order_id = :orderId
        """.trimIndent()

        val UPDATE_PAYMENT_EVENT_EXTRA_DETAILS_QUERY = """
            UPDATE payment_events
            SET order_name = :orderName,
            method = :method,
            approved_at = :approvedAt,
            type = :type,
            updated_at = CURRENT_TIMESTAMP
            WHERE order_id = :orderId
        """.trimIndent()

        val INCREMENT_PAYMENT_ORDER_FAIL_COUNT_QUERY = """
            UPDATE payment_orders
            SET fail_count = fail_count + 1,
            updated_at = CURRENT_TIMESTAMP
            WHERE order_id = :orderId
        """.trimIndent()
    }
}