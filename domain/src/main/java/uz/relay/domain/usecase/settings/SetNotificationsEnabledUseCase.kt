package uz.relay.domain.usecase.settings

import uz.relay.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * Bildirishnomalarni yoqadi yoki o'chiradi.
 *
 * Faqat repository'ga uzatadi — ViewModel repository'ni emas, aniq nomli amalni bilishi uchun (single responsibility, test'da oson almashtiriladi).
 */
class SetNotificationsEnabledUseCase @Inject constructor(
    private val repository: SettingsRepository
) {
    suspend operator fun invoke(enabled: Boolean) = repository.setNotificationsEnabled(enabled)
}
