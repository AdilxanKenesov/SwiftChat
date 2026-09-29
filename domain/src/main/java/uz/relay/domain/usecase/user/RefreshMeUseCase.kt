package uz.relay.domain.usecase.user

import uz.relay.core.common.result.AppResult
import uz.relay.domain.model.User
import uz.relay.domain.repository.UserRepository
import javax.inject.Inject

class RefreshMeUseCase @Inject constructor(
    private val repository: UserRepository
) {
    suspend operator fun invoke(): AppResult<User> = repository.refreshMe()
}
