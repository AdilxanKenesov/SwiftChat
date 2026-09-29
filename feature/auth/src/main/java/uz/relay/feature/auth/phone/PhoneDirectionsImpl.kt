package uz.relay.feature.auth.phone

import uz.relay.core.navigation.AppNavigationParam
import uz.relay.core.navigation.AppNavigator
import uz.relay.core.navigation.key.OtpKey
import javax.inject.Inject

/**
 * [PhoneContract.Directions] ning amalga oshirilishi.
 *
 * `To` (push) ishlatiladi, chunki OTP ekranidan "orqaga" bosib raqamni tuzatish mumkin bo'lishi kerak.
 */
internal class PhoneDirectionsImpl @Inject constructor(
    private val navigator: AppNavigator
) : PhoneContract.Directions {

    override suspend fun navigateToOtp(phone: String) = navigator.navigate(AppNavigationParam.To(OtpKey(phone)))
}
