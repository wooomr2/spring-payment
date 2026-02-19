package woo.paymentservice.payment.domain


data class PaymentConfirmResult(
    val status: PaymentStatus,
    val failure: PaymentFailure? = null
) {
    init {
        if (status == PaymentStatus.FAILURE) {
            requireNotNull(failure) {
                "결제상태가 FAILURE인 경우에는 failure 정보가 반드시 제공되어야 합니다."
            }
        }
    }

    val message = when (status) {
        PaymentStatus.SUCCESS -> "결제 처리에 성공하였습니다"
        PaymentStatus.FAILURE -> "결제 처리에 실패하였습니다"
        PaymentStatus.UNKNOWN -> "결제 처리 상태를 알 수 없습니다"
        else -> error("현재 결제 상태 (status: $status)는 결제 확인 결과로 올바르지 않습니다.")
    }
}
