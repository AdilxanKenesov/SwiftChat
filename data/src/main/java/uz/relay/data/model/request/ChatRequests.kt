package uz.relay.data.model.request

import kotlinx.serialization.Serializable

@Serializable
data class CreateDirectRequest(
    val peerUserId: String
)

@Serializable
data class CreateGroupRequest(
    val title: String,
    /** Men (yaratuvchi, OWNER) bu ro'yxatga kirmayman. */
    val memberIds: List<String>
)

@Serializable
data class UpdateChatRequest(
    val title: String? = null
)

@Serializable
data class ChatSettingsRequest(
    val muted: Boolean,
    /** `null` — muddatsiz ovozsiz. */
    val mutedUntil: Long? = null
)

@Serializable
data class AddMembersRequest(
    val userIds: List<String>
)

@Serializable
data class ChangeRoleRequest(
    /** ADMIN | MEMBER (OWNER faqat egasi chiqib ketganda avtomatik o'tadi). */
    val role: String
)
