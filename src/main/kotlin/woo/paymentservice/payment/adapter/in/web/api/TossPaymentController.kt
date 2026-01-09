package woo.paymentservice.payment.adapter.`in`.web.api

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import woo.paymentservice.common.annotation.WebAdapter
import woo.paymentservice.payment.adapter.`in`.web.request.TossPaymentConfirmRequest
import woo.paymentservice.payment.adapter.`in`.web.response.ApiResponse
import woo.paymentservice.payment.adapter.out.web.toss.executor.TossPaymentExecutor

@WebAdapter
@RequestMapping("/v1/toss")
@RestController
class TossPaymentController(
    private val tossPaymentExecutor: TossPaymentExecutor
) {

    @PostMapping("/confirm")
    fun confirm(@RequestBody request: TossPaymentConfirmRequest): Mono<ResponseEntity<ApiResponse<String>>> {
        return tossPaymentExecutor.execute(request).map {
            ResponseEntity.ok().body(
                ApiResponse.with(HttpStatus.OK, "ok", it)
            )
        }
    }
}

