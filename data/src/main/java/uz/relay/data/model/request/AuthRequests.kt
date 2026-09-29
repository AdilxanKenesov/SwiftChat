package uz.relay.data.model.request

import kotlinx.serialization.Serializable

@Serializable
data class OtpRequest(
    val phone: String
)

@Serializable
data class VerifyOtpRequest(
    val phone: String,
    val code: String,
    val deviceName: String
)

@Serializable
data class RefreshTokenRequest(
    val refreshToken: String
)
