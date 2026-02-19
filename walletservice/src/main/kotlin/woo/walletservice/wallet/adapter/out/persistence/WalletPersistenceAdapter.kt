package woo.walletservice.wallet.adapter.out.persistence

import woo.walletservice.common.annotation.PersistentAdapter
import woo.walletservice.wallet.adapter.out.persistence.repository.WalletRepository
import woo.walletservice.wallet.adapter.out.persistence.repository.WalletTransactionRepository
import woo.walletservice.wallet.application.port.out.DuplicateMessageFilterPort
import woo.walletservice.wallet.application.port.out.LoadWalletPort
import woo.walletservice.wallet.application.port.out.SaveWalletPort
import woo.walletservice.wallet.domain.PaymentEventMessage
import woo.walletservice.wallet.domain.Wallet

@PersistentAdapter
class WalletPersistenceAdapter(
    private val walletTransactionRepository: WalletTransactionRepository,
    private val walletRepository: WalletRepository
) : DuplicateMessageFilterPort, LoadWalletPort, SaveWalletPort {

    override fun isAlreadyProcess(paymentEventMessage: PaymentEventMessage): Boolean {
        return walletTransactionRepository.isExist(paymentEventMessage)
    }

    override fun getWallets(sellerIds: Set<Long>): Set<Wallet> {
        return walletRepository.getWallets(sellerIds)
    }

    override fun save(wallets: List<Wallet>) {
        return walletRepository.save(wallets)
    }
}