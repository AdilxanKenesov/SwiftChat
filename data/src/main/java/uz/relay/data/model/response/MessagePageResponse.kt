package uz.relay.data.model.response

import kotlinx.serialization.Serializable

/** `GET /v1/chats/{id}/messages` — serverSeq bo'yicha kamayish tartibida (eng yangisi birinchi). */
@Serializable
data class MessagePageResponse(
    val messages: List<MessageResponse>,
    /** `true` — bundan eskiroq xabarlar ham bor (yuqoriga scroll qilinganda yuklanadi). */
    val hasMore: Boolean
)

/** Xabar qabul qilindi: REST javobi va WebSocket `ack` frame'i bir xil ma'lumot beradi. */
@Serializable
data class SendMessageResultResponse(
    val clientMessageId: String,
    val serverId: Long,
    val serverSeq: Long,
    val serverCreatedAt: Long
)
