package uz.relay.feature.auth.phone

import uz.relay.core.navigation.AppNavigationParam
import uz.relay.core.navigation.AppNavigator
import uz.relay.core.navigation.key.OtpKey
import javax.inject.Inject

internal class PhoneDirectionsImpl @Inject constructor(
    private val navigator: AppNavigator
) : PhoneContract.Directions {

    override suspend fun navigateToOtp(phone: String) = navigator.navigate(AppNavigationParam.To(OtpKey(phone)))
}
