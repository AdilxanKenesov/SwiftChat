package uz.relay.domain.usecase.call

import uz.relay.core.common.result.AppResult
import uz.relay.domain.repository.CallRepository
import javax.inject.Inject

/** Shaxsiy chatdagi suhbatdoshga audio yoki video qo'ng'iroq boshlaydi. Qiymat — qo'ng'iroq id'si. */
class StartCallUseCase @Inject constructor(
    private val repository: CallRepository
) {
    suspend operator fun invoke(peerUserId: String, video: Boolean): AppResult<String> = repository.startCall(peerUserId, video)
}
