package woo.paymentservice.payment.adapter.out.web.executor

import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import woo.paymentservice.payment.adapter.`in`.web.request.TossPaymentConfirmRequest

@Component
class TossPaymentExecutor(
    private val tossPaymentWebClient: WebClient,
    private val uri: String = "/v1/payments/confirm"
) {

    fun execute(request: TossPaymentConfirmRequest): Mono<String> {

        return tossPaymentWebClient.post()
            .uri(uri)
            .bodyValue(request)
            .retrieve()
            .bodyToMono(String::class.java)
    }
}