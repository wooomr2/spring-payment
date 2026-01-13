package woo.paymentservice.payment.domain

/**
 * 결제수단입니다. 카드, 가상계좌, 간편결제, 휴대폰, 계좌이체, 문화상품권, 도서문화상품권, 게임문화상품권 중 하나입니다.
 * */
enum class PaymentMethod(val korMethodName: String) {
    CARD("카드"),
    VIRTUAL("가상계좌"),
    EASY_PAY("간편결제"),
    MOBILE("휴대폰"),
    TRANSFER("계좌이체"),
    CULTURE_GIFT("문화상품권"),
    BOOK_CULTURE_GIFT("도서문화상품권"),
    GAME_CULTURE_GIFT("게임문화상품권"),
    ;

    companion object {
        fun get(method: String): PaymentMethod {
            return entries.find { it.korMethodName == method } ?: error("PaymentMethod($method)을 찾을 수 없습니다.")
        }
    }
}