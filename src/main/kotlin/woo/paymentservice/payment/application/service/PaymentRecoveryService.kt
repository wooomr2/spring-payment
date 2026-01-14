package woo.paymentservice.payment.application.service

import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Scheduled
import reactor.core.scheduler.Schedulers
import woo.paymentservice.common.annotation.UseCase
import woo.paymentservice.payment.application.port.`in`.PaymentConfirmCommand
import woo.paymentservice.payment.application.port.`in`.PaymentRecoveryUseCase
import woo.paymentservice.payment.application.port.out.*
import java.util.concurrent.TimeUnit

@UseCase
@Profile("prod")
class PaymentRecoveryService(
    private val loadPendingPaymentPort: LoadPendingPaymentPort,
    private val paymentValidationPort: PaymentValidationPort,
    private val paymentExecutorPort: PaymentExecutorPort,
    private val paymentStatusUpdatePort: PaymentStatusUpdatePort,
    private val paymentErrorHandler: PaymentErrorHandler,
) : PaymentRecoveryUseCase {

    private val schedular = Schedulers.newSingle("recovery")

    /** 3분마다. 최초delay 3분 */
    @Scheduled(fixedDelay = 180, initialDelay = 180, timeUnit = TimeUnit.SECONDS)
    override fun recovery() {
        loadPendingPaymentPort.getPendingPayments()
            .map {
                PaymentConfirmCommand(
                    paymentKey = it.paymentKey,
                    orderId = it.orderId,
                    amount = it.totalAmount()
                )
            }
            .parallel(2)
            .runOn(Schedulers.parallel())
            .flatMap { command ->
                paymentValidationPort.isValid(command.orderId, command.amount).thenReturn(command)
                    .flatMap { paymentExecutorPort.execute(it) }
                    .flatMap { paymentStatusUpdatePort.updatePaymentStatus(PaymentStatusUpdateCommand(it)) }
                    .onErrorResume { error ->
                        println("[PaymentRecoveryService] 결제 복구 처리 중 오류 발생: ${command.orderId}")
                        paymentErrorHandler.handlePaymentConfirmError(error, command).thenReturn(true)
                    }
            }
            .sequential() // ParallelFlux<T> -> Flux<T> 완료된 순서대로 합쳐 단일 Stream으로 만든다
            .subscribeOn(schedular) // 이 체인의 구독 시작 신호를 보낼 Thread를 지정
            .subscribe()
    }
}


