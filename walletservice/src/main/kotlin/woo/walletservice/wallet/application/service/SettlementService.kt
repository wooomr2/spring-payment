package woo.walletservice.wallet.application.service

import woo.walletservice.common.annotation.UseCase
import woo.walletservice.wallet.application.port.`in`.SettlementUseCase
import woo.walletservice.wallet.application.port.out.DuplicateMessageFilterPort
import woo.walletservice.wallet.application.port.out.LoadPaymentOrderPort
import woo.walletservice.wallet.application.port.out.LoadWalletPort
import woo.walletservice.wallet.application.port.out.SaveWalletPort
import woo.walletservice.wallet.domain.PaymentEventMessage
import woo.walletservice.wallet.domain.WalletEventMessage
import woo.walletservice.wallet.domain.WalletEventMessageType

@UseCase
class SettlementService(
    private val duplicateMessageFilterPort: DuplicateMessageFilterPort,
    private val loadPaymentOrderPort: LoadPaymentOrderPort,
    private val loadWalletPort: LoadWalletPort,
    private val saveWalletPort: SaveWalletPort
) : SettlementUseCase {

    override fun processSettlement(paymentEventMessage: PaymentEventMessage): WalletEventMessage {
        if (duplicateMessageFilterPort.isAlreadyProcess(paymentEventMessage)) {
            return createWalletEventMessage(paymentEventMessage)
        }

        val paymentOrders = loadPaymentOrderPort.getPaymentOrders(paymentEventMessage.orderId())

        val sellerIdPaymentOrderListMap = paymentOrders.groupBy { it.sellerId }
        val sellerIds = sellerIdPaymentOrderListMap.keys

        val wallets = loadWalletPort.getWallets(sellerIds)

        val updatedWallets =
            wallets.map { wallet -> wallet.calculateBalanceWith(sellerIdPaymentOrderListMap[wallet.userId]!!) }
                .toList()

        saveWalletPort.save(updatedWallets)

        return createWalletEventMessage(paymentEventMessage)
    }

    private fun createWalletEventMessage(paymentEventMessage: PaymentEventMessage): WalletEventMessage {
        return WalletEventMessage(
            type = WalletEventMessageType.SUCCESS,
            payload = mapOf(
                "orderId" to paymentEventMessage.orderId()
            ),
        )
    }
}