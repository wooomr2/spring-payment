package woo.walletservice.wallet.adapter.out.persistence.entity

import jakarta.persistence.*
import woo.walletservice.wallet.domain.TransactionType
import java.math.BigDecimal

@Entity
@Table(name = "wallet_transactions")
class WalletTransactionEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "wallet_id")
    val walletId: Long,

    val amount: BigDecimal,

    @Enumerated(EnumType.STRING)
    val type: TransactionType,

    @Column(name = "reference_type")
    val referenceType: String,

    @Column(name = "reference_id")
    val referenceId: Long,

    @Column(name = "order_id")
    val orderId: String,

    @Column(name = "idempotency_key", unique = true)
    val idempotencyKey: String,
)
