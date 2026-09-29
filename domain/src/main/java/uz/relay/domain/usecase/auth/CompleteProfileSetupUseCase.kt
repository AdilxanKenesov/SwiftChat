package uz.relay.domain.usecase.auth

import uz.relay.domain.repository.AuthRepository
import javax.inject.Inject

class CompleteProfileSetupUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke() = repository.completeProfileSetup()
}
