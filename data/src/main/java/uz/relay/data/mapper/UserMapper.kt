package uz.relay.data.mapper

import uz.relay.data.model.response.UserMeResponse
import uz.relay.data.model.response.UserResponse
import uz.relay.data.source.local.database.entity.UserEntity
import uz.relay.domain.model.User

fun UserMeResponse.toUser() = User(
    id = id,
    username = username,
    displayName = displayName,
    avatarMediaId = avatarMediaId,
    avatarVersion = avatarVersion,
    phone = phone
)

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

fun UserResponse.toEntity() = UserEntity(
    id = id,
    displayName = displayName,
    username = username,
    avatarMediaId = avatarMediaId,
    avatarVersion = avatarVersion,
    online = online,
    lastSeenAt = lastSeenAt
)

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
