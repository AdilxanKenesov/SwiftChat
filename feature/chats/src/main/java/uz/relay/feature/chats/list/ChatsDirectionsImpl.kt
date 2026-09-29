package uz.relay.feature.chats.list

import uz.relay.core.navigation.AppNavigationParam
import uz.relay.core.navigation.AppNavigator
import uz.relay.core.navigation.key.ChatKey
import uz.relay.core.navigation.key.MyProfileKey
import uz.relay.core.navigation.key.SearchKey
import javax.inject.Inject

/**
 * [ChatsContract.Directions] ning haqiqiy amalga oshirilishi: [AppNavigator] (Navigation 3 event-bus)
 * orqali kalitni back stack'ga qo'shadi. ViewModel navigatsiya tafsilotlaridan ajratilgan — shu sababli
 * alohida klass va Hilt orqali bog'lanadi (ChatsDirectionsModule).
 */
internal class ChatsDirectionsImpl @Inject constructor(
    private val navigator: AppNavigator
) : ChatsContract.Directions {

    override suspend fun navigateToChat(chatId: String) = navigator.navigate(AppNavigationParam.To(ChatKey(chatId)))

    override suspend fun navigateToSearch() = navigator.navigate(AppNavigationParam.To(SearchKey))

    override suspend fun navigateToMyProfile() = navigator.navigate(AppNavigationParam.To(MyProfileKey))
}
