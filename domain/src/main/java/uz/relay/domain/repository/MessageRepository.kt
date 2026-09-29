package uz.relay.domain.repository

import kotlinx.coroutines.flow.Flow
import uz.relay.core.common.result.AppResult
import uz.relay.domain.model.Message

interface MessageRepository {

    /** Chat xabarlari lokal bazadan, eng yangisi birinchi (yuborilmaganlari eng oxirida). */
    fun observeMessages(chatId: String): Flow<List<Message>>

    /** Eng yangi sahifani serverdan yuklab, bazaga yozadi. Qiymat — eskiroq xabarlar ham bormi (`hasMore`). */
    suspend fun loadLatest(chatId: String): AppResult<Boolean>

    /** Bazadagi eng eski xabardan oldingi sahifani yuklaydi. Qiymat — `hasMore`. */
    suspend fun loadOlder(chatId: String): AppResult<Boolean>

    /**
     * Xabar darhol bazaga "yuborilmoqda" holatida yoziladi (UI uni shu zahoti ko'radi), keyin outbox
     * uni serverga yuboradi. Tarmoq bo'lmasa ham xabar yo'qolmaydi — internet qaytganda ketadi.
     */
    suspend fun sendText(chatId: String, text: String, replyToClientMessageId: String?)

    /** Xato bilan qolgan xabarni qaytadan outbox navbatiga qo'yadi. */
    suspend fun retry(clientMessageId: String)

    suspend fun edit(serverId: Long, text: String): AppResult<Unit>

    suspend fun delete(serverId: Long): AppResult<Unit>

    /** "Yozmoqda…" signali (faqat socket orqali, saqlanmaydi). */
    fun sendTyping(chatId: String)

    /** Chatni oxirigacha o'qildi deb belgilaydi: lokal belgi darhol, serverga `read` kvitansiyasi. */
    suspend fun markRead(chatId: String): AppResult<Unit>
}
