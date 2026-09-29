package uz.relay.domain.usecase.user

import kotlinx.coroutines.flow.Flow
import uz.relay.domain.model.User
import uz.relay.domain.repository.UserRepository
import javax.inject.Inject

class ObserveMeUseCase @Inject constructor(
    private val repository: UserRepository
) {
    operator fun invoke(): Flow<User?> = repository.observeMe()
}
