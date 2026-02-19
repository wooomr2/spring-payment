package woo.paymentservice.payment.adapter.out.stream

import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import woo.paymentservice.payment.application.stream.PaymentEventMessage
import woo.paymentservice.payment.application.stream.PaymentEventMessageType
import java.util.*

@SpringBootTest
@Tag("ExternalIntegration")
class PaymentEventSenderTest(
    @Autowired private val paymentEventSender: PaymentEventSender
) {

    @Test
    fun `should send eventMessage by using partitionKey`() {
        val paymentEventMessages = listOf(
            PaymentEventMessage(
                type = PaymentEventMessageType.PAYMENT_CONFIRMATION_SUCCESS,
                payload = mapOf(
                    "orderId" to UUID.randomUUID().toString(),
                ),
                metadata = mapOf(
                    "partitionKey" to 0
                ),
            ),
            PaymentEventMessage(
                type = PaymentEventMessageType.PAYMENT_CONFIRMATION_SUCCESS,
                payload = mapOf(
                    "orderId" to UUID.randomUUID().toString(),
                ),
                metadata = mapOf(
                    "partitionKey" to 1
                ),
            ),
            PaymentEventMessage(
                type = PaymentEventMessageType.PAYMENT_CONFIRMATION_SUCCESS,
                payload = mapOf(
                    "orderId" to UUID.randomUUID().toString(),
                ),
                metadata = mapOf(
                    "partitionKey" to 2
                ),
            ),
            PaymentEventMessage(
                type = PaymentEventMessageType.PAYMENT_CONFIRMATION_SUCCESS,
                payload = mapOf(
                    "orderId" to UUID.randomUUID().toString(),
                ),
                metadata = mapOf(
                    "partitionKey" to 3
                ),
            ),
            PaymentEventMessage(
                type = PaymentEventMessageType.PAYMENT_CONFIRMATION_SUCCESS,
                payload = mapOf(
                    "orderId" to UUID.randomUUID().toString(),
                ),
                metadata = mapOf(
                    "partitionKey" to 4
                ),
            ),
            PaymentEventMessage(
                type = PaymentEventMessageType.PAYMENT_CONFIRMATION_SUCCESS,
                payload = mapOf(
                    "orderId" to UUID.randomUUID().toString(),
                ),
                metadata = mapOf(
                    "partitionKey" to 5
                ),
            ),
        )

        paymentEventMessages.forEach {
            paymentEventSender.dispatch(it)
        }

        Thread.sleep(10000)
    }
}