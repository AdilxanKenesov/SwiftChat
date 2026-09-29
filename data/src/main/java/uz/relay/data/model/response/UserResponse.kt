package uz.relay.data.model.response

import kotlinx.serialization.Serializable

/** Boshqa foydalanuvchining ochiq profili (`GET /v1/users/{id}`). Telefon raqami bu yerda yo'q. */
@Serializable
data class UserResponse(
    val id: String,
    val username: String? = null,
    val displayName: String,
    val avatarMediaId: String? = null,
    val avatarVersion: Int = 0,
    /** Hozir kamida bitta jonli WebSocket ulanishi bormi. */
    val online: Boolean = false,
    /** Online paytda yoki hech ko'rilmagan bo'lsa `null`. */
    val lastSeenAt: Long? = null
)
