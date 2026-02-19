package woo.walletservice.wallet.domain

data class PaymentEventMessage(
    val type: PaymentEventMessageType,
    val payload: Map<String, Any>,
    val metadata: Map<String, Any>
) {
    fun orderId(): String {
        return payload["orderId"] as String
    }
}

enum class PaymentEventMessageType(description: String) {
    PAYMENT_CONFIRMATION_SUCCESS("결제 승인 완료 이벤트"),
}