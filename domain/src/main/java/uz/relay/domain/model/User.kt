package uz.relay.domain.model

data class User(
    val id: String,
    val username: String?,
    val displayName: String,
    val avatarMediaId: String?,
    val avatarVersion: Int,
    val phone: String?
)
