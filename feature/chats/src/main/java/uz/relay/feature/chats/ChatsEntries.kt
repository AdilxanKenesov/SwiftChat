package uz.relay.feature.chats

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import uz.relay.core.navigation.key.ChatsKey
import uz.relay.core.navigation.key.SearchKey
import uz.relay.feature.chats.list.ChatsScreen
import uz.relay.feature.chats.search.SearchScreen

/**
 * Chats feature ekranlarini ilovaning NavDisplay'iga ro'yxatdan o'tkazadi (NavKey → ekran).
 * Har feature o'z entry'larini o'zi e'lon qiladi — app moduli ekranlarning ichki tuzilishini bilmaydi,
 * faqat `chatsEntries()` ni chaqiradi.
 */
fun EntryProviderScope<NavKey>.chatsEntries() {
    entry<ChatsKey> { ChatsScreen() }
    entry<SearchKey> { SearchScreen() }
}
