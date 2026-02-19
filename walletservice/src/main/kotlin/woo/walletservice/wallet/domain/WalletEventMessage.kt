package woo.walletservice.wallet.domain

data class WalletEventMessage(
    val type: WalletEventMessageType,
    val payload: Map<String, Any>,
)

enum class WalletEventMessageType(description: String) {
    SUCCESS("정산 성공"),
}