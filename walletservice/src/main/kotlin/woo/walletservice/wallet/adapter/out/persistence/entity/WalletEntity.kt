package woo.walletservice.wallet.adapter.out.persistence.entity

import jakarta.persistence.*
import java.math.BigDecimal

@Entity
@Table(name = "wallets")
class WalletEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "user_id")
    val userId: Long,

    val balance: BigDecimal,

    @Version
    val version: Int
)