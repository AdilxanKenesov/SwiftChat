package uz.relay.domain.model

/**
 * Profil maydonlari qoidalari — serverdagi UpdateMeRequest sxemasi bilan bir xil. Tekshiruv klientda ham
 * bo'lgani uchun noto'g'ri so'rov umuman yuborilmaydi va xato foydalanuvchiga darhol ko'rsatiladi.
 */
object ProfileRules {
    /** Lotin harflari, raqamlar va `_`, 3–32 belgi. */
    val USERNAME = Regex("^[a-zA-Z0-9_]{3,32}$")
    const val NAME_MAX = 128
    const val USERNAME_MAX = 32

    fun isNameValid(name: String) = name.trim().length in 1..NAME_MAX
    fun isUsernameValid(username: String) = USERNAME.matches(username)
}
