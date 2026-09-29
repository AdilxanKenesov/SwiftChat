package uz.relay.domain.usecase.auth

import kotlinx.coroutines.flow.Flow
import uz.relay.domain.model.AuthState
import uz.relay.domain.repository.AuthRepository
import javax.inject.Inject

class ObserveAuthStateUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    operator fun invoke(): Flow<AuthState> = repository.authState
}
