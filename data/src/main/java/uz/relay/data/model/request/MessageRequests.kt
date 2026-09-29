package uz.relay.data.model.request

import kotlinx.serialization.Serializable

/** REST orqali xabar yuborish (socket ulanmagan paytdagi zaxira yo'l). */
@Serializable
data class SendMessageRequest(
    /** Qurilmada yaratilgan UUID — idempotentlik kaliti: qayta yuborilsa ham dublikat bo'lmaydi. */
    val clientMessageId: String,
    /** TEXT | IMAGE | VIDEO | FILE */
    val type: String,
    val body: String? = null,
    val mediaIds: List<String>? = null,
    /** Javob berilayotgan xabarning clientMessageId'si (shu chat ichida). */
    val replyTo: String? = null
)

@Serializable
data class EditMessageRequest(
    val body: String
)
