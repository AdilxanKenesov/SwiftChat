package uz.relay.domain.usecase.chat

import kotlinx.coroutines.flow.Flow
import uz.relay.domain.model.ConnectionStatus
import uz.relay.domain.repository.ConnectionRepository
import javax.inject.Inject

class ObserveConnectionStatusUseCase @Inject constructor(
    private val repository: ConnectionRepository
) {
    operator fun invoke(): Flow<ConnectionStatus> = repository.status
}
