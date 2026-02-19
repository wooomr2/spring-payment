package woo.paymentservice.payment.application.service

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import reactor.core.publisher.Mono
import woo.paymentservice.payment.adapter.out.persistent.exception.PaymentValidationException
import woo.paymentservice.payment.adapter.out.web.toss.exception.EnumTossPaymentConfirmError
import woo.paymentservice.payment.adapter.out.web.toss.exception.PSPConfirmationException
import woo.paymentservice.payment.application.port.`in`.CheckoutCommand
import woo.paymentservice.payment.application.port.`in`.CheckoutUseCase
import woo.paymentservice.payment.application.port.`in`.PaymentConfirmCommand
import woo.paymentservice.payment.application.port.out.PaymentExecutorPort
import woo.paymentservice.payment.application.port.out.PaymentStatusUpdatePort
import woo.paymentservice.payment.application.port.out.PaymentValidationPort
import woo.paymentservice.payment.config.PaymentTestConfig
import woo.paymentservice.payment.domain.*
import woo.paymentservice.payment.helper.PaymentDatabaseHelper
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.*
import kotlin.test.Test

@SpringBootTest
@Import(PaymentTestConfig::class)
class PaymentConfirmServiceTest(
    @Autowired private val checkoutUseCase: CheckoutUseCase,
    @Autowired private val paymentStatusUpdatePort: PaymentStatusUpdatePort,
    @Autowired private val paymentValidationPort: PaymentValidationPort,
    @Autowired private val paymentDatabaseHelper: PaymentDatabaseHelper,
    @Autowired private val paymentErrorHandler: PaymentErrorHandler
) {

    private val mockPaymentExecutorPort = mockk<PaymentExecutorPort>()
    private val mockPaymentValidationPort = mockk<PaymentValidationPort>()

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
            PaymentConfirmService(
                paymentStatusUpdatePort,
                paymentValidationPort,
                mockPaymentExecutorPort,
                paymentErrorHandler
            )

        val paymentConfirmResult = paymentConfirmService.confirm(paymentConfirmCommand).block()!!

        val paymentEvent: PaymentEvent = paymentDatabaseHelper.getPayments(orderId)!!
        val extraDetails = paymentExecutionResult.extraDetails!!

        // then
        assertThat(paymentConfirmResult.status).isEqualTo(PaymentStatus.SUCCESS)
        assertTrue(paymentEvent.isSuccess())
        assertThat(paymentEvent.paymentType).isEqualTo(extraDetails.type)
        assertThat(paymentEvent.paymentMethod).isEqualTo(extraDetails.method)
        assertThat(paymentEvent.orderName).isEqualTo(extraDetails.orderName)
        assertThat(paymentEvent.approvedAt!!.truncatedTo(ChronoUnit.MINUTES)).isEqualTo(
            extraDetails.approvedAt.truncatedTo(ChronoUnit.MINUTES)
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
            failure = PaymentFailure(
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
            PaymentConfirmService(
                paymentStatusUpdatePort,
                paymentValidationPort,
                mockPaymentExecutorPort,
                paymentErrorHandler
            )

        val paymentConfirmResult = paymentConfirmService.confirm(paymentConfirmCommand).block()!!

        val paymentEvent: PaymentEvent = paymentDatabaseHelper.getPayments(orderId)!!

        // then
        assertThat(paymentConfirmResult.status).isEqualTo(PaymentStatus.FAILURE)
        assertTrue(paymentEvent.isFailure())
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
            PaymentConfirmService(
                paymentStatusUpdatePort,
                paymentValidationPort,
                mockPaymentExecutorPort,
                paymentErrorHandler
            )

        val paymentConfirmResult = paymentConfirmService.confirm(paymentConfirmCommand).block()!!

        val paymentEvent: PaymentEvent = paymentDatabaseHelper.getPayments(orderId)!!

        // then
        assertThat(paymentConfirmResult.status).isEqualTo(PaymentStatus.UNKNOWN)
        assertTrue(paymentEvent.isUnknown())
    }

    @Test
    fun `should handle PSPConfirmException`() {
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

        val pspConfirmException = PSPConfirmationException(
            errorCode = EnumTossPaymentConfirmError.REJECT_ACCOUNT_PAYMENT.name,
            errorMessage = EnumTossPaymentConfirmError.REJECT_ACCOUNT_PAYMENT.description,
            isSuccess = false,
            isFailure = true,
            isUnknown = false,
            isRetryableError = false
        )

        every { mockPaymentExecutorPort.execute(paymentConfirmCommand) } returns Mono.error(pspConfirmException)

        val paymentConfirmService =
            PaymentConfirmService(
                paymentStatusUpdatePort,
                paymentValidationPort,
                mockPaymentExecutorPort,
                paymentErrorHandler
            )

        val paymentConfirmResult = paymentConfirmService.confirm(paymentConfirmCommand).block()!!

        val paymentEvent: PaymentEvent = paymentDatabaseHelper.getPayments(orderId)!!

        // then
        assertThat(paymentConfirmResult.status).isEqualTo(PaymentStatus.FAILURE)
        assertTrue(paymentEvent.isFailure())
    }

    @Test
    fun `should handle PaymentValidationException`() {
        val orderId = UUID.randomUUID().toString()

        val checkoutCommand = CheckoutCommand(
            cartId = 1,
            buyerId = 1,
            productIds = listOf(1, 2, 3),
            idempotencyKey = orderId
        )

        val checkoutResult: CheckoutResult = checkoutUseCase.checkout(checkoutCommand).block()!!

        val paymentConfirmCommand = PaymentConfirmCommand(
            paymentKey = UUID.randomUUID().toString(),
            orderId = checkoutResult.orderId,
            amount = checkoutResult.amount + 1000 // invalid amount
        )

        val validationException = PaymentValidationException("결제 유효성 검증에 실패하였습니다")

        // 2. mock PaymentExecutorPort.execute
        every { mockPaymentValidationPort.isValid(orderId, paymentConfirmCommand.amount) } returns Mono.error(
            validationException
        )

        val paymentConfirmService =
            PaymentConfirmService(
                paymentStatusUpdatePort,
                mockPaymentValidationPort,
                mockPaymentExecutorPort,
                paymentErrorHandler
            )


        val paymentConfirmResult = paymentConfirmService.confirm(paymentConfirmCommand).block()!!
        val paymentEvent: PaymentEvent = paymentDatabaseHelper.getPayments(orderId)!!

        assertThat(paymentConfirmResult.status).isEqualTo(PaymentStatus.FAILURE)
        assertTrue(paymentEvent.isFailure())
    }

    @Test
    @Tag("ExternalIntegration")
    fun `should send the event message to the external message system after the payment confirmation`() {
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
            PaymentConfirmService(
                paymentStatusUpdatePort,
                paymentValidationPort,
                mockPaymentExecutorPort,
                paymentErrorHandler
            )

        paymentConfirmService.confirm(paymentConfirmCommand).block()!!

        Thread.sleep(1000)
    }
}