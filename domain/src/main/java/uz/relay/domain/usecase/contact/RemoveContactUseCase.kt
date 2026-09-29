package uz.relay.domain.usecase.contact

import uz.relay.domain.repository.ContactRepository
import javax.inject.Inject

/** Kontaktni o'chiradi — faqat ro'yxatdan; chat va xabarlarga tegmaydi. */
class RemoveContactUseCase @Inject constructor(
    private val repository: ContactRepository
) {
    suspend operator fun invoke(userId: String) = repository.remove(userId)
}
