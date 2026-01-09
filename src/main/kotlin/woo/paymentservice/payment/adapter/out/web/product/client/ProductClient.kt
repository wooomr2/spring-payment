package woo.paymentservice.payment.adapter.out.web.product.client

import reactor.core.publisher.Flux
import woo.paymentservice.payment.domain.Product

interface ProductClient {

    fun getProducts(cartId: Long, productIds: List<Long>): Flux<Product>
}