package woo.paymentservice.payment.application.port.`in`

import reactor.core.publisher.Mono
import woo.paymentservice.payment.domain.CheckoutResult

interface CheckoutUseCase {

    fun checkout(command: CheckoutCommand): Mono<CheckoutResult>
}