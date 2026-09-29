package uz.relay.domain.usecase.settings

import uz.relay.domain.model.AppLanguage
import uz.relay.domain.repository.SettingsRepository
import javax.inject.Inject

class SetLanguageUseCase @Inject constructor(
    private val repository: SettingsRepository
) {
    suspend operator fun invoke(language: AppLanguage) = repository.setLanguage(language)
}
