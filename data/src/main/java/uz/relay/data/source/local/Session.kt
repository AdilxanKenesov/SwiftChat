package uz.relay.data.source.local

import kotlinx.serialization.Serializable

/**
 * Tizimga kirgan foydalanuvchi sessiyasi. [SessionStorage] uni JSON'ga aylantirib, shifrlab saqlaydi
 * (shuning uchun @Serializable). `deviceId` — server shu qurilmaga bergan id, refresh va push token
 * uchun kerak.
 */
@Serializable
data class Session(
    val accessToken: String,
    val refreshToken: String,
    val userId: String,
    val deviceId: String
)
