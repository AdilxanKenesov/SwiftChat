package uz.relay.feature.conversation.chat

import uz.relay.core.navigation.AppNavigationParam
import uz.relay.core.navigation.AppNavigator
import uz.relay.core.navigation.key.GroupInfoKey
import uz.relay.core.navigation.key.MediaViewerKey
import uz.relay.core.navigation.key.UserProfileKey
import javax.inject.Inject

internal class ChatDirectionsImpl @Inject constructor(
    private val navigator: AppNavigator
) : ChatContract.Directions {

    override suspend fun back() = navigator.navigate(AppNavigationParam.Back)

    override suspend fun navigateToGroupInfo(chatId: String) = navigator.navigate(AppNavigationParam.To(GroupInfoKey(chatId)))

    override suspend fun navigateToUserProfile(userId: String) = navigator.navigate(AppNavigationParam.To(UserProfileKey(userId)))

    override suspend fun navigateToMediaViewer(chatId: String, clientMessageId: String) =
        navigator.navigate(AppNavigationParam.To(MediaViewerKey(chatId, clientMessageId)))
}
