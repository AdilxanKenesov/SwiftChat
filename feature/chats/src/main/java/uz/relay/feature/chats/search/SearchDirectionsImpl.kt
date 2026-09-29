package uz.relay.feature.chats.search

import uz.relay.core.navigation.AppNavigationParam
import uz.relay.core.navigation.AppNavigator
import uz.relay.core.navigation.key.ChatKey
import uz.relay.core.navigation.key.GroupCreateKey
import javax.inject.Inject

/**
 * [SearchContract.Directions] ning [AppNavigator] (Navigation 3 event-bus) orqali amalga oshirilishi.
 * Alohida klass — ViewModel navigatsiya tafsilotlarini bilmasin va testda almashtirish oson bo'lsin.
 */
internal class SearchDirectionsImpl @Inject constructor(
    private val navigator: AppNavigator
) : SearchContract.Directions {

    override suspend fun back() = navigator.navigate(AppNavigationParam.Back)

    /** Qidiruv o'rniga chat ochiladi: chatdan "orqaga" bosilsa qidiruvga emas, ro'yxatga qaytiladi. */
    override suspend fun navigateToChat(chatId: String) = navigator.navigate(AppNavigationParam.Replace(ChatKey(chatId)))

    /** Guruh yaratish ekrani ustiga qo'yiladi — orqaga bosilsa qidiruvga qaytiladi. */
    override suspend fun navigateToGroupCreate() = navigator.navigate(AppNavigationParam.To(GroupCreateKey()))
}
