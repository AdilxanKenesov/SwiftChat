package uz.relay.domain.usecase.settings

import kotlinx.coroutines.flow.Flow
import uz.relay.domain.model.AppLanguage
import uz.relay.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * Ilova tilini kuzatadi. MainActivity shu orqali Activity'ni kerakli tilda qayta yaratadi.
 *
 * Faqat repository'ga uzatadi — ViewModel repository'ni emas, aniq nomli amalni bilishi uchun (single responsibility, test'da oson almashtiriladi).
 */
class ObserveLanguageUseCase @Inject constructor(
    private val repository: SettingsRepository
) {
    operator fun invoke(): Flow<AppLanguage> = repository.language
}
