package uz.relay.domain.repository

import kotlinx.coroutines.flow.Flow
import uz.relay.core.common.result.AppResult
import uz.relay.domain.model.Attachment
import uz.relay.domain.model.Message

/**
 * Xabarlar: o'qish, yuborish (outbox orqali), tahrir, o'chirish, kvitansiyalar va qidiruv.
 *
 * Nega interface: domain toza Kotlin moduli (Android'ga bog'liq emas) va faqat shartnomani belgilaydi,
 * amalga oshirish esa `data` modulida (Retrofit + Room). Shunda feature modullar data'ni bilmaydi,
 * use case'larni fake repository bilan oson test qilish mumkin (clean architecture, dependency inversion).
 */
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

    /**
     * Rasm/video/fayl yuborish. Fayl avval ilova papkasiga nusxalanadi (galereya ruxsati vaqtinchalik,
     * ilova qayta ochilganda ham yuklashni davom ettirish kerak), keyin xabar "yuborilmoqda" holatida
     * bazaga yoziladi va outbox uni bo'laklab yuklaydi.
     *
     * Xato faqat tayyorlash bosqichida qaytadi (fayl o'qilmadi, 100 MB dan katta).
     * FILE uchun [caption] o'rniga fayl nomi yuboriladi — server fayl nomini saqlamaydi.
     */
    suspend fun sendMedia(chatId: String, attachment: Attachment, caption: String?, replyToClientMessageId: String?): AppResult<Unit>

    /** Yuklanayotgan xabarni bekor qilish: xabar va uning lokal fayli o'chiriladi. */
    suspend fun cancelUpload(clientMessageId: String)

    /** Xato bilan qolgan xabarni qaytadan outbox navbatiga qo'yadi. */
    suspend fun retry(clientMessageId: String)

    /** Xabar matnini tahrirlash (faqat serverga yetib borgan xabar — shuning uchun [serverId]). */
    suspend fun edit(serverId: Long, text: String): AppResult<Unit>

    /** Xabarni o'chirish: serverda va bazada tombstone bo'lib qoladi. */
    suspend fun delete(serverId: Long): AppResult<Unit>

    /** "Yozmoqda…" signali (faqat socket orqali, saqlanmaydi). */
    fun sendTyping(chatId: String)

    /** Chatni oxirigacha o'qildi deb belgilaydi: lokal belgi darhol, serverga `read` kvitansiyasi. */
    suspend fun markRead(chatId: String): AppResult<Unit>

    /** Chat ichida lokal qidiruv — faqat qurilmadagi (yuklangan) xabarlar ichidan. */
    suspend fun search(chatId: String, query: String): List<Message>
}
