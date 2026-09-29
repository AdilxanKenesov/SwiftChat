package uz.relay.data.mapper

import uz.relay.data.model.response.TokenPairResponse
import uz.relay.data.model.response.UserMeResponse
import uz.relay.data.source.local.Session
import uz.relay.domain.model.User

fun TokenPairResponse.toSession() = Session(
    accessToken = accessToken,
    refreshToken = refreshToken,
    userId = userId,
    deviceId = deviceId
)

fun UserMeResponse.toUser() = User(
    id = id,
    username = username,
    displayName = displayName,
    avatarMediaId = avatarMediaId,
    avatarVersion = avatarVersion,
    phone = phone
)
