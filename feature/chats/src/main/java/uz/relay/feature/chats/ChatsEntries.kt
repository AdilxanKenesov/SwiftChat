package uz.relay.feature.chats

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import uz.relay.core.navigation.key.ChatsKey

fun EntryProviderScope<NavKey>.chatsEntries() {
    entry<ChatsKey> { ChatsScreen() }
}
