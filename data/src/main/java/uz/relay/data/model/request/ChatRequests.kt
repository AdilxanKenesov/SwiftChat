package uz.relay.data.model.request

import kotlinx.serialization.Serializable

/* Chat va guruh boshqaruvi so'rovlarining DTO'lari ([uz.relay.data.source.network.api.ChatApi] uchun). */

/** Shaxsiy (DIRECT) chat ochish; server mavjud chatni qaytaradi, dublikat yaratmaydi. */
@Serializable
data class CreateDirectRequest(
    val peerUserId: String
)

/** Yangi guruh yaratish. */
@Serializable
data class CreateGroupRequest(
    val title: String,
    /** Men (yaratuvchi, OWNER) bu ro'yxatga kirmayman. */
    val memberIds: List<String>
)

/** Guruh ma'lumotini PATCH qilish: `null` maydon yuborilmaydi (Json `explicitNulls = false`). */
@Serializable
data class UpdateChatRequest(
    val title: String? = null
)

/** Chatning shaxsiy sozlamalari (hozircha ovozsiz rejim). */
@Serializable
data class ChatSettingsRequest(
    val muted: Boolean,
    /** `null` — muddatsiz ovozsiz. */
    val mutedUntil: Long? = null
)

/** Guruhga a'zo qo'shish. */
@Serializable
data class AddMembersRequest(
    val userIds: List<String>
)

/** A'zoning rolini o'zgartirish. */
@Serializable
data class ChangeRoleRequest(
    /** ADMIN | MEMBER (OWNER faqat egasi chiqib ketganda avtomatik o'tadi). */
    val role: String
)
