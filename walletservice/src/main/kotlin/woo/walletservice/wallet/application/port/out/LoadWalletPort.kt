package woo.walletservice.wallet.application.port.out

import woo.walletservice.wallet.domain.Wallet

interface LoadWalletPort {

    fun getWallets(sellerIds: Set<Long>): Set<Wallet>
}