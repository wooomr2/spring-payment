package woo.paymentservice.payment.application.service

import reactor.core.publisher.Mono
import woo.paymentservice.common.annotation.UseCase
import woo.paymentservice.payment.application.port.`in`.CheckoutCommand
import woo.paymentservice.payment.application.port.`in`.CheckoutUseCase
import woo.paymentservice.payment.application.port.out.LoadProductPort
import woo.paymentservice.payment.application.port.out.SavePaymentPort
import woo.paymentservice.payment.domain.*

@UseCase
class CheckoutService(
    private val loadProductPort: LoadProductPort,
    private val savePaymentPort: SavePaymentPort
) : CheckoutUseCase {

    override fun checkout(command: CheckoutCommand): Mono<CheckoutResult> {
        return loadProductPort.getProducts(command.cartId, command.productIds)
            .collectList()
            .map { createPaymentEvent(command, it) }
            .flatMap { savePaymentPort.save(it).thenReturn(it) }
            .map {
                CheckoutResult(
                    amount = it.totalAmount(),
                    orderId = it.orderId,
                    orderName = it.orderName
                )
            }
    }

    private fun createPaymentEvent(command: CheckoutCommand, products: List<Product>): PaymentEvent {
        return PaymentEvent(
            buyerId = command.buyerId,
            orderId = command.idempotencyKey,
            orderName = products.joinToString { it.name },
            paymentOrders = products.map {
                PaymentOrder(
                    sellerId = it.sellerId,
                    orderId = command.idempotencyKey,
                    productId = it.id,
                    amount = it.amount,
                    paymentStatus = PaymentStatus.NOT_STARTED,
                )
            }
        )
    }

}