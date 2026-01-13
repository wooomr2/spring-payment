package woo.paymentservice.payment.application.service

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import reactor.core.publisher.Mono
import woo.paymentservice.payment.adapter.out.web.toss.response.PaymentExecutionFailure
import woo.paymentservice.payment.application.port.`in`.CheckoutCommand
import woo.paymentservice.payment.application.port.`in`.CheckoutUseCase
import woo.paymentservice.payment.application.port.`in`.PaymentConfirmCommand
import woo.paymentservice.payment.application.port.out.PaymentExecutorPort
import woo.paymentservice.payment.application.port.out.PaymentStatusUpdatePort
import woo.paymentservice.payment.application.port.out.PaymentValidationPort
import woo.paymentservice.payment.domain.*
import woo.paymentservice.payment.test.PaymentDatabaseHelper
import woo.paymentservice.payment.test.PaymentTestConfiguration
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.*
import kotlin.test.Test

@SpringBootTest
@Import(PaymentTestConfiguration::class)
class PaymentConfirmServiceTest(
    @Autowired private val checkoutUseCase: CheckoutUseCase,
    @Autowired private val paymentStatusUpdatePort: PaymentStatusUpdatePort,
    @Autowired private val paymentValidationPort: PaymentValidationPort,
    @Autowired private val paymentDatabaseHelper: PaymentDatabaseHelper,
) {

    private val mockPaymentExecutorPort = mockk<PaymentExecutorPort>()

    @BeforeEach
    fun setup() {
        // block(): 비동기 blockings
        paymentDatabaseHelper.clean().block()
    }

    @Test
    fun `should be marked as SUCCESS if payment Confimation success in PSPs`() {
        val orderId = UUID.randomUUID().toString()

        val checkoutCommand = CheckoutCommand(
            cartId = 1,
            buyerId = 1,
            productIds = listOf(1, 2, 3),
            idempotencyKey = orderId
        )

        // 1. checkout
        val checkoutResult: CheckoutResult = checkoutUseCase.checkout(checkoutCommand).block()!!

        val paymentConfirmCommand = PaymentConfirmCommand(
            paymentKey = UUID.randomUUID().toString(),
            orderId = checkoutResult.orderId,
            amount = checkoutResult.amount
        )

        val paymentExecutionResult = PaymentExecutionResult(
            paymentKey = paymentConfirmCommand.paymentKey,
            orderId = paymentConfirmCommand.orderId,
            extraDetails = PaymentExtraDetails(
                type = PaymentType.NORMAL,
                method = PaymentMethod.EASY_PAY,
                approvedAt = LocalDateTime.now(),
                orderName = "test_order_name",
                totalAmount = paymentConfirmCommand.amount,
                pspConfirmationStatus = PSPConfirmationStatus.DONE,
                pspRawData = "{}"
            ),
            isSuccess = true,
            isFailure = false,
            isUnknown = false,
            isRetryable = false
        )

        // 2. mock PaymentExecutorPort.execute
        every { mockPaymentExecutorPort.execute(paymentConfirmCommand) } returns Mono.just(paymentExecutionResult)

        // 3. confirm
        val paymentConfirmService =
            PaymentConfirmService(paymentStatusUpdatePort, paymentValidationPort, mockPaymentExecutorPort)

        val paymentConfirmResult = paymentConfirmService.confirm(paymentConfirmCommand).block()!!

        val paymentEvent: PaymentEvent = paymentDatabaseHelper.getPayments(orderId)!!

        // then
        assertThat(paymentConfirmResult.status).isEqualTo(PaymentStatus.SUCCESS)
        assertTrue(paymentEvent.paymentOrders.all { it.paymentStatus == PaymentStatus.SUCCESS })
        assertThat(paymentEvent.paymentType).isEqualTo(paymentExecutionResult.extraDetails!!.type)
        assertThat(paymentEvent.paymentMethod).isEqualTo(paymentExecutionResult.extraDetails.method)
        assertThat(paymentEvent.orderName).isEqualTo(paymentExecutionResult.extraDetails.orderName)
        assertThat(paymentEvent.approvedAt!!.truncatedTo(ChronoUnit.MINUTES)).isEqualTo(
            paymentExecutionResult.extraDetails.approvedAt.truncatedTo(
                ChronoUnit.MINUTES
            )
        )
    }

    @Test
    fun `should be marked as FAILURE if payment Confimation fail in PSPs`() {
        val orderId = UUID.randomUUID().toString()

        val checkoutCommand = CheckoutCommand(
            cartId = 1,
            buyerId = 1,
            productIds = listOf(1, 2, 3),
            idempotencyKey = orderId
        )

        // 1. checkout
        val checkoutResult: CheckoutResult = checkoutUseCase.checkout(checkoutCommand).block()!!

        val paymentConfirmCommand = PaymentConfirmCommand(
            paymentKey = UUID.randomUUID().toString(),
            orderId = checkoutResult.orderId,
            amount = checkoutResult.amount
        )

        val paymentExecutionResult = PaymentExecutionResult(
            paymentKey = paymentConfirmCommand.paymentKey,
            orderId = paymentConfirmCommand.orderId,
            extraDetails = PaymentExtraDetails(
                type = PaymentType.NORMAL,
                method = PaymentMethod.EASY_PAY,
                approvedAt = LocalDateTime.now(),
                orderName = "test_order_name",
                totalAmount = paymentConfirmCommand.amount,
                pspConfirmationStatus = PSPConfirmationStatus.DONE,
                pspRawData = "{}"
            ),
            failure = PaymentExecutionFailure(
                code = "TEST_FAILURE_CODE",
                message = "Test failure message from PSP"
            ),
            isSuccess = false,
            isFailure = true,
            isUnknown = false,
            isRetryable = false
        )

        // 2. mock PaymentExecutorPort.execute
        every { mockPaymentExecutorPort.execute(paymentConfirmCommand) } returns Mono.just(paymentExecutionResult)

        // 3. confirm
        val paymentConfirmService =
            PaymentConfirmService(paymentStatusUpdatePort, paymentValidationPort, mockPaymentExecutorPort)

        val paymentConfirmResult = paymentConfirmService.confirm(paymentConfirmCommand).block()!!

        val paymentEvent: PaymentEvent = paymentDatabaseHelper.getPayments(orderId)!!

        // then
        assertThat(paymentConfirmResult.status).isEqualTo(PaymentStatus.FAILURE)
        assertTrue(paymentEvent.paymentOrders.all { it.paymentStatus == PaymentStatus.FAILURE })
    }

    @Test
    fun `should be marked as UNKNOWN if payment confirmation is unknown in PSPs`() {
        val orderId = UUID.randomUUID().toString()

        val checkoutCommand = CheckoutCommand(
            cartId = 1,
            buyerId = 1,
            productIds = listOf(1, 2, 3),
            idempotencyKey = orderId
        )

        // 1. checkout
        val checkoutResult: CheckoutResult = checkoutUseCase.checkout(checkoutCommand).block()!!

        val paymentConfirmCommand = PaymentConfirmCommand(
            paymentKey = UUID.randomUUID().toString(),
            orderId = checkoutResult.orderId,
            amount = checkoutResult.amount
        )

        val paymentExecutionResult = PaymentExecutionResult(
            paymentKey = paymentConfirmCommand.paymentKey,
            orderId = paymentConfirmCommand.orderId,
            isSuccess = false,
            isFailure = false,
            isUnknown = true,
            isRetryable = false
        )

        // 2. mock PaymentExecutorPort.execute
        every { mockPaymentExecutorPort.execute(paymentConfirmCommand) } returns Mono.just(paymentExecutionResult)

        // 3. confirm
        val paymentConfirmService =
            PaymentConfirmService(paymentStatusUpdatePort, paymentValidationPort, mockPaymentExecutorPort)

        val paymentConfirmResult = paymentConfirmService.confirm(paymentConfirmCommand).block()!!

        val paymentEvent: PaymentEvent = paymentDatabaseHelper.getPayments(orderId)!!

        // then
        assertThat(paymentConfirmResult.status).isEqualTo(PaymentStatus.UNKNOWN)
        assertTrue(paymentEvent.paymentOrders.all { it.paymentStatus == PaymentStatus.UNKNOWN })
    }
}