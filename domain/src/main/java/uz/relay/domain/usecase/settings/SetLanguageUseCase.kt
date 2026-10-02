package uz.relay.domain.usecase.settings

import uz.relay.domain.model.AppLanguage
import uz.relay.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * Ilova tilini almashtiradi (qurilma sozlamasi, logout'da o'chmaydi).
 *
 * Faqat repository'ga uzatadi — ViewModel repository'ni emas, aniq nomli amalni bilishi uchun (single responsibility, test'da oson almashtiriladi).
 */
class SetLanguageUseCase @Inject constructor(
    private val repository: SettingsRepository
) {
    suspend operator fun invoke(language: AppLanguage) = repository.setLanguage(language)
}
