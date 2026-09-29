package uz.relay.domain.usecase.auth

import uz.relay.core.common.result.AppResult
import uz.relay.domain.repository.AuthRepository
import javax.inject.Inject

class VerifyOtpUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    /** The value is `isNewUser`. */
    suspend operator fun invoke(phone: String, code: String): AppResult<Boolean> =
        repository.verifyOtp(phone, code)
}
