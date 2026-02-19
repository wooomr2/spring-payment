package woo.walletservice.wallet.adapter.out.persistence.repository.jpa

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import woo.walletservice.wallet.adapter.out.persistence.entity.PaymentOrderEntity
import woo.walletservice.wallet.adapter.out.persistence.entity.PaymentOrderMapper
import woo.walletservice.wallet.adapter.out.persistence.repository.PaymentOrderRepository
import woo.walletservice.wallet.domain.PaymentOrder

@Repository
class JpaPaymentOrderRepository(
    private val springDataJpaPaymentOrderRepository: SpringDataJpaPaymentOrderRepository,
    private val paymentOrderMapper: PaymentOrderMapper
) : PaymentOrderRepository {
    override fun getPaymentOrders(orderId: String): List<PaymentOrder> {
        return springDataJpaPaymentOrderRepository.findByOrderId(orderId).map { paymentOrderMapper.toDomain(it) }
    }
}

interface SpringDataJpaPaymentOrderRepository : JpaRepository<PaymentOrderEntity, Long> {
    fun findByOrderId(orderId: String): List<PaymentOrderEntity>
}