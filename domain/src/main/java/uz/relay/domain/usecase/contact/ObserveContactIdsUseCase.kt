package uz.relay.domain.usecase.contact

import kotlinx.coroutines.flow.Flow
import uz.relay.domain.repository.ContactRepository
import javax.inject.Inject

/** [ContactRepository.observeContactIds()] ni UI'ga beradi (ViewModel repository'ni to'g'ridan-to'g'ri bilmaydi). */
class ObserveContactIdsUseCase @Inject constructor(
    private val repository: ContactRepository
) {
    operator fun invoke(): Flow<Set<String>> = repository.observeContactIds()
}
