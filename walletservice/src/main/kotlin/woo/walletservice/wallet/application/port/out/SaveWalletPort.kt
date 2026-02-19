package woo.walletservice.wallet.application.port.out

import woo.walletservice.wallet.domain.Wallet

interface SaveWalletPort {

    fun save(wallets: List<Wallet>)
}