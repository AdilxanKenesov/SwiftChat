package uz.relay.domain.usecase.chat

import uz.relay.core.common.result.AppResult
import uz.relay.domain.model.MuteDuration
import uz.relay.domain.repository.ChatRepository
import javax.inject.Inject

class SetChatMutedUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    /** Oddiy almashtirish (profil va guruh ma'lumotidagi tugma): yoqilsa — muddatsiz. */
    suspend operator fun invoke(chatId: String, muted: Boolean): AppResult<Unit> = repository.setMuted(chatId, muted)

    /**
     * Muddat bilan ovozsiz qilish (chatlar ro'yxatidagi long-press). Tugash vaqti shu yerda hisoblanadi —
     * server mutlaq vaqtni (epoch ms) kutadi, "necha soat" emas.
     */
    suspend operator fun invoke(chatId: String, duration: MuteDuration): AppResult<Unit> =
        repository.setMuted(chatId, muted = true, mutedUntil = duration.millis?.let { System.currentTimeMillis() + it })
}
