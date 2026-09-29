package uz.relay.feature.auth.profile

import uz.relay.core.navigation.AppNavigationParam
import uz.relay.core.navigation.AppNavigator
import uz.relay.core.navigation.key.ChatsKey
import javax.inject.Inject

internal class ProfileSetupDirectionsImpl @Inject constructor(
    private val navigator: AppNavigator
) : ProfileSetupContract.Directions {

    override suspend fun navigateToChats() = navigator.navigate(AppNavigationParam.ResetTo(ChatsKey))
}
