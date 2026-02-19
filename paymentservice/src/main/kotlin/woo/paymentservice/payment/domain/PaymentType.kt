package woo.paymentservice.payment.domain

enum class PaymentType(description: String) {
    NORMAL("일반결제"),
    ;

    companion object {
        fun get(type: String): PaymentType {
            return entries.find { it.name == type } ?: error("PaymentType($type)을 찾을 수 없습니다.")
        }
    }
}