package woo.walletservice.wallet.adapter.`in`.stream

import org.springframework.cloud.stream.function.StreamBridge
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.messaging.Message
import woo.walletservice.common.annotation.StreamAdapter
import woo.walletservice.common.util.Logger
import woo.walletservice.wallet.application.port.`in`.SettlementUseCase
import woo.walletservice.wallet.domain.PaymentEventMessage
import java.util.function.Consumer

@Configuration
@StreamAdapter
class PaymentEventMessageHandler(
    private val settlementUseCase: SettlementUseCase,
    private val streamBridge: StreamBridge
) {

    @Bean
    fun consume(): Consumer<Message<PaymentEventMessage>> {
        return Consumer { message ->
            val walletEventMessage = settlementUseCase.processSettlement(message.payload)
            streamBridge.send("wallet", walletEventMessage)
            Logger.info("consume", message.payload.toString())
        }
    }
}