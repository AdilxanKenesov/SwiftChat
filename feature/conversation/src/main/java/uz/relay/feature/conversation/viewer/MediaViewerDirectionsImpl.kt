package uz.relay.feature.conversation.viewer

import uz.relay.core.navigation.AppNavigationParam
import uz.relay.core.navigation.AppNavigator
import javax.inject.Inject

/** [MediaViewerContract.Directions] implementatsiyasi: orqaga qaytishni AppNavigator event bus'iga yuboradi. */
internal class MediaViewerDirectionsImpl @Inject constructor(
    private val navigator: AppNavigator
) : MediaViewerContract.Directions {

    override suspend fun back() = navigator.navigate(AppNavigationParam.Back)
}
