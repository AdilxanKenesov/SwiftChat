package uz.relay.domain.usecase.settings

import uz.relay.domain.repository.SettingsRepository
import javax.inject.Inject

/** Paneldan tanlangan emojini "So'nggi" ro'yxatining boshiga qo'yadi. */
class AddRecentEmojiUseCase @Inject constructor(
    private val repository: SettingsRepository
) {
    suspend operator fun invoke(emoji: String) = repository.addRecentEmoji(emoji)
}
