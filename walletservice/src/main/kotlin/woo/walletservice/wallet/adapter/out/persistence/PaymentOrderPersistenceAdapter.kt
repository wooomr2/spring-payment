package woo.walletservice.wallet.adapter.out.persistence

import woo.walletservice.common.annotation.PersistentAdapter
import woo.walletservice.wallet.adapter.out.persistence.repository.jpa.JpaPaymentOrderRepository
import woo.walletservice.wallet.application.port.out.LoadPaymentOrderPort
import woo.walletservice.wallet.domain.PaymentOrder

@PersistentAdapter
class PaymentOrderPersistenceAdapter(
    private val paymentOrderRepository: JpaPaymentOrderRepository
) : LoadPaymentOrderPort {

    override fun getPaymentOrders(orderId: String): List<PaymentOrder> {
        return paymentOrderRepository.getPaymentOrders(orderId)
    }
}