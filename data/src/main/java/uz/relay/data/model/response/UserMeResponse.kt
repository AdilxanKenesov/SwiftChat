package uz.relay.data.model.response

import kotlinx.serialization.Serializable

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
