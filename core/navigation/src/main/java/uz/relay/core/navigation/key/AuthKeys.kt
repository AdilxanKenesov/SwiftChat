package uz.relay.core.navigation.key

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

// Kalitlar feature'larda emas, shu yerda: bir feature boshqasining ekranini unga bog'lanmasdan ochishi uchun.
// @Serializable — Navigation 3 back stack'ni saqlaydi, jarayon o'ldirilsa (process death) ham stek tiklanadi.
// Kalitda faqat id/oddiy qiymatlar: katta obyektlar emas, ma'lumot ekranda bazadan o'qiladi.

/** Splash: auth holatiga qarab boshlang'ich ekranni tanlaydi. */
@Serializable
data object SplashKey : NavKey

/** Telefon raqamini kiritish (login'ning birinchi qadami; logout'dan keyin ham shu yerga qaytiladi). */
@Serializable
data object PhoneKey : NavKey

/** OTP kodni kiritish. [phone] xalqaro formatda, masalan +998901234567. */
@Serializable
data class OtpKey(val phone: String) : NavKey

/** Yangi foydalanuvchi profilini to'ldirish (ism, username). */
@Serializable
data object ProfileSetupKey : NavKey
