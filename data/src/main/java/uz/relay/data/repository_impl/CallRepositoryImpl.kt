package uz.relay.data.repository_impl

import io.getstream.android.video.generated.models.CallSettingsRequest
import io.getstream.android.video.generated.models.RingSettingsRequest
import io.getstream.result.Result
import io.getstream.video.android.core.RingingState
import io.getstream.video.android.core.StreamVideo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
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
        val result = call.create(
            memberIds = listOf(client.userId, peerUserId),
            ring = true,
            video = video,
            // Telegram'dagidek: 15 s ichida javob bo'lmasa server qo'ng'iroqni ikkala tomonda ham o'zi tugatadi
            // (qo'ng'iroq qiluvchida "javob yo'q", qabul qiluvchida jiringlash to'xtaydi).
            settings = CallSettingsRequest(ring = RingSettingsRequest(autoCancelTimeoutMs = RING_TIMEOUT_MS, incomingCallTimeoutMs = RING_TIMEOUT_MS))
        )
        return when (result) {
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

    /**
     * `ring = false` — Telegram'dagi video chat: hech kimga jiringlamaydi, a'zolar chatdagi banner/xabar orqali
     * o'zlari qo'shiladi. `create` aslida "getOrCreate": xona bor bo'lsa xato emas, o'sha xona qaytadi.
     * A'zolar ro'yxati — Stream'da xonaga kirish huquqi shu a'zolikka bog'liq.
     */
    override suspend fun prepareGroupCall(chatId: String, memberIds: List<String>): AppResult<String> {
        val client = StreamVideo.instanceOrNull() ?: return unavailable()
        val callId = groupCallId(chatId)
        val result = client.call(type = CALL_TYPE, id = callId).create(
            memberIds = (memberIds + client.userId).distinct(),
            ring = false,
            video = true
        )
        return when (result) {
            is Result.Success -> AppResult.Success(callId)
            is Result.Failure -> AppError.Api(0, ErrorCodes.CALL_FAILED, result.value.message, retryable = true).let { AppResult.Error(it) }
        }
    }

    /**
     * `get()` xonani "kuzatish"ga oladi (connection id bilan) — shundan keyin kim kirdi/chiqdi event'lari WebSocket
     * orqali `session`ni o'zi yangilaydi. Baribir [GROUP_CALL_REFRESH_MS]da bir qayta so'raymiz: xonani boshqa
     * odam keyinroq yaratsa (biz uni hali kuzatmayotgan bo'lsak) yoki socket uzilib qolsa ham banner to'g'ri bo'lsin.
     * Xona yo'q bo'lsa `get()` 404 qaytaradi — bu shunchaki "video chat yo'q" (0).
     */
    override fun observeGroupCall(chatId: String): Flow<Int> = connector.connectedUserId
        .flatMapLatest { userId ->
            val client = StreamVideo.instanceOrNull()
            if (userId == null || client == null) flowOf(0)
            else channelFlow {
                val call = client.call(type = CALL_TYPE, id = groupCallId(chatId))
                launch {
                    while (isActive) {
                        call.get()
                        delay(GROUP_CALL_REFRESH_MS)
                    }
                }
                call.state.session.collect { session ->
                    val count = if (session == null || session.endedAt != null) 0
                    else session.participants.distinctBy { it.user.id }.size
                    send(count)
                }
            }
        }
        .distinctUntilChanged()

    /** Stream id'da faqat harf, raqam, `_` va `-` bo'lishi mumkin — chat id'dagi boshqa belgilar almashtiriladi. */
    private fun groupCallId(chatId: String): String = "group_" + chatId.replace(Regex("[^A-Za-z0-9_-]"), "_")

    private fun unavailable(): AppResult<String> =
        AppResult.Error(AppError.Api(0, ErrorCodes.CALLS_UNAVAILABLE, "Stream Video client is not connected", retryable = true))

    private companion object {
        const val CALL_TYPE = "default"
        const val RING_TIMEOUT_MS = 15_000
        const val GROUP_CALL_REFRESH_MS = 30_000L
    }
}
