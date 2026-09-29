package uz.relay.domain.usecase.chat

import kotlinx.coroutines.flow.Flow
import uz.relay.domain.model.SyncStatus
import uz.relay.domain.repository.ChatRepository
import javax.inject.Inject

/**
 * Sinxronlash holatini kuzatadi — skeleton yoki "chatlar yo'q" holatini to'g'ri ajratish uchun.
 *
 * Faqat repository'ga uzatadi — ViewModel repository'ni emas, aniq nomli amalni bilishi uchun (single responsibility, test'da oson almashtiriladi).
 */
class ObserveSyncStatusUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    operator fun invoke(): Flow<SyncStatus> = repository.observeSyncStatus()
}
