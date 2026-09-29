package uz.relay.data.realtime

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uz.relay.core.common.dispatcher.ApplicationScope
import uz.relay.domain.repository.TypingRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Kim qaysi chatda yozayotgani: `chatId → userId'lar`.
 *
 * Nega xotirada (bazada emas): typing hech qachon saqlanmaydi va `/v1/updates` da yo'q (protokol) —
 * u faqat "hozir" uchun. Server bitta foydalanuvchidan 3 s da ko'pi bilan bitta frame uzatadi, shuning
 * uchun 5 s ichida yangisi kelmasa, odam yozishni to'xtatgan deb hisoblaymiz (spec: "expire after ~5s").
 *
 * Domain'dagi [TypingRepository] interfeysini ham shu klass bajaradi — UI "yozmoqda..." belgisini
 * [typing] StateFlow'dan o'qiydi. Ma'lumotni [RealtimeCoordinator] (`typing` frame) va
 * UpdateApplier (xabar kelganda [clear]) yangilaydi.
 */
@Singleton
class TypingTracker @Inject constructor(
    @ApplicationScope private val scope: CoroutineScope
) : TypingRepository {

    private val _typing = MutableStateFlow<Map<String, Set<String>>>(emptyMap())
    override val typing: StateFlow<Map<String, Set<String>>> = _typing.asStateFlow()

    // (chatId, userId) → muddat tugashini kutayotgan job. Bir nechta coroutine'dan o'zgargani uchun
    // `synchronized` bilan himoyalangan.
    private val expiryJobs = HashMap<Pair<String, String>, Job>()

    /** `typing` frame keldi: foydalanuvchini ro'yxatga qo'shadi va 5 s lik taymerni qayta boshlaydi. */
    fun onTyping(chatId: String, userId: String) {
        _typing.update { current -> current + (chatId to (current[chatId].orEmpty() + userId)) }
        val key = chatId to userId
        synchronized(expiryJobs) {
            // Har yangi frame muddatni qaytadan 5 s ga uzaytiradi.
            expiryJobs.remove(key)?.cancel()
            expiryJobs[key] = scope.launch {
                delay(TYPING_TTL_MS)
                clear(chatId, userId)
            }
        }
    }

    /** Xabari kelgan odam endi "yozmayapti" — 5 s kutmasdan darhol olib tashlaymiz (Telegram'dagidek). */
    fun clear(chatId: String, userId: String) {
        synchronized(expiryJobs) { expiryJobs.remove(chatId to userId)?.cancel() }
        _typing.update { current ->
            val remaining = current[chatId].orEmpty() - userId
            if (remaining.isEmpty()) current - chatId else current + (chatId to remaining)
        }
    }

    private companion object {
        const val TYPING_TTL_MS = 5_000L
    }
}
