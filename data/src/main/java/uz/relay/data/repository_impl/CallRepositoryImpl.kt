package uz.relay.data.repository_impl

import io.getstream.result.Result
import io.getstream.video.android.core.RingingState
import io.getstream.video.android.core.StreamVideo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import uz.relay.core.common.result.AppError
import uz.relay.core.common.result.AppResult
import uz.relay.core.common.result.ErrorCodes
import uz.relay.data.call.StreamVideoConnector
import uz.relay.domain.repository.CallRepository
import java.util.UUID
import javax.inject.Inject

/** [CallRepository]ning Stream Video bilan implementatsiyasi. Stream tiplari shu klassdan tashqariga chiqmaydi. */
@OptIn(ExperimentalCoroutinesApi::class) // flatMapLatest
internal class CallRepositoryImpl @Inject constructor(
    private val connector: StreamVideoConnector
) : CallRepository {

    /**
     * `default` turdagi yangi qo'ng'iroq: har safar yangi UUID — Stream bitta id uchun faqat bir marta jiringlatadi.
     * `ring = true` suhbatdoshda kiruvchi qo'ng'iroqni boshlaydi; `video` — kamera yoqilgan holatda boshlanishi.
     * Suhbatdosh Stream'ga kamida bir marta ulangan bo'lishi kerak (ilovaga kirgan bo'lsa — ulangan).
     */
    override suspend fun startCall(peerUserId: String, video: Boolean): AppResult<String> {
        val client = StreamVideo.instanceOrNull() ?: return unavailable()
        val callId = UUID.randomUUID().toString()
        val call = client.call(type = CALL_TYPE, id = callId)
        return when (val result = call.create(memberIds = listOf(client.userId, peerUserId), ring = true, video = video)) {
            is Result.Success -> AppResult.Success(callId)
            is Result.Failure -> AppError.Api(0, ErrorCodes.CALL_FAILED, result.value.message, retryable = true).let { AppResult.Error(it) }
        }
    }

    /**
     * Client almashsa (login/logout) kuzatish o'zi qayta boshlanadi. Faqat hali qabul qilinmagan kiruvchi qo'ng'iroq
     * chiqariladi — o'zim qabul qilgan yoki chiquvchi qo'ng'iroq ekranni qayta ochmaydi.
     */
    override fun observeIncomingCalls(): Flow<String> = connector.connectedUserId
        .flatMapLatest { userId ->
            val client = StreamVideo.instanceOrNull()
            if (userId == null || client == null) emptyFlow()
            else client.state.ringingCall.flatMapLatest { call ->
                call?.state?.ringingState?.map { state ->
                    call.id.takeIf { state is RingingState.Incoming && !state.acceptedByMe }
                } ?: flowOf(null)
            }
        }
        .filterNotNull()
        .distinctUntilChanged()

    private fun unavailable(): AppResult<String> =
        AppResult.Error(AppError.Api(0, ErrorCodes.CALLS_UNAVAILABLE, "Stream Video client is not connected", retryable = true))

    private companion object {
        const val CALL_TYPE = "default"
    }
}
