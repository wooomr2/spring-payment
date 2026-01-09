package woo.paymentservice.common

import java.util.*

object IdempodencyCreater {

    fun create(data: Any): String {
        return UUID.nameUUIDFromBytes(data.toString().toByteArray()).toString()
    }
}