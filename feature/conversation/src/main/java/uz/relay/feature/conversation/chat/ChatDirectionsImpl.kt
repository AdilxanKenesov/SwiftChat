package uz.relay.feature.conversation.chat

import uz.relay.core.navigation.AppNavigationParam
import uz.relay.core.navigation.AppNavigator
import uz.relay.core.navigation.key.GroupInfoKey
import javax.inject.Inject

internal class ChatDirectionsImpl @Inject constructor(
    private val navigator: AppNavigator
) : ChatContract.Directions {

    override suspend fun back() = navigator.navigate(AppNavigationParam.Back)

    override suspend fun navigateToGroupInfo(chatId: String) = navigator.navigate(AppNavigationParam.To(GroupInfoKey(chatId)))
}
