package uz.relay.data.model.response

import kotlinx.serialization.Serializable

/**
 * Login/refresh javobi. Ikkala token ham shifrlangan holda [uz.relay.data.source.local.SessionStorage] ga
 * saqlanadi. [isNewUser] — yangi foydalanuvchi bo'lsa UI profilni to'ldirish ekraniga o'tadi.
 */
@Serializable
data class TokenPairResponse(
    val accessToken: String,
    val refreshToken: String,
    val userId: String,
    val deviceId: String,
    val isNewUser: Boolean
)
