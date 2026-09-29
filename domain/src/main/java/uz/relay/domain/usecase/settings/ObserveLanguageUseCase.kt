package uz.relay.domain.usecase.settings

import kotlinx.coroutines.flow.Flow
import uz.relay.domain.model.AppLanguage
import uz.relay.domain.repository.SettingsRepository
import javax.inject.Inject

class ObserveLanguageUseCase @Inject constructor(
    private val repository: SettingsRepository
) {
    operator fun invoke(): Flow<AppLanguage> = repository.language
}
