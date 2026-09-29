package uz.relay.domain.repository

import kotlinx.coroutines.flow.Flow
import uz.relay.core.common.result.AppResult
import uz.relay.domain.model.ChatSummary
import uz.relay.domain.model.SyncStatus

interface ChatRepository {

    /**
     * Chatlar lokal bazadan o'qiladi (offline ham ishlaydi). Tarmoq faqat bazani yangilaydi, UI esa
     * bazaning o'zgarishini shu Flow orqali oladi — "yagona haqiqat manbai" (single source of truth).
     */
    fun observeChats(): Flow<List<ChatSummary>>

    /** Bitta chat (suhbat ekrani sarlavhasi uchun). Bazada yo'q bo'lsa `null`. */
    fun observeChat(chatId: String): Flow<ChatSummary?>

    fun observeSyncStatus(): Flow<SyncStatus>

    /**
     * Server bilan sinxronlash: baza hali to'ldirilmagan bo'lsa — to'liq bootstrap, aks holda faqat
     * o'tkazib yuborilgan hodisalar (`GET /v1/updates`). Natija [observeChats] orqali keladi.
     */
    suspend fun refresh(): AppResult<Unit>

    /** Bu odam bilan DIRECT chat: bo'lsa o'sha, bo'lmasa yangisi (get-or-create). Qiymat — chat id'si. */
    suspend fun openDirect(peerUserId: String): AppResult<String>

    /** Faqat o'zim uchun ovozsiz qilish — boshqa a'zolar buni ko'rmaydi, push'lar ham to'xtaydi. */
    suspend fun setMuted(chatId: String, muted: Boolean): AppResult<Unit>
}
