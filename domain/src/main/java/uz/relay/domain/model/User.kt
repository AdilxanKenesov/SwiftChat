package uz.relay.domain.model

/**
 * Foydalanuvchi profili (o'zim yoki boshqa odam). Lokal keshdan o'qiladi; online holati socket'dagi
 * `presence` hodisalari bilan yangilanadi.
 */
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
