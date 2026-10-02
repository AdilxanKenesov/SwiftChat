package uz.relay.domain.usecase.call

import kotlinx.coroutines.flow.Flow
import uz.relay.domain.repository.CallRepository
import javax.inject.Inject

/** Kiruvchi qo'ng'iroqlar — ilova darajasida (MainViewModel) kuzatiladi va qo'ng'iroq ekrani ochiladi. */
class ObserveIncomingCallsUseCase @Inject constructor(
    private val repository: CallRepository
) {
    operator fun invoke(): Flow<String> = repository.observeIncomingCalls()
}
