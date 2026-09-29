package uz.relay.feature.conversation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import uz.relay.core.navigation.key.ChatKey
import uz.relay.core.navigation.key.ChatSearchKey
import uz.relay.core.navigation.key.MediaViewerKey
import uz.relay.feature.conversation.chat.ChatScreen
import uz.relay.feature.conversation.search.ChatSearchScreen
import uz.relay.feature.conversation.viewer.MediaViewerScreen

/** Bu feature ekranlarini ilovaning NavDisplay'iga ro'yxatdan o'tkazadi (kalit → ekran). */
fun EntryProviderScope<NavKey>.conversationEntries() {
    entry<ChatKey> { key -> ChatScreen(chatId = key.chatId, focusMessageId = key.focusMessageId) }
    entry<ChatSearchKey> { key -> ChatSearchScreen(chatId = key.chatId) }
    entry<MediaViewerKey> { key -> MediaViewerScreen(chatId = key.chatId, clientMessageId = key.clientMessageId) }
}
