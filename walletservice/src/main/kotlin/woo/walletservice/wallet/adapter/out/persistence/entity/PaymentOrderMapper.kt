package woo.walletservice.wallet.adapter.out.persistence.entity

import org.springframework.stereotype.Component
import woo.walletservice.wallet.domain.PaymentOrder

@Component
class PaymentOrderMapper {

    fun toDomain(entity: PaymentOrderEntity): PaymentOrder {
        return PaymentOrder(
            id = entity.id!!,
            sellerId = entity.sellerId,
            amount = entity.amount.toLong(),
            orderId = entity.orderId
        )
    }
}