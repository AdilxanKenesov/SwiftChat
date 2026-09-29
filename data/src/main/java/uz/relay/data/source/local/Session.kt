package uz.relay.data.source.local

import kotlinx.serialization.Serializable

@Serializable
data class Session(
    val accessToken: String,
    val refreshToken: String,
    val userId: String,
    val deviceId: String
)
