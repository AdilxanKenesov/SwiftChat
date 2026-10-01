package uz.relay.data.model.request

import kotlinx.serialization.Serializable

/*
 * Autentifikatsiya so'rovlarining DTO'lari (tokensiz [uz.relay.data.source.network.api.AuthApi] orqali).
 * DTO'lar domain modellaridan alohida: server JSON shakli o'zgarsa, faqat data qatlami tegiladi.
 */

/** `POST /v1/auth/otp/request` — ko'rsatilgan raqamga tasdiqlash kodini yuborishni so'rash. */
@Serializable
data class OtpRequest(
    val phone: String
)

/** OTP kodini tekshirish; [deviceName] sessiyalar ro'yxatida qurilmani ko'rsatish uchun yuboriladi. */
@Serializable
data class VerifyOtpRequest(
    val phone: String,
    val code: String,
    val deviceName: String
)

/** Access token'ni yangilash. Refresh token bir martalik — javobda yangisi keladi (rotation). */
@Serializable
data class RefreshTokenRequest(
    val refreshToken: String
)
