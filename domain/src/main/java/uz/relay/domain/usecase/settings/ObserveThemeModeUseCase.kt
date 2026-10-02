package uz.relay.domain.usecase.settings

import kotlinx.coroutines.flow.Flow
import uz.relay.domain.model.ThemeMode
import uz.relay.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * Tanlangan temani (kunduzgi/tungi) kuzatadi — SwiftTheme'ga uzatiladi.
 *
 * Faqat repository'ga uzatadi — ViewModel repository'ni emas, aniq nomli amalni bilishi uchun (single responsibility, test'da oson almashtiriladi).
 */
class ObserveThemeModeUseCase @Inject constructor(
    private val repository: SettingsRepository
) {
    operator fun invoke(): Flow<ThemeMode> = repository.themeMode
}
