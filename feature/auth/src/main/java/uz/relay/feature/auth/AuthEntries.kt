package uz.relay.feature.auth

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import uz.relay.core.navigation.key.OtpKey
import uz.relay.core.navigation.key.PhoneKey
import uz.relay.core.navigation.key.ProfileSetupKey
import uz.relay.core.navigation.key.SplashKey
import uz.relay.feature.auth.otp.OtpScreen
import uz.relay.feature.auth.phone.PhoneScreen
import uz.relay.feature.auth.profile.ProfileSetupScreen
import uz.relay.feature.auth.splash.SplashScreen

/**
 * Auth feature ekranlarini ilovaning NavDisplay'iga ro'yxatdan o'tkazadi (key -> ekran).
 *
 * Navigation 3 da har bir ekran [NavKey] orqali aniqlanadi; app moduli faqat shu
 * extension'ni chaqiradi va auth ichidagi ekranlarni (ular `internal`) bilishi shart emas.
 * Oqim: Splash -> Phone -> Otp -> (kerak bo'lsa) ProfileSetup -> Chats.
 */
fun EntryProviderScope<NavKey>.authEntries() {
    entry<SplashKey> { SplashScreen() }
    entry<PhoneKey> { PhoneScreen() }
    // Telefon raqami key ichida keladi, OTP ekrani uni ViewModel'ga runtime argument sifatida beradi.
    entry<OtpKey> { key -> OtpScreen(phone = key.phone) }
    entry<ProfileSetupKey> { ProfileSetupScreen() }
}
