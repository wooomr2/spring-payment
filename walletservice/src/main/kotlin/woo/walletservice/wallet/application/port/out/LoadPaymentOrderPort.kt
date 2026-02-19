package woo.walletservice.wallet.application.port.out

import woo.walletservice.wallet.domain.PaymentOrder

interface LoadPaymentOrderPort {

    fun getPaymentOrders(orderId: String): List<PaymentOrder>
}