package woo.paymentservice.payment.adapter.out.web.toss

import reactor.core.publisher.Mono
import woo.paymentservice.common.annotation.WebAdapter
import woo.paymentservice.payment.adapter.out.web.toss.executor.TossPaymentExecutor
import woo.paymentservice.payment.application.port.`in`.PaymentConfirmCommand
import woo.paymentservice.payment.application.port.out.PaymentExecutorPort
import woo.paymentservice.payment.domain.PaymentExecutionResult

@WebAdapter
class PaymentExecutorWebAdapter(
    private val tossPaymentExecutor: TossPaymentExecutor,
) : PaymentExecutorPort {

    override fun execute(command: PaymentConfirmCommand): Mono<PaymentExecutionResult> {
        return tossPaymentExecutor.execute(command)
    }
}