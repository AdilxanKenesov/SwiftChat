package uz.relay.domain.usecase.chat

import kotlinx.coroutines.flow.Flow
import uz.relay.domain.model.ConnectionStatus
import uz.relay.domain.repository.ConnectionRepository
import javax.inject.Inject

/**
 * Umumiy ulanish holatini ([ConnectionStatus]) kuzatadi — "Ulanmoqda…" kabi sarlavha/banner uchun.
 *
 * Faqat repository'ga uzatadi — ViewModel repository'ni emas, aniq nomli amalni bilishi uchun (single responsibility, test'da oson almashtiriladi).
 */
class ObserveConnectionStatusUseCase @Inject constructor(
    private val repository: ConnectionRepository
) {
    operator fun invoke(): Flow<ConnectionStatus> = repository.status
}
