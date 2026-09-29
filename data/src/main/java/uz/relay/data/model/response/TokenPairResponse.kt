package uz.relay.data.model.response

import kotlinx.serialization.Serializable

@Serializable
data class TokenPairResponse(
    val accessToken: String,
    val refreshToken: String,
    val userId: String,
    val deviceId: String,
    val isNewUser: Boolean
)
