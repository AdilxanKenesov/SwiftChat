package uz.relay.feature.group.info

import uz.relay.core.navigation.AppNavigationParam
import uz.relay.core.navigation.AppNavigator
import uz.relay.core.navigation.key.ChatKey
import uz.relay.core.navigation.key.ChatSearchKey
import uz.relay.core.navigation.key.ChatsKey
import uz.relay.core.navigation.key.GroupCreateKey
import javax.inject.Inject

/**
 * [GroupInfoContract.Directions] realizatsiyasi: [AppNavigator] event-bus'i orqali Nav3 back stack'ini o'zgartiradi.
 * "A'zo qo'shish" uchun guruh yaratish ekrani qayta ishlatiladi ([GroupCreateKey] + `addToChatId`).
 */
internal class GroupInfoDirectionsImpl @Inject constructor(
    private val navigator: AppNavigator
) : GroupInfoContract.Directions {

    override suspend fun back() = navigator.navigate(AppNavigationParam.Back)

    override suspend fun navigateToAddMembers(chatId: String) =
        navigator.navigate(AppNavigationParam.To(GroupCreateKey(addToChatId = chatId)))

    override suspend fun navigateToChatSearch(chatId: String) = navigator.navigate(AppNavigationParam.To(ChatSearchKey(chatId)))

    override suspend fun navigateToChat(chatId: String) = navigator.navigate(AppNavigationParam.To(ChatKey(chatId)))

    override suspend fun backToChats() = navigator.navigate(AppNavigationParam.BackTo(ChatsKey))
}
