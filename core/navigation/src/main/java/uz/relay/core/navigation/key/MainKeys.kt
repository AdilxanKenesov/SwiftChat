package uz.relay.core.navigation.key

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object ChatsKey : NavKey

/** Suhbat ekrani. Hamma ma'lumot bazadan `chatId` bo'yicha o'qiladi — kalitda faqat id. */
@Serializable
data class ChatKey(val chatId: String) : NavKey
