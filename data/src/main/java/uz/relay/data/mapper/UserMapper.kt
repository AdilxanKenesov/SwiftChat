package uz.relay.data.mapper

import uz.relay.data.model.response.UserMeResponse
import uz.relay.data.model.response.UserResponse
import uz.relay.data.source.local.database.entity.UserEntity
import uz.relay.domain.model.User

/**
 * Foydalanuvchi DTO'lari ↔ Room entity ↔ domain [User] o'rtasidagi mapper'lar.
 *
 * Nega uch qatlam: network DTO server formatiga, entity Room jadvaliga, domain modeli esa UI/use-case'ga
 * xizmat qiladi — har biri boshqasidan mustaqil o'zgara oladi. Repository'lar, SyncEngine va UserCache ishlatadi.
 */

/** O'z profilim javobini to'g'ridan-to'g'ri domain modeliga (bazaga yozmasdan) aylantiradi. */
fun UserMeResponse.toUser() = User(
    id = id,
    username = username,
    displayName = displayName,
    avatarMediaId = avatarMediaId,
    avatarVersion = avatarVersion,
    phone = phone
)

/** O'z profilim — `phone` faqat shu javobda keladi, shuning uchun u ham keshga yoziladi. */
fun UserMeResponse.toEntity() = UserEntity(
    id = id,
    displayName = displayName,
    username = username,
    avatarMediaId = avatarMediaId,
    avatarVersion = avatarVersion,
    online = online,
    lastSeenAt = lastSeenAt,
    phone = phone
)

/** Boshqa foydalanuvchi profili: telefon raqami server tomonidan berilmaydi (maxfiylik). */
fun UserResponse.toEntity() = UserEntity(
    id = id,
    displayName = displayName,
    username = username,
    avatarMediaId = avatarMediaId,
    avatarVersion = avatarVersion,
    online = online,
    lastSeenAt = lastSeenAt
)

/** Keshdagi qatorni UI ko'rsatadigan domain modeliga aylantiradi. */
fun UserEntity.toDomain() = User(
    id = id,
    username = username,
    displayName = displayName,
    avatarMediaId = avatarMediaId,
    avatarVersion = avatarVersion,
    phone = phone,
    online = online,
    lastSeenAt = lastSeenAt
)
