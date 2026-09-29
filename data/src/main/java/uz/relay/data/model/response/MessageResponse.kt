package uz.relay.data.model.response

import kotlinx.serialization.Serializable

/**
 * To'liq xabar: `message_new` update'ining payload'i va xabarlar sahifasi (`GET .../messages`) elementi.
 * Mapper orqali Room'dagi xabar jadvaliga yoziladi; `clientMessageId` bo'yicha upsert qilingani uchun
 * o'zimiz yuborgan xabarning echo'si dublikat hosil qilmaydi.
 */
@Serializable
data class MessageResponse(
    /** Qurilmada yaratilgan UUID — xabarning universal identifikatori (lokal primary key bo'ladi). */
    val clientMessageId: String,
    val serverId: Long,
    val chatId: String,
    val senderId: String,
    /** Chat ichidagi tartib raqami: qat'iy o'sadi, teshiksiz. */
    val serverSeq: Long,
    /** TEXT | IMAGE | VIDEO | FILE | SYSTEM */
    val type: String,
    /** TEXT uchun matn yoki caption; SYSTEM uchun JSON satr (SystemMessageBody). */
    val body: String? = null,
    val replyToClientMessageId: String? = null,
    /** IMAGE/VIDEO/FILE xabarning fayllari. Thumbnail yo'q — server uni hech qayerda qaytarmaydi. */
    val media: List<MediaMetaResponse> = emptyList(),
    val createdAt: Long,
    val editedAt: Long? = null,
    val editVersion: Int = 0,
    val deletedAt: Long? = null
)

/** Media meta'si (`MediaMeta`). */
@Serializable
data class MediaMetaResponse(
    val mediaId: String,
    /** IMAGE | VIDEO | FILE */
    val kind: String,
    val mimeType: String,
    val sizeBytes: Long,
    val width: Int? = null,
    val height: Int? = null,
    val durationMs: Long? = null,
    /** UPLOADING | READY */
    val status: String = "READY"
)

/** SYSTEM xabarning `body`si ichidagi JSON. */
@Serializable
data class SystemMessageBodyResponse(
    val event: String,
    val actorId: String,
    val targetUserIds: List<String> = emptyList(),
    val title: String? = null
)
