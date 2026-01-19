package woo.paymentservice.payment.application.service

import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import reactor.core.publisher.Hooks
import woo.paymentservice.payment.adapter.out.persistent.repository.PaymentOutboxRepository
import woo.paymentservice.payment.application.port.`in`.PaymentEventMessageRelayUseCase
import woo.paymentservice.payment.application.port.out.PaymentStatusUpdateCommand
import woo.paymentservice.payment.domain.*
import java.time.LocalDateTime
import java.util.*

@SpringBootTest
@Tag("ExternalIntegration")
class PaymentEventMessageRelayServiceTest(
    @Autowired private val paymentOutboxRepository: PaymentOutboxRepository,
    @Autowired private val paymentEventMessageRelayUseCase: PaymentEventMessageRelayUseCase
) {

    @Test
    fun `should dispatch external message system`() {
        Hooks.onOperatorDebug()

        val paymentExecutionResult = PaymentExecutionResult(
            paymentKey = UUID.randomUUID().toString(),
            orderId = UUID.randomUUID().toString(),
            extraDetails = PaymentExtraDetails(
                type = PaymentType.NORMAL,
                method = PaymentMethod.EASY_PAY,
                approvedAt = LocalDateTime.now(),
                orderName = "test_ordfer_name",
                totalAmount = 50000L,
                pspConfirmationStatus = PSPConfirmationStatus.DONE,
                pspRawData = "{}"
            ),
            isSuccess = true,
            isFailure = false,
            isUnknown = false,
            isRetryable = false,
        )

        val command = PaymentStatusUpdateCommand(paymentExecutionResult)

        paymentOutboxRepository.insertOutbox(command).block()

        paymentEventMessageRelayUseCase.relay()

        Thread.sleep(10000)
    }
}