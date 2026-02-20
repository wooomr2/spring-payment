package woo.walletservice.wallet.adapter.out.persistence.repository.jpa

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.RepeatedTest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import woo.walletservice.wallet.adapter.out.persistence.entity.WalletEntity
import woo.walletservice.wallet.adapter.out.persistence.entity.WalletMapper
import woo.walletservice.wallet.domain.Item
import woo.walletservice.wallet.domain.ReferenceType
import java.math.BigDecimal
import java.util.*
import java.util.concurrent.Executors

@SpringBootTest
class JpaWalletRepositoryTest(
    @Autowired private val walletRepository: JpaWalletRepository,
    @Autowired private val springDataJpaWalletRepository: SpringDataJpaWalletRepository,
    @Autowired private val springDataJpaWalletTransactionRepository: SpringDataJpaWalletTransactionRepository,
    @Autowired private val walletMapper: WalletMapper,
) {

    @BeforeEach
    fun clean() {
        springDataJpaWalletTransactionRepository.deleteAll()
        springDataJpaWalletRepository.deleteAll()
    }

    @RepeatedTest(value = 5)
    fun `should update the blanace of wallet successfully when execute the updated command at the same time`() {
        val walletEntity1 = WalletEntity(
            userId = 1L,
            balance = BigDecimal.ZERO,
            version = 0
        )
        val walletEntity2 = WalletEntity(
            userId = 2L,
            balance = BigDecimal.ZERO,
            version = 0
        )

        springDataJpaWalletRepository.saveAll(listOf(walletEntity1, walletEntity2))

        val baseWallet1 = walletMapper.toDomain(walletEntity1)
        val baseWallet2 = walletMapper.toDomain(walletEntity2)

        val items1 = listOf(
            Item(
                amount = 1000L,
                referenceId = 1L,
                referenceType = ReferenceType.PAYMENT_ORDER,
                orderId = UUID.randomUUID().toString()
            ),
        )

        val items2 = listOf(
            Item(
                amount = 2000L,
                referenceId = 2L,
                referenceType = ReferenceType.PAYMENT_ORDER,
                orderId = UUID.randomUUID().toString()
            ),
        )

        val items3 = listOf(
            Item(
                amount = 3000L,
                referenceId = 3L,
                referenceType = ReferenceType.PAYMENT_ORDER,
                orderId = UUID.randomUUID().toString()
            ),
        )

        val updatedWallet1 = baseWallet1.calculateBalanceWith(items1)
        val updatedWallet2 = baseWallet1.calculateBalanceWith(items2)
        val updatedWallet3 = baseWallet1.calculateBalanceWith(items3)

        val updatedWallet4 = baseWallet2.calculateBalanceWith(items1)
        val updatedWallet5 = baseWallet2.calculateBalanceWith(items2)
        val updatedWallet6 = baseWallet2.calculateBalanceWith(items3)

        val executorService = Executors.newFixedThreadPool(3)

        val future1 = executorService.submit { walletRepository.save(listOf(updatedWallet1, updatedWallet4)) }
        val future2 = executorService.submit { walletRepository.save(listOf(updatedWallet2, updatedWallet5)) }
        val future3 = executorService.submit { walletRepository.save(listOf(updatedWallet3, updatedWallet6)) }

        future1.get()
        future2.get()
        future3.get()

        val retrievedWallet1 = springDataJpaWalletRepository.findById(baseWallet1.id).get()
        val retrievedWallet2 = springDataJpaWalletRepository.findById(baseWallet2.id).get()

        assertThat(retrievedWallet1.version).isEqualTo(3)
        assertThat(retrievedWallet2.version).isEqualTo(3)

        assertThat(retrievedWallet1.balance.toInt()).isEqualTo(6000)
        assertThat(retrievedWallet2.balance.toInt()).isEqualTo(6000)
    }
}