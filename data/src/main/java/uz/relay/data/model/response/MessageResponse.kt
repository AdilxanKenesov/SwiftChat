package uz.relay.data.model.response

import kotlinx.serialization.Serializable

/**
 * To'liq xabar (`message_new` update'ining payload'i). Hozircha undan faqat chatlar ro'yxatidagi
 * "oxirgi xabar" yangilanadi; xabarlar jadvali suhbat bosqichida qo'shiladi.
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
    val createdAt: Long,
    val editedAt: Long? = null,
    val editVersion: Int = 0,
    val deletedAt: Long? = null
)

/** SYSTEM xabarning `body`si ichidagi JSON. */
@Serializable
data class SystemMessageBodyResponse(
    val event: String,
    val actorId: String,
    val targetUserIds: List<String> = emptyList(),
    val title: String? = null
)
