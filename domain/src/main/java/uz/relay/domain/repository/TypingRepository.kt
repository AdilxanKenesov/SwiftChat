package uz.relay.domain.repository

import kotlinx.coroutines.flow.Flow

interface TypingRepository {
    /**
     * Hozir kim qaysi chatda yozyapti: `chatId → userId'lar`. Faqat xotirada yashaydi — protokol bo'yicha
     * typing hech qachon saqlanmaydi va `/v1/updates` da ham yo'q.
     */
    val typing: Flow<Map<String, Set<String>>>
}
