package uz.relay.domain.usecase.settings

import uz.relay.domain.model.ThemeMode
import uz.relay.domain.repository.SettingsRepository
import javax.inject.Inject

class SetThemeModeUseCase @Inject constructor(
    private val repository: SettingsRepository
) {
    suspend operator fun invoke(mode: ThemeMode) = repository.setThemeMode(mode)
}
