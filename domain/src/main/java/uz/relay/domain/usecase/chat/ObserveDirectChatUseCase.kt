package uz.relay.domain.usecase.chat

import kotlinx.coroutines.flow.Flow
import uz.relay.domain.model.ChatSummary
import uz.relay.domain.repository.ChatRepository
import javax.inject.Inject

/**
 * Shu odam bilan shaxsiy chatni kuzatadi (bo'lmasa `null`) — profil ekranidagi "Ovozsiz qilish" uchun.
 *
 * Faqat repository'ga uzatadi — ViewModel repository'ni emas, aniq nomli amalni bilishi uchun (single responsibility, test'da oson almashtiriladi).
 */
class ObserveDirectChatUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    operator fun invoke(peerUserId: String): Flow<ChatSummary?> = repository.observeDirectChat(peerUserId)
}
