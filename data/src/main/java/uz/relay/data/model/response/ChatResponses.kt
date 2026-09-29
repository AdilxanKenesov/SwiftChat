package uz.relay.data.model.response

import kotlinx.serialization.Serializable

/**
 * `GET /v1/chats` dagi `Chat` obyekti.
 *
 * `type` maydonlari enum emas, String: server kelajakda yangi qiymat qo'shsa (kontrakt faqat qo'shimcha
 * o'zgaradi), JSON parse yiqilmasligi kerak. Enum'ga aylantirish mapper'da, noma'lumi UNKNOWN bo'ladi.
 */
@Serializable
data class ChatResponse(
    val id: String,
    /** DIRECT | GROUP */
    val type: String,
    val title: String? = null,
    val avatarMediaId: String? = null,
    /** Faqat DIRECT chat uchun. */
    val peerUserId: String? = null,
    val lastMessage: MessagePreviewResponse? = null,
    val lastActivityAt: Long,
    val unreadCount: Int = 0,
    /** MENING o'qish kursorim: shu serverSeq gacha o'qiganman. */
    val readUpToSeq: Long = 0,
    /** Chatdagi eng katta serverSeq. */
    val topSeq: Long = 0,
    val muted: Boolean = false,
    val mutedUntil: Long? = null
)

/** Chatlar ro'yxatidagi "oxirgi xabar" ko'rinishi (to'liq xabar emas — media va h.k. yo'q). */
@Serializable
data class MessagePreviewResponse(
    val serverId: Long,
    val senderId: String,
    /** TEXT | IMAGE | VIDEO | FILE | SYSTEM */
    val type: String,
    val body: String? = null,
    val serverSeq: Long,
    val createdAt: Long,
    val deletedAt: Long? = null
)

/** Chatlar ro'yxatining bitta sahifasi (cursor asosida sahifalash). */
@Serializable
data class ChatListPageResponse(
    val chats: List<ChatResponse>,
    /** `null` — bu oxirgi sahifa. */
    val nextCursor: String? = null
)
