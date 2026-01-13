package woo.paymentservice.payment.adapter.out.web.toss.executor

import reactor.core.publisher.Mono
import woo.paymentservice.payment.application.port.`in`.PaymentConfirmCommand
import woo.paymentservice.payment.domain.PaymentExecutionResult

interface PaymentExecutor {

    fun execute(command: PaymentConfirmCommand): Mono<PaymentExecutionResult>
}