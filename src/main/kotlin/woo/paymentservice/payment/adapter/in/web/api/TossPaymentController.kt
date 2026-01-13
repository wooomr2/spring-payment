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
import woo.paymentservice.payment.application.port.`in`.PaymentConfirmCommand
import woo.paymentservice.payment.application.port.`in`.PaymentConfirmUseCase
import woo.paymentservice.payment.domain.PaymentConfirmResult

@WebAdapter
@RequestMapping("/v1/toss")
@RestController
class TossPaymentController(
    private val paymentConfirmUseCase: PaymentConfirmUseCase
) {

    @PostMapping("/confirm")
    fun confirm(@RequestBody request: TossPaymentConfirmRequest): Mono<ResponseEntity<ApiResponse<PaymentConfirmResult>>> {

        val command = PaymentConfirmCommand(
            paymentKey = request.paymentKey,
            orderId = request.orderId,
            amount = request.amount
        )

        return paymentConfirmUseCase.confirm(command)
            .map { ResponseEntity.ok().body(ApiResponse.with(HttpStatus.OK, "", it)) }
    }
}

