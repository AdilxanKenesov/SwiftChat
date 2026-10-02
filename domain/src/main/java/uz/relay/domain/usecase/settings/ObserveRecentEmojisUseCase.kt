package uz.relay.domain.usecase.settings

import kotlinx.coroutines.flow.Flow
import uz.relay.domain.repository.SettingsRepository
import javax.inject.Inject

/** Emoji panelidagi "So'nggi" bo'limi. */
class ObserveRecentEmojisUseCase @Inject constructor(
    private val repository: SettingsRepository
) {
    operator fun invoke(): Flow<List<String>> = repository.recentEmojis
}
