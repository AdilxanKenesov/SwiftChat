package uz.relay.feature.group.create

import uz.relay.core.navigation.AppNavigationParam
import uz.relay.core.navigation.AppNavigator
import uz.relay.core.navigation.key.ChatKey
import uz.relay.core.navigation.key.ChatsKey
import javax.inject.Inject

/**
 * [GroupCreateContract.Directions] realizatsiyasi: [AppNavigator] event-bus'iga navigatsiya buyrug'ini yuboradi.
 * Nav3 back stack'ini `app` modulidagi navigator o'zgartiradi — bu klass faqat "nima qilish kerak"ligini aytadi.
 */
internal class GroupCreateDirectionsImpl @Inject constructor(
    private val navigator: AppNavigator
) : GroupCreateContract.Directions {

    override suspend fun back() = navigator.navigate(AppNavigationParam.Back)

    /** Stack: [Chatlar, Qidiruv, Guruh yaratish] → [Chatlar, Yangi guruh chati] — "orqaga" ro'yxatga qaytaradi. */
    override suspend fun openCreatedChat(chatId: String) {
        navigator.navigate(AppNavigationParam.BackTo(ChatsKey))
        navigator.navigate(AppNavigationParam.To(ChatKey(chatId)))
    }
}
