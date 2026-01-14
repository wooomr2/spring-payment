package woo.paymentservice.payment.adapter.`in`.web.view

import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import reactor.core.publisher.Mono
import woo.paymentservice.common.IdempodencyCreater
import woo.paymentservice.common.annotation.WebAdapter
import woo.paymentservice.payment.adapter.`in`.web.request.CheckoutRequest
import woo.paymentservice.payment.application.port.`in`.CheckoutCommand
import woo.paymentservice.payment.application.port.`in`.CheckoutUseCase

@WebAdapter
@Controller
class CheckoutController(
    private val checkoutUseCase: CheckoutUseCase,
) {

    @GetMapping("/")
    fun checkoutPage(request: CheckoutRequest, model: Model): Mono<String> {
        val command = CheckoutCommand(
            cartId = request.cartId,
            buyerId = request.buyerId,
            productIds = request.productIds,
            idempotencyKey = IdempodencyCreater.create(request.seed)
        )

        return checkoutUseCase.checkout(command).map {
            model.addAttribute("orderId", it.orderId)
            model.addAttribute("orderName", it.orderName)
            model.addAttribute("amount", it.amount)
            "checkout"
        }
    }
}