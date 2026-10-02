package uz.relay.domain.usecase.chat

import uz.relay.core.common.result.AppResult
import uz.relay.domain.repository.ChatRepository
import javax.inject.Inject

/**
 * Shu odam bilan DIRECT chatni ochadi: bor bo'lsa o'sha, yo'q bo'lsa yangisi (get-or-create).
 *
 * Faqat repository'ga uzatadi — ViewModel repository'ni emas, aniq nomli amalni bilishi uchun (single responsibility, test'da oson almashtiriladi).
 */
class OpenDirectChatUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    /** Qiymat — chat id'si. */
    suspend operator fun invoke(peerUserId: String): AppResult<String> = repository.openDirect(peerUserId)
}
