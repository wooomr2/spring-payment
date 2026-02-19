package woo.paymentservice.payment.domain

import java.time.LocalDateTime

data class PaymentExtraDetails(
    val type: PaymentType,
    val method: PaymentMethod,
    val approvedAt: LocalDateTime,
    val orderName: String,
    val totalAmount: Long,
    val pspConfirmationStatus: PSPConfirmationStatus,
    val pspRawData: String
)
