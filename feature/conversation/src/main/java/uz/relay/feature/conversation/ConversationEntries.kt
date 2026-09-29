package uz.relay.feature.conversation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import uz.relay.core.navigation.key.ChatKey
import uz.relay.feature.conversation.chat.ChatScreen

/** Bu feature ekranlarini ilovaning NavDisplay'iga ro'yxatdan o'tkazadi (kalit → ekran). */
fun EntryProviderScope<NavKey>.conversationEntries() {
    entry<ChatKey> { key -> ChatScreen(chatId = key.chatId) }
}
