package uz.relay.data.model.response

import kotlinx.serialization.Serializable

/** Chat a'zosi: roli, o'qish kursori va presence (online/lastSeen) holati bilan. */
@Serializable
data class ChatMemberResponse(
    val userId: String,
    /** OWNER | ADMIN | MEMBER */
    val role: String,
    val joinedAt: Long = 0,
    val readUpToSeq: Long = 0,
    val online: Boolean = false,
    val lastSeenAt: Long? = null
)

/** `POST /v1/chats/{id}/members` javobi — o'zgarishdan keyingi TO'LIQ a'zolar ro'yxati. */
@Serializable
data class MembersResponse(
    val members: List<ChatMemberResponse>
)

/** Foydalanuvchilarni qidirish natijasi (username/ism bo'yicha). */
@Serializable
data class UserSearchResponse(
    val users: List<UserResponse>
)
