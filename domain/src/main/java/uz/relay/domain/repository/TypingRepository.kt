package uz.relay.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * "Yozmoqda…" holatlari manbai (socket'dan keladi, faqat xotirada).
 *
 * Nega interface: domain toza Kotlin moduli (Android'ga bog'liq emas) va faqat shartnomani belgilaydi,
 * amalga oshirish esa `data` modulida (Retrofit + Room). Shunda feature modullar data'ni bilmaydi,
 * use case'larni fake repository bilan oson test qilish mumkin (clean architecture, dependency inversion).
 */
interface TypingRepository {
    /**
     * Hozir kim qaysi chatda yozyapti: `chatId → userId'lar`. Faqat xotirada yashaydi — protokol bo'yicha
     * typing hech qachon saqlanmaydi va `/v1/updates` da ham yo'q.
     */
    val typing: Flow<Map<String, Set<String>>>
}
