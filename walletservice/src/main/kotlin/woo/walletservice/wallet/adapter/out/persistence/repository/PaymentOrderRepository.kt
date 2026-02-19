package woo.walletservice.wallet.adapter.out.persistence.repository

import woo.walletservice.wallet.domain.PaymentOrder

interface PaymentOrderRepository {

    fun getPaymentOrders(orderId: String): List<PaymentOrder>
}