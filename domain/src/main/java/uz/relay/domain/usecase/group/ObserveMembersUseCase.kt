package uz.relay.domain.usecase.group

import kotlinx.coroutines.flow.Flow
import uz.relay.domain.model.ChatMember
import uz.relay.domain.repository.GroupRepository
import javax.inject.Inject

/**
 * Guruh a'zolarini lokal bazadan kuzatadi.
 *
 * Faqat repository'ga uzatadi — ViewModel repository'ni emas, aniq nomli amalni bilishi uchun (single responsibility, test'da oson almashtiriladi).
 */
class ObserveMembersUseCase @Inject constructor(
    private val repository: GroupRepository
) {
    operator fun invoke(chatId: String): Flow<List<ChatMember>> = repository.observeMembers(chatId)
}
