package uz.relay.domain.usecase.contact

import uz.relay.core.common.result.AppResult
import uz.relay.domain.repository.ContactRepository
import javax.inject.Inject

/** Foydalanuvchini qurilmadagi kontaktlarga qo'shadi (profil keshda bo'lmasa avval yuklanadi). */
class AddContactUseCase @Inject constructor(
    private val repository: ContactRepository
) {
    suspend operator fun invoke(userId: String): AppResult<Unit> = repository.add(userId)
}
