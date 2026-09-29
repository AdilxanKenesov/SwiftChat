package uz.relay.domain.usecase.settings

import kotlinx.coroutines.flow.Flow
import uz.relay.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * Bildirishnomalar yoqilganmi — shu sozlamani kuzatadi.
 *
 * Faqat repository'ga uzatadi — ViewModel repository'ni emas, aniq nomli amalni bilishi uchun (single responsibility, test'da oson almashtiriladi).
 */
class ObserveNotificationsEnabledUseCase @Inject constructor(
    private val repository: SettingsRepository
) {
    operator fun invoke(): Flow<Boolean> = repository.notificationsEnabled
}
