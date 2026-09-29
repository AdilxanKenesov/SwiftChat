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

/** `POST /v1/media/uploads` — faylni e'lon qilish (baytlar keyin bo'laklab yuboriladi). */
@Serializable
data class StartUploadRequest(
    /** IMAGE | VIDEO | FILE */
    val kind: String,
    val mimeType: String,
    val sizeBytes: Long,
    /** Butun faylning SHA-256 (hex). Oxirgi bo'lakdan keyin server tekshiradi. */
    val sha256: String,
    val width: Int? = null,
    val height: Int? = null,
    val durationMs: Long? = null,
    /** ≤ 8 KB kichik rasm (base64). Server uni saqlaydi, lekin hozircha qaytarmaydi. */
    val thumbBase64: String? = null
)
