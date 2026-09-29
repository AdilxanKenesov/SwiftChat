package uz.relay.feature.group

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import uz.relay.core.navigation.key.GroupCreateKey
import uz.relay.core.navigation.key.GroupInfoKey
import uz.relay.feature.group.create.GroupCreateScreen
import uz.relay.feature.group.info.GroupInfoScreen

/**
 * Bu feature ekranlarini ilovaning NavDisplay'iga ro'yxatdan o'tkazadi (kalit → ekran).
 *
 * Navigation 3'da har bir ekran `NavKey` orqali topiladi: `app` moduli barcha feature'larning `xxxEntries()`
 * funksiyalarini bitta `entryProvider` ichida chaqiradi. Shu sababli feature moduli boshqa feature'larni bilmaydi —
 * faqat `core:navigation` dagi kalitlarni biladi. Kalit ichidagi argument (`addToChatId`, `chatId`) to'g'ridan-to'g'ri
 * ekranga, u yerdan esa AssistedInject orqali ViewModel'ga uzatiladi.
 */
fun EntryProviderScope<NavKey>.groupEntries() {
    entry<GroupCreateKey> { key -> GroupCreateScreen(addToChatId = key.addToChatId) }
    entry<GroupInfoKey> { key -> GroupInfoScreen(chatId = key.chatId) }
}
