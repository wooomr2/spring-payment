package woo.paymentservice.payment.adapter.out.web.toss.executor

import io.netty.handler.timeout.TimeoutException
import org.springframework.http.HttpStatusCode
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import reactor.util.retry.Retry
import woo.paymentservice.common.util.objectMapper
import woo.paymentservice.payment.adapter.out.web.toss.exception.EnumTossPaymentConfirmError
import woo.paymentservice.payment.adapter.out.web.toss.exception.PSPConfirmationException
import woo.paymentservice.payment.adapter.out.web.toss.response.TossPaymentConfirmResponse
import woo.paymentservice.payment.application.port.`in`.PaymentConfirmCommand
import woo.paymentservice.payment.domain.*
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Component
class TossPaymentExecutor(
    private val tossPaymentWebClient: WebClient,
    private val uri: String = "/v1/payments/confirm"
) : PaymentExecutor {

    override fun execute(command: PaymentConfirmCommand): Mono<PaymentExecutionResult> {
        return tossPaymentWebClient.post()
            .uri(uri)
            .header("Idempodency-Key", command.orderId)
            .bodyValue(command)
            .retrieve()
            .onStatus(
                { statusCode: HttpStatusCode -> statusCode.is4xxClientError || statusCode.is5xxServerError },
                { response ->
                    response
                        .bodyToMono<PaymentFailure>()
                        .flatMap {
                            val error = EnumTossPaymentConfirmError.get(it.code)
                            Mono.error<PSPConfirmationException>(
                                PSPConfirmationException(
                                    errorCode = error.name,
                                    errorMessage = error.description,
                                    isSuccess = error.isSuccessError(),
                                    isFailure = error.isFailureError(),
                                    isUnknown = error.isUnknownError(),
                                    isRetryableError = error.isRetryableError(),
                                )
                            )
                        }
                }
            )
            .bodyToMono<TossPaymentConfirmResponse>()
            .map {
                PaymentExecutionResult(
                    paymentKey = it.paymentKey,
                    orderId = command.orderId,
                    extraDetails = PaymentExtraDetails(
                        type = PaymentType.get(it.type),
                        method = PaymentMethod.get(it.method),
                        approvedAt = LocalDateTime.parse(it.approvedAt, DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                        orderName = it.orderName,
                        totalAmount = it.totalAmount,
                        pspConfirmationStatus = PSPConfirmationStatus.get(it.status),
                        pspRawData = objectMapper.writeValueAsString(it)
                    ),
                    isSuccess = true,
                    isFailure = false,
                    isUnknown = false,
                    isRetryable = false
                )
            }
            .retryWhen(
                Retry.backoff(2, Duration.ofSeconds(1)).jitter(0.1)
                    .filter { (it is PSPConfirmationException && it.isRetryableError) || it is TimeoutException }
                    .doBeforeRetry {
                        val failure = it.failure() as PSPConfirmationException
                        println(
                            """
                                [TossPaymentExecutor.beforeRetryHook]
                                retryCount: ${it.totalRetries()},
                                errorCode: ${failure.errorCode},
                                isUnknown: ${failure.isUnknown},
                                isFailure: ${failure.isFailure}
                            """.trimIndent()
                        )
                    }
                    .onRetryExhaustedThrow { _, retrySignal -> retrySignal.failure() }
            )
    }
}