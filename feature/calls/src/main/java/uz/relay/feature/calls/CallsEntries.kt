package uz.relay.feature.calls

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import uz.relay.core.navigation.key.CallKey
import uz.relay.feature.calls.call.CallScreen

/** Calls feature ekranlarini ilovaning NavDisplay'iga ro'yxatdan o'tkazadi (NavKey → ekran). */
fun EntryProviderScope<NavKey>.callsEntries() {
    entry<CallKey> { key -> CallScreen(callId = key.callId, video = key.video, chatId = key.chatId, group = key.group) }
}
