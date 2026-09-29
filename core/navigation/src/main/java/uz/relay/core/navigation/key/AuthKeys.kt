package uz.relay.core.navigation.key

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

// Keys live here (not in features) so one feature can open another's screen without depending on it.

@Serializable
data object SplashKey : NavKey

@Serializable
data object PhoneKey : NavKey

/** [phone] in international format, e.g. +998901234567. */
@Serializable
data class OtpKey(val phone: String) : NavKey

@Serializable
data object ProfileSetupKey : NavKey
