package uz.relay.domain.usecase.settings

import uz.relay.domain.repository.SettingsRepository
import javax.inject.Inject

class SetNotificationsEnabledUseCase @Inject constructor(
    private val repository: SettingsRepository
) {
    suspend operator fun invoke(enabled: Boolean) = repository.setNotificationsEnabled(enabled)
}
