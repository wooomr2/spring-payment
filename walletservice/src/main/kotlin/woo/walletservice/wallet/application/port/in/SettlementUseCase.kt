package woo.walletservice.wallet.application.port.`in`

import woo.walletservice.wallet.domain.PaymentEventMessage
import woo.walletservice.wallet.domain.WalletEventMessage

interface SettlementUseCase {

    fun processSettlement(paymentEventMessage: PaymentEventMessage): WalletEventMessage
}