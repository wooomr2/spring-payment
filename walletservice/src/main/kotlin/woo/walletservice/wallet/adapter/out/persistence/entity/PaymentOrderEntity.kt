package woo.walletservice.wallet.adapter.out.persistence.entity

import jakarta.persistence.*
import java.math.BigDecimal

@Entity
@Table(name = "payment_orders")
class PaymentOrderEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "seller_id")
    val sellerId: Long,

    val amount: BigDecimal,

    @Column(name = "order_id")
    val orderId: String
)