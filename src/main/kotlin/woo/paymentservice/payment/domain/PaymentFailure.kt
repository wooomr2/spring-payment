package woo.paymentservice.payment.domain

data class PaymentFailure(
    val code: String,
    val message: String
)