package woo.paymentservice.payment.application.port.out

import woo.paymentservice.payment.application.stream.PaymentEventMessage

interface DispatchEventMessagePort {

    fun dispatch(paymentEventMessage: PaymentEventMessage)
}