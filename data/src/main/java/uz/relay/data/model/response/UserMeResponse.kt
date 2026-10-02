package uz.relay.data.model.response

import kotlinx.serialization.Serializable

/**
 * `GET /v1/users/me` — o'zimning profilim. [UserResponse] dan farqi: telefon raqami ham keladi
 * (u faqat egasiga ko'rsatiladi). [avatarVersion] avatar keshini yangilash uchun ishlatiladi.
 */
@Serializable
data class UserMeResponse(
    val id: String,
    val username: String? = null,
    val displayName: String,
    val avatarMediaId: String? = null,
    val avatarVersion: Int = 0,
    val online: Boolean = false,
    val lastSeenAt: Long? = null,
    val phone: String? = null
)
