package woo.walletservice.wallet.application.service

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import woo.walletservice.wallet.adapter.out.persistence.entity.WalletEntity
import woo.walletservice.wallet.adapter.out.persistence.repository.jpa.SpringDataJpaWalletRepository
import woo.walletservice.wallet.adapter.out.persistence.repository.jpa.SpringDataJpaWalletTransactionRepository
import woo.walletservice.wallet.application.port.out.DuplicateMessageFilterPort
import woo.walletservice.wallet.application.port.out.LoadPaymentOrderPort
import woo.walletservice.wallet.application.port.out.LoadWalletPort
import woo.walletservice.wallet.application.port.out.SaveWalletPort
import woo.walletservice.wallet.domain.PaymentEventMessage
import woo.walletservice.wallet.domain.PaymentEventMessageType
import woo.walletservice.wallet.domain.PaymentOrder
import woo.walletservice.wallet.domain.WalletEventMessageType
import java.math.BigDecimal
import java.util.*

@SpringBootTest
class SettlementServiceTest(
    @Autowired private val duplicateMessageFilterPort: DuplicateMessageFilterPort,
    @Autowired private val loadPaymentOrderPort: LoadPaymentOrderPort,
    @Autowired private val loadWalletPort: LoadWalletPort,
    @Autowired private val saveWalletPort: SaveWalletPort,
    @Autowired private val springDataJpaWalletRepository: SpringDataJpaWalletRepository,
    @Autowired private val springDataJpaWalletTransactionRepository: SpringDataJpaWalletTransactionRepository
) {

    @BeforeEach
    fun clean() {
        springDataJpaWalletTransactionRepository.deleteAll()
        springDataJpaWalletRepository.deleteAll()
    }

    @Test
    fun `should process settlement succesfully`() {
        val walletEntities = listOf(
            WalletEntity(
                userId = 1L,
                balance = BigDecimal.ZERO,
                version = 0,
            ),
            WalletEntity(
                userId = 2L,
                balance = BigDecimal.ZERO,
                version = 0,
            )
        )

        springDataJpaWalletRepository.saveAll(walletEntities)


        val paymentEventMessage = PaymentEventMessage(
            type = PaymentEventMessageType.PAYMENT_CONFIRMATION_SUCCESS,
            payload = mapOf(
                "orderId" to UUID.randomUUID().toString()
            ),
            metadata = mapOf(
                "test" to UUID.randomUUID().toString()
            )
        )

        val mockLoadPaymentOrderRepository = mockk<LoadPaymentOrderPort>()

        every {
            mockLoadPaymentOrderRepository.getPaymentOrders(paymentEventMessage.orderId())
        } returns listOf(
            PaymentOrder(
                id = 1,
                sellerId = 1,
                amount = 3000L,
                orderId = paymentEventMessage.orderId()
            ),
            PaymentOrder(
                id = 2,
                sellerId = 2,
                amount = 4000L,
                orderId = paymentEventMessage.orderId()
            )
        )

        val settlementService = SettlementService(
            duplicateMessageFilterPort = duplicateMessageFilterPort,
            loadPaymentOrderPort = mockLoadPaymentOrderRepository,
            loadWalletPort = loadWalletPort,
            saveWalletPort = saveWalletPort
        )

        val walletEventMessage = settlementService.processSettlement(paymentEventMessage)

        val updatedWallets = loadWalletPort.getWallets(walletEntities.map { it.userId }.toSet())
            .sortedBy { it.userId }

        assertThat(walletEventMessage.payload["orderId"]).isEqualTo(paymentEventMessage.orderId())
        assertThat(walletEventMessage.type).isEqualTo(WalletEventMessageType.SUCCESS)
        assertThat(updatedWallets[0].balance.toInt()).isEqualTo(3000)
        assertThat(updatedWallets[1].balance.toInt()).isEqualTo(4000)
    }
}