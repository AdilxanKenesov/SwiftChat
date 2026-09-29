package uz.relay.domain.usecase.user

import kotlinx.coroutines.flow.Flow
import uz.relay.domain.repository.UserRepository
import javax.inject.Inject

class ObserveUserNamesUseCase @Inject constructor(
    private val repository: UserRepository
) {
    operator fun invoke(): Flow<Map<String, String>> = repository.observeUserNames()
}
