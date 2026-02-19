package woo.walletservice.wallet.adapter.out.persistence.repository

import woo.walletservice.wallet.domain.Wallet

interface WalletRepository {

    fun getWallets(sellerIds: Set<Long>): Set<Wallet>

    fun save(wallets: List<Wallet>)
}