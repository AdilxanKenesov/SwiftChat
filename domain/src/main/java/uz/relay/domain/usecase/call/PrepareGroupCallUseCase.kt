package uz.relay.domain.usecase.call

import uz.relay.core.common.result.AppResult
import uz.relay.domain.repository.CallRepository
import javax.inject.Inject

/** Guruh video chati xonasini tayyorlaydi (yo'q bo'lsa yaratadi). Qiymat — xona id'si. */
class PrepareGroupCallUseCase @Inject constructor(
    private val repository: CallRepository
) {
    suspend operator fun invoke(chatId: String, memberIds: List<String>): AppResult<String> =
        repository.prepareGroupCall(chatId, memberIds)
}
