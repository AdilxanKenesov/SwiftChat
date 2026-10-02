package uz.relay.feature.conversation.search

import uz.relay.core.navigation.AppNavigationParam
import uz.relay.core.navigation.AppNavigator
import uz.relay.core.navigation.key.ChatKey
import uz.relay.core.navigation.key.ChatsKey
import javax.inject.Inject

/** [ChatSearchContract.Directions] implementatsiyasi: Nav3 kalitlarini AppNavigator event bus'iga yuboradi. */
internal class ChatSearchDirectionsImpl @Inject constructor(
    private val navigator: AppNavigator
) : ChatSearchContract.Directions {

    override suspend fun back() = navigator.navigate(AppNavigationParam.Back)

    /**
     * Stack: [Chatlar, Chat, Guruh ma'lumoti, Qidiruv] → [Chatlar, Chat(xabarga scroll)]. Chat qaytadan ochiladi,
     * chunki kalitda scroll qilinadigan xabar bor; "orqaga" esa chatlar ro'yxatiga olib boradi.
     */
    override suspend fun openMessage(chatId: String, clientMessageId: String) {
        navigator.navigate(AppNavigationParam.BackTo(ChatsKey))
        navigator.navigate(AppNavigationParam.To(ChatKey(chatId, focusMessageId = clientMessageId)))
    }
}
