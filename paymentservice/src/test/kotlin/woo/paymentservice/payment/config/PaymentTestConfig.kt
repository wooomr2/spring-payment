package woo.paymentservice.payment.config

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.transaction.reactive.TransactionalOperator
import woo.paymentservice.payment.helper.PaymentDatabaseHelper
import woo.paymentservice.payment.helper.R2DBCPaymentDatabaseHelper

@TestConfiguration
class PaymentTestConfig {

    @Bean
    fun paymentDatabaseHelper(
        databaseClient: DatabaseClient,
        transactionOperator: TransactionalOperator
    ): PaymentDatabaseHelper {
        return R2DBCPaymentDatabaseHelper(databaseClient, transactionOperator)
    }
}