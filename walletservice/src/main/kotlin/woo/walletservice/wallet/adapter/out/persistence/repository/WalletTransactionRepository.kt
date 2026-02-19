package woo.walletservice.wallet.adapter.out.persistence.repository

import woo.walletservice.wallet.domain.PaymentEventMessage
import woo.walletservice.wallet.domain.WalletTransaction

interface WalletTransactionRepository {

    fun isExist(paymentEventMessage: PaymentEventMessage): Boolean

    fun saveAll(walletTransactions: List<WalletTransaction>)
}