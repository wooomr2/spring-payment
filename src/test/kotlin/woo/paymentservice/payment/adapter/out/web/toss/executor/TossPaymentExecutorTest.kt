package woo.paymentservice.payment.adapter.out.web.toss.executor

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import woo.paymentservice.payment.adapter.out.web.toss.exception.EnumTossPaymentConfirmError
import woo.paymentservice.payment.adapter.out.web.toss.exception.PSPConfirmationException
import woo.paymentservice.payment.application.port.`in`.PaymentConfirmCommand
import woo.paymentservice.payment.config.PSPTestWebClientConfig
import java.util.*

/**
 * 토스 - 테스트환경에서 에러 재현하기
 * https://docs.tosspayments.com/guides/v2/get-started/environment#%ED%85%8C%EC%8A%A4%ED%8A%B8-%ED%99%98%EA%B2%BD%EC%97%90%EC%84%9C-%EC%97%90%EB%9F%AC-%EC%9E%AC%ED%98%84%ED%95%98%EA%B8%B0
 * */
@SpringBootTest
@Import(PSPTestWebClientConfig::class)
//@Tag("TooLongToRun")
class TossPaymentExecutorTest(
    @Autowired private val pspTestWebClientConfig: PSPTestWebClientConfig
) {

    @Test
    fun `should handle correctly various TossPaymentError scenarios`() {
        generateErrorSenarios().forEach { errorSenario ->
            val command = PaymentConfirmCommand(
                paymentKey = UUID.randomUUID().toString(),
                orderId = UUID.randomUUID().toString(),
                amount = 10000L,
            )

            val paymentExecutor = TossPaymentExecutor(
                tossPaymentWebClient = pspTestWebClientConfig.createTestTossWebClient(
                    Pair("TossPayments-Test-Code", errorSenario.errorCode)
                ),
                uri = "/v1/payments/key-in"
            )

            try {
                paymentExecutor.execute(command).block()
            } catch (e: PSPConfirmationException) {
                assertThat(e.isSuccess == errorSenario.isSuccess)
                assertThat(e.isFailure == errorSenario.isFailure)
                assertThat(e.isUnknown == errorSenario.isUnknown)
            }
        }
    }

    private fun generateErrorSenarios(): List<ErrorSenario> {
        return EnumTossPaymentConfirmError.entries.map { error ->
            ErrorSenario(
                errorCode = error.name,
                isSuccess = error.isSuccessError(),
                isFailure = error.isFailureError(),
                isUnknown = error.isUnknownError()
            )
        }
    }
}

data class ErrorSenario(
    val errorCode: String,
    val isSuccess: Boolean,
    val isFailure: Boolean,
    val isUnknown: Boolean,
)