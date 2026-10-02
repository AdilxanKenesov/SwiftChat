package uz.relay.domain.model

/**
 * Avtorizatsiya holati — MainViewModel shunga qarab ilovani qaysi ekrandan boshlashni tanlaydi
 * (telefon kiritish, profilni to'ldirish yoki chatlar ro'yxati).
 */
enum class AuthState {
    LOGGED_OUT,

    /** Tizimga kirgan, lekin yangi foydalanuvchi profilini (ism, username) hali to'ldirmagan. */
    NEEDS_PROFILE,
    LOGGED_IN
}
