package uz.relay.feature.group

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import uz.relay.core.navigation.key.GroupCreateKey
import uz.relay.core.navigation.key.GroupInfoKey
import uz.relay.feature.group.create.GroupCreateScreen
import uz.relay.feature.group.info.GroupInfoScreen

/** Bu feature ekranlarini ilovaning NavDisplay'iga ro'yxatdan o'tkazadi (kalit → ekran). */
fun EntryProviderScope<NavKey>.groupEntries() {
    entry<GroupCreateKey> { key -> GroupCreateScreen(addToChatId = key.addToChatId) }
    entry<GroupInfoKey> { key -> GroupInfoScreen(chatId = key.chatId) }
}
