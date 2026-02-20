package woo.walletservice.wallet.adapter.out.persistence.repository.jpa

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.orm.ObjectOptimisticLockingFailureException
import org.springframework.stereotype.Repository
import org.springframework.transaction.support.TransactionTemplate
import woo.walletservice.wallet.adapter.out.persistence.entity.WalletEntity
import woo.walletservice.wallet.adapter.out.persistence.entity.WalletMapper
import woo.walletservice.wallet.adapter.out.persistence.exception.RetryExhaustedWithOptimisticLockingFailureException
import woo.walletservice.wallet.adapter.out.persistence.repository.WalletRepository
import woo.walletservice.wallet.adapter.out.persistence.repository.WalletTransactionRepository
import woo.walletservice.wallet.domain.Wallet
import java.math.BigDecimal

@Repository
class JpaWalletRepository(
    private val springDataJpaWalletRepository: SpringDataJpaWalletRepository,
    private val walletMapper: WalletMapper,
    private val walletTransactionRepository: WalletTransactionRepository,
    private val transactionTemplate: TransactionTemplate
) : WalletRepository {

    override fun getWallets(sellerIds: Set<Long>): Set<Wallet> {
        return springDataJpaWalletRepository.findByUserIdIn(sellerIds).map {
            walletMapper.toDomain(it)
        }.toSet()
    }

    override fun save(wallets: List<Wallet>) {
        try {
            performSaveOperation(wallets)
        } catch (e: ObjectOptimisticLockingFailureException) {
            retrySaveOperation(wallets)
        }
    }

    private fun performSaveOperation(wallets: List<Wallet>) {
        transactionTemplate.execute {
            springDataJpaWalletRepository.saveAll(wallets.map { walletMapper.toEntity(it) });
            walletTransactionRepository.saveAll(wallets.flatMap { it.walletTransactions })
        }
    }

    private fun retrySaveOperation(wallets: List<Wallet>, maxRetries: Int = 3, baseDely: Int = 100) {
        var retryCount = 0

        while (true) {
            try {
                performSaveOperationWithRecent(wallets)
                break
            } catch (e: ObjectOptimisticLockingFailureException) {
                if (++retryCount > maxRetries) {
                    throw RetryExhaustedWithOptimisticLockingFailureException(
                        e.message ?: "Retry exhausted after $maxRetries attempts due to optimistic locking failure."
                    )
                }
                waitForNextRetry(baseDely)
            }
        }
    }

    private fun performSaveOperationWithRecent(wallets: List<Wallet>) {
        val recentWallets = springDataJpaWalletRepository.findByIdIn(wallets.map { it.id }.toSet())
        val receltWalletsById = recentWallets.associateBy { it.id }

        val walletPairs = wallets.map { wallet ->
            Pair(wallet, receltWalletsById[wallet.id]!!)
        }

        val updatedWallets = walletPairs.map {
            it.second.addBalance(
                BigDecimal(it.first.walletTransactions.sumOf { v -> v.amount })
            )
        }

        transactionTemplate.execute {
            springDataJpaWalletRepository.saveAll(updatedWallets)
            walletTransactionRepository.saveAll(wallets.flatMap { it.walletTransactions })
        }
    }

    private fun waitForNextRetry(baseDelay: Int) {
        val jitter = (Math.random() * baseDelay).toLong()

        try {
            Thread.sleep(jitter)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            throw RuntimeException("Interrupted during retry wait", e)
        }
    }
}

interface SpringDataJpaWalletRepository : JpaRepository<WalletEntity, Long> {
    fun findByUserIdIn(userIds: Set<Long>): List<WalletEntity>
    fun findByIdIn(ids: Set<Long>): List<WalletEntity>
}