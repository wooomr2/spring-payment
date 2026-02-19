package woo.walletservice.wallet.adapter.out.persistence.entity

import org.springframework.stereotype.Component
import woo.walletservice.common.IdempodencyCreater
import woo.walletservice.wallet.domain.WalletTransaction

@Component
class WalletTransactionMapper {

    fun toEntity(walletTransaction: WalletTransaction): WalletTransactionEntity {
        return WalletTransactionEntity(
            walletId = walletTransaction.walletId,
            amount = walletTransaction.amount.toBigDecimal(),
            type = walletTransaction.type,
            referenceType = walletTransaction.referenceType.name,
            referenceId = walletTransaction.referenceId,
            orderId = walletTransaction.orderId,
            idempotencyKey = IdempodencyCreater.create(walletTransaction),
        )
    }
}