package woo.walletservice.wallet.application.port.out

import woo.walletservice.wallet.domain.PaymentEventMessage

interface DuplicateMessageFilterPort {

    fun isAlreadyProcess(paymentEventMessage: PaymentEventMessage): Boolean
}