package uz.relay.feature.chats.newmessage

import uz.relay.core.navigation.AppNavigationParam
import uz.relay.core.navigation.AppNavigator
import uz.relay.core.navigation.key.AddContactKey
import uz.relay.core.navigation.key.ChatKey
import uz.relay.core.navigation.key.GroupCreateKey
import javax.inject.Inject

internal class NewMessageDirectionsImpl @Inject constructor(
    private val navigator: AppNavigator
) : NewMessageContract.Directions {

    override suspend fun back() = navigator.navigate(AppNavigationParam.Back)

    override suspend fun navigateToGroupCreate() = navigator.navigate(AppNavigationParam.To(GroupCreateKey()))

    override suspend fun navigateToAddContact() = navigator.navigate(AppNavigationParam.To(AddContactKey))

    /** Replace: chatdan "orqaga" bosilganda chatlar ro'yxatiga qaytiladi, bu ekranga emas (Telegram kabi). */
    override suspend fun navigateToChat(chatId: String) = navigator.navigate(AppNavigationParam.Replace(ChatKey(chatId)))
}
