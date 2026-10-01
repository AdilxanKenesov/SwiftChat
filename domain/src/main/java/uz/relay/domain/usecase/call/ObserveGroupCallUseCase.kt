package uz.relay.domain.usecase.call

import kotlinx.coroutines.flow.Flow
import uz.relay.domain.repository.CallRepository
import javax.inject.Inject

/** Guruh video chatidagi odamlar soni (0 — video chat yo'q). */
class ObserveGroupCallUseCase @Inject constructor(
    private val repository: CallRepository
) {
    operator fun invoke(chatId: String): Flow<Int> = repository.observeGroupCall(chatId)
}
