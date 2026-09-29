package uz.relay.domain.usecase.auth

import uz.relay.core.common.result.AppResult
import uz.relay.domain.repository.AuthRepository
import javax.inject.Inject

class RequestOtpUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(phone: String): AppResult<Unit> = repository.requestOtp(phone)
}
