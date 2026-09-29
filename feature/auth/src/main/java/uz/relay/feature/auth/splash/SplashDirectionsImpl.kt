package uz.relay.feature.auth.splash

import uz.relay.core.navigation.AppNavigationParam
import uz.relay.core.navigation.AppNavigator
import uz.relay.core.navigation.key.ChatsKey
import uz.relay.core.navigation.key.PhoneKey
import uz.relay.core.navigation.key.ProfileSetupKey
import javax.inject.Inject

/**
 * [SplashContract.Directions] ning [AppNavigator] (event-bus navigator) orqali amalga oshirilishi.
 *
 * Hamma o'tishlar `ResetTo` - splash back stack'da qolmasligi kerak, aks holda "orqaga"
 * bosilganda foydalanuvchi yana splash'ga qaytib qolardi.
 */
internal class SplashDirectionsImpl @Inject constructor(
    private val navigator: AppNavigator
) : SplashContract.Directions {

    override suspend fun navigateToPhone() = navigator.navigate(AppNavigationParam.ResetTo(PhoneKey))

    override suspend fun navigateToProfileSetup() = navigator.navigate(AppNavigationParam.ResetTo(ProfileSetupKey))

    override suspend fun navigateToChats() = navigator.navigate(AppNavigationParam.ResetTo(ChatsKey))
}
