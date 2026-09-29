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

/** Registers this feature's screens in the app's NavDisplay (key → screen). */
fun EntryProviderScope<NavKey>.authEntries() {
    entry<SplashKey> { SplashScreen() }
    entry<PhoneKey> { PhoneScreen() }
    entry<OtpKey> { key -> OtpScreen(phone = key.phone) }
    entry<ProfileSetupKey> { ProfileSetupScreen() }
}
