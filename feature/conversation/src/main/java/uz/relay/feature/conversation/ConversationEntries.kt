package uz.relay.feature.conversation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import uz.relay.core.navigation.key.ChatKey
import uz.relay.core.navigation.key.ChatSearchKey
import uz.relay.core.navigation.key.MediaViewerKey
import uz.relay.feature.conversation.chat.ChatScreen
import uz.relay.feature.conversation.search.ChatSearchScreen
import uz.relay.feature.conversation.viewer.MediaViewerScreen

/**
 * Bu feature ekranlarini ilovaning NavDisplay'iga ro'yxatdan o'tkazadi (kalit → ekran).
 *
 * Navigation 3'da har feature o'z entry'larini o'zi e'lon qiladi: app moduli faqat shu funksiyani chaqiradi va
 * ekranlarning ichki tuzilishini bilmaydi. Kalitlar (ChatKey va h.k.) core:navigation'da turadi, shuning uchun
 * boshqa feature'lar bu modulga bog'lanmasdan turib chat/qidiruv/ko'ruvchini ocha oladi.
 */
fun EntryProviderScope<NavKey>.conversationEntries() {
    entry<ChatKey> { key -> ChatScreen(chatId = key.chatId, focusMessageId = key.focusMessageId) }
    entry<ChatSearchKey> { key -> ChatSearchScreen(chatId = key.chatId) }
    entry<MediaViewerKey> { key -> MediaViewerScreen(chatId = key.chatId, clientMessageId = key.clientMessageId) }
}
