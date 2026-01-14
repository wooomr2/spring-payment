package woo.paymentservice.payment.application.port.out

import woo.paymentservice.payment.domain.PaymentExecutionResult
import woo.paymentservice.payment.domain.PaymentExtraDetails
import woo.paymentservice.payment.domain.PaymentFailure
import woo.paymentservice.payment.domain.PaymentStatus

data class PaymentStatusUpdateCommand(
    val paymentKey: String,
    val orderId: String,
    val status: PaymentStatus,
    val extraDetails: PaymentExtraDetails? = null,
    val failure: PaymentFailure? = null,
) {
    constructor(paymentExecutionResult: PaymentExecutionResult) : this(
        paymentKey = paymentExecutionResult.paymentKey,
        orderId = paymentExecutionResult.orderId,
        status = paymentExecutionResult.paymentStatus(),
        extraDetails = paymentExecutionResult.extraDetails,
        failure = paymentExecutionResult.failure,
    )

    init {
        require(status == PaymentStatus.SUCCESS || status == PaymentStatus.FAILURE || status == PaymentStatus.UNKNOWN) {
            "결제상태 (status: $status)는 올바르지 않은 결제 상태입니다. 허용되는 상태는 SUCCESS, FAILURE, UNKNWON 입니다."
        }

        if (status == PaymentStatus.SUCCESS) {
            requireNotNull(extraDetails) {
                "결제상태가 SUCCESS인 경우에는 extraDetails가 반드시 제공되어야 합니다."
            }
        } else if (status == PaymentStatus.FAILURE) {
            requireNotNull(failure) {
                "결제상태가 FAILURE인 경우에는 failure 정보가 반드시 제공되어야 합니다."
            }
        }
    }
}
