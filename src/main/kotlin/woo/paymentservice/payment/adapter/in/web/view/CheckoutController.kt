package woo.paymentservice.payment.adapter.`in`.web.view

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import reactor.core.publisher.Mono
import woo.paymentservice.common.WebAdapter

@WebAdapter
@Controller
class CheckoutController {

    @GetMapping("/")
    fun checkoutPage(): Mono<String> {
        return Mono.just("checkout")
    }
}