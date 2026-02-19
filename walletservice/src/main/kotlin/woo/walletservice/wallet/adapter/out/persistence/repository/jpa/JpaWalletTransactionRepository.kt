package woo.walletservice.wallet.adapter.out.persistence.repository.jpa

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import woo.walletservice.wallet.adapter.out.persistence.entity.WalletTransactionEntity
import woo.walletservice.wallet.adapter.out.persistence.entity.WalletTransactionMapper
import woo.walletservice.wallet.adapter.out.persistence.repository.WalletTransactionRepository
import woo.walletservice.wallet.domain.PaymentEventMessage
import woo.walletservice.wallet.domain.WalletTransaction

@Repository
class JpaWalletTransactionRepository(
    private val springDataJpaWalletTransactionRepository: SpringDataJpaWalletTransactionRepository,
    private val walletTransactionMapper: WalletTransactionMapper
) : WalletTransactionRepository {

    override fun isExist(paymentEventMessage: PaymentEventMessage): Boolean {
        return springDataJpaWalletTransactionRepository.existsByOrderId(paymentEventMessage.orderId())
    }

    override fun saveAll(walletTransactions: List<WalletTransaction>) {
        springDataJpaWalletTransactionRepository.saveAll(
            walletTransactions.map { walletTransactionMapper.toEntity(it) }
        )
    }
}

interface SpringDataJpaWalletTransactionRepository : JpaRepository<WalletTransactionEntity, Long> {

    fun existsByOrderId(orderId: String): Boolean
}