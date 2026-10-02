package uz.relay.feature.profile.edit

import uz.relay.core.navigation.AppNavigationParam
import uz.relay.core.navigation.AppNavigator
import javax.inject.Inject

/** [EditProfileContract.Directions] implementatsiyasi: orqaga qaytishni AppNavigator event bus'iga yuboradi. */
internal class EditProfileDirectionsImpl @Inject constructor(
    private val navigator: AppNavigator
) : EditProfileContract.Directions {

    override suspend fun back() = navigator.navigate(AppNavigationParam.Back)
}
