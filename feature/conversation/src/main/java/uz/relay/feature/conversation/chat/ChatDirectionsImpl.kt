package uz.relay.feature.conversation.chat

import uz.relay.core.navigation.AppNavigationParam
import uz.relay.core.navigation.AppNavigator
import uz.relay.core.navigation.key.CallKey
import uz.relay.core.navigation.key.GroupInfoKey
import uz.relay.core.navigation.key.MediaViewerKey
import uz.relay.core.navigation.key.UserProfileKey
import javax.inject.Inject

/**
 * [ChatContract.Directions] ning amalga oshirilishi: har bir yo'nalishni Navigation 3 kalitiga aylantirib,
 * AppNavigator (event bus) ga yuboradi. Back stack'ni app darajasidagi NavDisplay o'zgartiradi — shuning uchun
 * ViewModel ham, bu klass ham back stack'ga to'g'ridan-to'g'ri tegmaydi. Hilt orqali ConversationDirectionsModule'da bog'lanadi.
 */
internal class ChatDirectionsImpl @Inject constructor(
    private val navigator: AppNavigator
) : ChatContract.Directions {

    override suspend fun back() = navigator.navigate(AppNavigationParam.Back)

    override suspend fun navigateToGroupInfo(chatId: String) = navigator.navigate(AppNavigationParam.To(GroupInfoKey(chatId)))

    override suspend fun navigateToUserProfile(userId: String) = navigator.navigate(AppNavigationParam.To(UserProfileKey(userId)))

    override suspend fun navigateToMediaViewer(chatId: String, clientMessageId: String) =
        navigator.navigate(AppNavigationParam.To(MediaViewerKey(chatId, clientMessageId)))

    override suspend fun navigateToCall(callId: String, video: Boolean, chatId: String) =
        navigator.navigate(AppNavigationParam.To(CallKey(callId, video, chatId)))
}
