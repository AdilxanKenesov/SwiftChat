package uz.relay.feature.profile.me

import uz.relay.core.navigation.AppNavigationParam
import uz.relay.core.navigation.AppNavigator
import uz.relay.core.navigation.key.EditProfileKey
import javax.inject.Inject

/** [MyProfileContract.Directions] implementatsiyasi: Nav3 kalitlarini AppNavigator event bus'iga yuboradi. */
internal class MyProfileDirectionsImpl @Inject constructor(
    private val navigator: AppNavigator
) : MyProfileContract.Directions {

    override suspend fun back() = navigator.navigate(AppNavigationParam.Back)

    override suspend fun navigateToEditProfile() = navigator.navigate(AppNavigationParam.To(EditProfileKey))
}
