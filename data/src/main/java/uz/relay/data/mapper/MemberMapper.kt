package uz.relay.data.mapper

import uz.relay.data.model.response.ChatMemberResponse
import uz.relay.data.source.local.database.dao.MemberItem
import uz.relay.data.source.local.database.entity.ChatMemberEntity
import uz.relay.domain.model.ChatMember
import uz.relay.domain.model.MemberRole

/**
 * Guruh a'zolari mapper'lari: server javobi → Room entity → domain [ChatMember].
 *
 * GroupRepositoryImpl va UpdateApplier (a'zolik update'lari) ishlatadi. Rol satr ko'rinishida saqlanadi —
 * server yangi rol qo'shsa ham baza buzilmaydi, domain'da esa UNKNOWN bo'lib qoladi.
 */

/** Serverdagi a'zo javobida `chatId` yo'q — u so'rov kontekstidan beriladi. */
fun ChatMemberResponse.toEntity(chatId: String) = ChatMemberEntity(
    chatId = chatId,
    userId = userId,
    role = role,
    joinedAt = joinedAt
)

/** A'zo + profil (JOIN) qatori → domain; `isMe` ro'yxatda "Siz" belgisi va o'zini chiqarib bo'lmaslik uchun. */
fun MemberItem.toDomain(myUserId: String?) = ChatMember(
    userId = member.userId,
    displayName = displayName,
    role = member.role.toMemberRole(),
    online = online == true,
    lastSeenAt = lastSeenAt,
    isMe = member.userId == myUserId
)

/** Server rol satrini enum'ga aylantiradi; noma'lum qiymat ilovani yiqitmasligi uchun UNKNOWN. */
fun String.toMemberRole(): MemberRole = when (this) {
    "OWNER" -> MemberRole.OWNER
    "ADMIN" -> MemberRole.ADMIN
    "MEMBER" -> MemberRole.MEMBER
    else -> MemberRole.UNKNOWN
}
