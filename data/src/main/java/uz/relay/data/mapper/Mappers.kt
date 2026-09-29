package uz.relay.data.mapper

import uz.relay.data.model.response.TokenPairResponse
import uz.relay.data.source.local.Session

fun TokenPairResponse.toSession() = Session(
    accessToken = accessToken,
    refreshToken = refreshToken,
    userId = userId,
    deviceId = deviceId
)
