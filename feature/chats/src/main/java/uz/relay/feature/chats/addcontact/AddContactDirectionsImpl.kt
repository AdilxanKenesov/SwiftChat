package uz.relay.feature.chats.addcontact

import uz.relay.core.navigation.AppNavigationParam
import uz.relay.core.navigation.AppNavigator
import uz.relay.core.navigation.key.UserProfileKey
import javax.inject.Inject

internal class AddContactDirectionsImpl @Inject constructor(
    private val navigator: AppNavigator
) : AddContactContract.Directions {

    override suspend fun back() = navigator.navigate(AppNavigationParam.Back)

    override suspend fun navigateToUserProfile(userId: String) = navigator.navigate(AppNavigationParam.To(UserProfileKey(userId)))
}
