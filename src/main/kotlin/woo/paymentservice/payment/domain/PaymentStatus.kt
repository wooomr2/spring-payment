package woo.paymentservice.payment.domain

enum class PaymentStatus(description: String) {
    NOT_STARTED("결제 승인 전"),
    EXECUTING("결제 승인 중"),
    SUCCESS("결제승인 완료"),
    FAILURE("결제승인 실패"),
    UNKNOWN("결제 승인 완료"),
    ;

    companion object {
        fun get(status: String): PaymentStatus {
            return entries.find { it.name == status }
                ?: throw IllegalArgumentException("Unknown PaymentStatus: $status")
        }
    }
}