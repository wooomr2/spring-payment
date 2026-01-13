package woo.paymentservice.payment.adapter.out.web.toss.executor

import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import woo.paymentservice.payment.adapter.out.web.toss.response.TossPaymentConfirmResponse
import woo.paymentservice.payment.application.port.`in`.PaymentConfirmCommand
import woo.paymentservice.payment.domain.*
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
                        pspRawData = it.toString()
                    ),
                    isSuccess = true,
                    isFailure = false,
                    isUnknown = false,
                    isRetryable = false
                )
            }
    }
}