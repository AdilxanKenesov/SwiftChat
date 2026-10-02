package uz.relay.domain.repository

import kotlinx.coroutines.flow.Flow
import uz.relay.core.common.result.AppResult
import uz.relay.domain.model.ChatSummary
import uz.relay.domain.model.SyncStatus

/**
 * Chatlar ro'yxati, sinxronlash va chat sozlamalari (mute).
 *
 * Nega interface: domain toza Kotlin moduli (Android'ga bog'liq emas) va faqat shartnomani belgilaydi,
 * amalga oshirish esa `data` modulida (Retrofit + Room). Shunda feature modullar data'ni bilmaydi,
 * use case'larni fake repository bilan oson test qilish mumkin (clean architecture, dependency inversion).
 */
interface ChatRepository {

    /**
     * Chatlar lokal bazadan o'qiladi (offline ham ishlaydi). Tarmoq faqat bazani yangilaydi, UI esa
     * bazaning o'zgarishini shu Flow orqali oladi — "yagona haqiqat manbai" (single source of truth).
     */
    fun observeChats(): Flow<List<ChatSummary>>

    /** Bitta chat (suhbat ekrani sarlavhasi uchun). Bazada yo'q bo'lsa `null`. */
    fun observeChat(chatId: String): Flow<ChatSummary?>

    /** Shu odam bilan shaxsiy chat (bo'lmasa `null`) — foydalanuvchi profilidagi "Ovozsiz qilish" uchun. */
    fun observeDirectChat(peerUserId: String): Flow<ChatSummary?>

    /** Bootstrap/catch-up holati — skeleton va "Yangilanmoqda…" uchun. */
    fun observeSyncStatus(): Flow<SyncStatus>

    /**
     * Server bilan sinxronlash: baza hali to'ldirilmagan bo'lsa — to'liq bootstrap, aks holda faqat
     * o'tkazib yuborilgan hodisalar (`GET /v1/updates`). Natija [observeChats] orqali keladi.
     */
    suspend fun refresh(): AppResult<Unit>

    /** Bu odam bilan DIRECT chat: bo'lsa o'sha, bo'lmasa yangisi (get-or-create). Qiymat — chat id'si. */
    suspend fun openDirect(peerUserId: String): AppResult<String>

    /**
     * Faqat o'zim uchun ovozsiz qilish — boshqa a'zolar buni ko'rmaydi, push'lar ham to'xtaydi.
     * [mutedUntil] `null` — muddatsiz (yoki [muted] `false` bo'lsa ahamiyatsiz).
     */
    suspend fun setMuted(chatId: String, muted: Boolean, mutedUntil: Long? = null): AppResult<Unit>
}
