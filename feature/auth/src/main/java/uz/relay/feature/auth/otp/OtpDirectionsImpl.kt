package uz.relay.feature.auth.otp

import uz.relay.core.navigation.AppNavigationParam
import uz.relay.core.navigation.AppNavigator
import uz.relay.core.navigation.key.ChatsKey
import uz.relay.core.navigation.key.ProfileSetupKey
import javax.inject.Inject

/**
 * [OtpContract.Directions] ning amalga oshirilishi.
 *
 * Muvaffaqiyatli kirishdan keyin `ResetTo` ishlatiladi - auth ekranlari back stack'dan tozalanadi,
 * foydalanuvchi "orqaga" bosib OTP ekraniga qaytib qolmaydi.
 */
internal class OtpDirectionsImpl @Inject constructor(
    private val navigator: AppNavigator
) : OtpContract.Directions {

    override suspend fun back() = navigator.navigate(AppNavigationParam.Back)

    override suspend fun navigateToProfileSetup() = navigator.navigate(AppNavigationParam.ResetTo(ProfileSetupKey))

    override suspend fun navigateToChats() = navigator.navigate(AppNavigationParam.ResetTo(ChatsKey))
}
