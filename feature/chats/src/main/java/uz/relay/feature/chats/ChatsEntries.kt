package uz.relay.feature.chats

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import uz.relay.core.navigation.key.ChatsKey
import uz.relay.core.navigation.key.SearchKey
import uz.relay.feature.chats.list.ChatsScreen
import uz.relay.feature.chats.search.SearchScreen

/** Bu feature ekranlarini ilovaning NavDisplay'iga ro'yxatdan o'tkazadi (kalit → ekran). */
fun EntryProviderScope<NavKey>.chatsEntries() {
    entry<ChatsKey> { ChatsScreen() }
    entry<SearchKey> { SearchScreen() }
}
