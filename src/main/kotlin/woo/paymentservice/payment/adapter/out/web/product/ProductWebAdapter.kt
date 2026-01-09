package woo.paymentservice.payment.adapter.out.web.product

import reactor.core.publisher.Flux
import woo.paymentservice.common.annotation.WebAdapter
import woo.paymentservice.payment.adapter.out.web.product.client.ProductClient
import woo.paymentservice.payment.application.port.out.LoadProductPort
import woo.paymentservice.payment.domain.Product

@WebAdapter
class ProductWebAdapter(
    private val productClient: ProductClient
) : LoadProductPort {

    override fun getProducts(cartId: Long, productIds: List<Long>): Flux<Product> {
        return productClient.getProducts(cartId, productIds)
    }
}