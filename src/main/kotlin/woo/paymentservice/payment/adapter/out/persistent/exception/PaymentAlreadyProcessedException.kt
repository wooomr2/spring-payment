package woo.paymentservice.payment.adapter.out.persistent.exception

import woo.paymentservice.payment.domain.PaymentStatus

class PaymentAlreadyProcessedException(val status: PaymentStatus, message: String) : RuntimeException(message)