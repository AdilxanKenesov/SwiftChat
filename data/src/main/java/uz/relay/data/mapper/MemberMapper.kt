package uz.relay.data.mapper

import uz.relay.data.model.response.ChatMemberResponse
import uz.relay.data.source.local.database.dao.MemberItem
import uz.relay.data.source.local.database.entity.ChatMemberEntity
import uz.relay.domain.model.ChatMember
import uz.relay.domain.model.MemberRole

fun ChatMemberResponse.toEntity(chatId: String) = ChatMemberEntity(
    chatId = chatId,
    userId = userId,
    role = role,
    joinedAt = joinedAt
)

fun MemberItem.toDomain(myUserId: String?) = ChatMember(
    userId = member.userId,
    displayName = displayName,
    role = member.role.toMemberRole(),
    online = online == true,
    lastSeenAt = lastSeenAt,
    isMe = member.userId == myUserId
)

fun String.toMemberRole(): MemberRole = when (this) {
    "OWNER" -> MemberRole.OWNER
    "ADMIN" -> MemberRole.ADMIN
    "MEMBER" -> MemberRole.MEMBER
    else -> MemberRole.UNKNOWN
}
