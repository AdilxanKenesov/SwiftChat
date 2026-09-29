package uz.relay.feature.chats.list

import uz.relay.core.navigation.AppNavigationParam
import uz.relay.core.navigation.AppNavigator
import uz.relay.core.navigation.key.ChatKey
import uz.relay.core.navigation.key.SearchKey
import javax.inject.Inject

internal class ChatsDirectionsImpl @Inject constructor(
    private val navigator: AppNavigator
) : ChatsContract.Directions {

    override suspend fun navigateToChat(chatId: String) = navigator.navigate(AppNavigationParam.To(ChatKey(chatId)))

    override suspend fun navigateToSearch() = navigator.navigate(AppNavigationParam.To(SearchKey))
}
