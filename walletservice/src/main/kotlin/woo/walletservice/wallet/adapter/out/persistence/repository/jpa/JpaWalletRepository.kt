package woo.walletservice.wallet.adapter.out.persistence.repository.jpa

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import woo.walletservice.wallet.adapter.out.persistence.entity.WalletEntity
import woo.walletservice.wallet.adapter.out.persistence.entity.WalletMapper
import woo.walletservice.wallet.adapter.out.persistence.repository.WalletRepository
import woo.walletservice.wallet.adapter.out.persistence.repository.WalletTransactionRepository
import woo.walletservice.wallet.domain.Wallet

@Repository
class JpaWalletRepository(
    private val springDataJpaWalletRepository: SpringDataJpaWalletRepository,
    private val walletMapper: WalletMapper,
    private val walletTransactionRepository: WalletTransactionRepository
) : WalletRepository {

    override fun getWallets(sellerIds: Set<Long>): Set<Wallet> {
        return springDataJpaWalletRepository.findByUserIdIn(sellerIds).map {
            walletMapper.toDomain(it)
        }.toSet()
    }

    @Transactional
    override fun save(wallets: List<Wallet>) {
        springDataJpaWalletRepository.saveAll(wallets.map { walletMapper.toEntity(it) });
        walletTransactionRepository.saveAll(wallets.flatMap { it.walletTransactions })
    }
}

interface SpringDataJpaWalletRepository : JpaRepository<WalletEntity, Long> {
    fun findByUserIdIn(userIds: Set<Long>): List<WalletEntity>
}