package uz.relay.domain.usecase.chat

import kotlinx.coroutines.flow.Flow
import uz.relay.domain.model.SyncStatus
import uz.relay.domain.repository.ChatRepository
import javax.inject.Inject

class ObserveSyncStatusUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    operator fun invoke(): Flow<SyncStatus> = repository.observeSyncStatus()
}
