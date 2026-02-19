package woo.walletservice.wallet.adapter.out.persistence.entity

import org.springframework.stereotype.Component
import woo.walletservice.wallet.domain.Wallet

@Component
class WalletMapper {

    fun toDomain(entity: WalletEntity): Wallet {
        return Wallet(
            id = entity.id!!,
            userId = entity.userId,
            balance = entity.balance,
            version = entity.version
        )
    }

    fun toEntity(domain: Wallet): WalletEntity {
        return WalletEntity(
            id = domain.id,
            userId = domain.userId,
            balance = domain.balance,
            version = domain.version
        )
    }
}