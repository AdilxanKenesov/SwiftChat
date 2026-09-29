package uz.relay.domain.usecase.user

import uz.relay.core.common.result.AppResult
import uz.relay.domain.model.User
import uz.relay.domain.repository.UserRepository
import javax.inject.Inject

class SearchUsersUseCase @Inject constructor(
    private val repository: UserRepository
) {
    /** Bo'sh so'rov serverga yuborilmaydi (API kamida 1 belgi talab qiladi). "@ali" ham "ali" deb qidiriladi. */
    suspend operator fun invoke(query: String): AppResult<List<User>> {
        val trimmed = query.trim().removePrefix("@")
        return if (trimmed.isEmpty()) AppResult.Success(emptyList()) else repository.search(trimmed)
    }
}
