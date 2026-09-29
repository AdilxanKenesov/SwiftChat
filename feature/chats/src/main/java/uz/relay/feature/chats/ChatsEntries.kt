package uz.relay.feature.chats

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import uz.relay.core.navigation.key.ChatsKey
import uz.relay.feature.chats.list.ChatsScreen

/** Bu feature ekranlarini ilovaning NavDisplay'iga ro'yxatdan o'tkazadi (kalit → ekran). */
fun EntryProviderScope<NavKey>.chatsEntries() {
    entry<ChatsKey> { ChatsScreen() }
}
