package woo.paymentservice.payment.domain

/**
 * 결제 처리 상태입니다. 아래와 같은 상태 값을 가질 수 있습니다. 상태 변화 흐름이 궁금하다면 흐름도를 살펴보세요.
 * - READY: 결제를 생성하면 가지게 되는 초기 상태입니다. 인증 전까지는 READY 상태를 유지합니다.
 * - IN_PROGRESS: 결제수단 정보와 해당 결제수단의 소유자가 맞는지 인증을 마친 상태입니다. 결제 승인 API를 호출하면 결제가 완료됩니다.
 * - WAITING_FOR_DEPOSIT: 가상계좌 결제 흐름에만 있는 상태입니다. 발급된 가상계좌에 구매자가 아직 입금하지 않은 상태입니다.
 * - DONE: 인증된 결제수단으로 요청한 결제가 승인된 상태입니다.
 * - CANCELED: 승인된 결제가 취소된 상태입니다.
 * - PARTIAL_CANCELED: 승인된 결제가 부분 취소된 상태입니다.
 * - ABORTED: 결제 승인이 실패한 상태입니다.
 * - EXPIRED: 결제 유효 시간 30분이 지나 거래가 취소된 상태입니다. IN_PROGRESS 상태에서 결제 승인 API를 호출하지 않으면 EXPIRED가 됩니다.
 * */
enum class PSPConfirmationStatus(description: String) {
    READY("결제 생성 후 인증 전 초기 상태"),
    IN_PROGRESS("결제수단 정보와 해당 결제수단의 소유자가 맞는지 인증을 마친 상태"),
    WAITING_FOR_DEPOSIT("가상계좌 결제 흐름에만 있는 상태로, 발급된 가상계좌에 구매자가 아직 입금하지 않은 상태"),
    DONE("완료"),
    CANCELED("승인된 결제가 취소된 상태"),
    PARTIAL_CANCELED("결제유효시간이 지나서 만료된 상태"),
    ABORTED("결제 승인이 실패한 상태"),
    EXPIRED("결제 유효 시간 30분이 지나 거래가 취소된 상태"),
    ;

    companion object {
        fun get(status: String): PSPConfirmationStatus {
            return entries.find { it.name == status } ?: error("PSP승인상태: (status: $status)는 올바르지 않습니다.")
        }
    }
}