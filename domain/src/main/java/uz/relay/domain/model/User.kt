package uz.relay.domain.model

data class User(
    val id: String,
    val username: String?,
    val displayName: String,
    val avatarMediaId: String?,
    val avatarVersion: Int,
    val phone: String?,
    /** Hozir kamida bitta jonli ulanishi bormi. */
    val online: Boolean = false,
    /** Oxirgi marta qachon online bo'lgan (online paytda yoki noma'lum bo'lsa `null`). */
    val lastSeenAt: Long? = null
)
