package uz.relay.domain.usecase.settings

import uz.relay.domain.model.ThemeMode
import uz.relay.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * Ilova temasini almashtiradi.
 *
 * Faqat repository'ga uzatadi — ViewModel repository'ni emas, aniq nomli amalni bilishi uchun (single responsibility, test'da oson almashtiriladi).
 */
class SetThemeModeUseCase @Inject constructor(
    private val repository: SettingsRepository
) {
    suspend operator fun invoke(mode: ThemeMode) = repository.setThemeMode(mode)
}
