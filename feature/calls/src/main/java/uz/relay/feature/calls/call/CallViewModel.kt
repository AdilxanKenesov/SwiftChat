package uz.relay.feature.calls.call

import android.util.Log
import androidx.lifecycle.ViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import io.getstream.result.Result
import io.getstream.video.android.compose.ui.components.call.controls.actions.DefaultOnCallActionHandler
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.RingingState
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.call.state.AcceptCall
import io.getstream.video.android.core.call.state.CallAction
import io.getstream.video.android.core.call.state.CancelCall
import io.getstream.video.android.core.call.state.DeclineCall
import io.getstream.video.android.core.call.state.LeaveCall
import io.getstream.video.android.core.model.RejectReason
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.orbitmvi.orbit.syntax.Syntax
import org.orbitmvi.orbit.viewmodel.orbitContainer
import uz.relay.core.common.result.AppError
import uz.relay.core.common.result.ErrorCodes
import uz.relay.domain.model.CallLog
import uz.relay.domain.model.CallLogFormat
import uz.relay.domain.model.CallOutcome
import uz.relay.domain.usecase.message.SendTextMessageUseCase

/**
 * Bitta qo'ng'iroqni boshqaradi. `Call` obyekti ViewModel'da saqlanadi (Stream qoidasi: Composable yoki `remember`
 * ichida emas) — ekran burilganda qo'ng'iroq uzilmaydi.
 *
 * Stream'ning `DefaultOnCallActionHandler`i faqat mikrofon/kamera/karnayni boshqaradi; qabul qilish, rad etish,
 * bekor qilish va chiqish bu yerda — SDK'ning o'z `StreamCallActivity`dagi tartibda (accept → join,
 * reject(Decline/Cancel), leave). Chiquvchi qo'ng'iroqni suhbatdosh qabul qilsa, SDK qo'ng'iroq qiluvchini o'zi ulaydi.
 */
@HiltViewModel(assistedFactory = CallViewModel.Factory::class)
class CallViewModel @AssistedInject constructor(
    @Assisted("callId") callId: String,
    @Assisted video: Boolean?,
    @Assisted("chatId") private val chatId: String?,
    private val sendTextMessage: SendTextMessageUseCase,
    private val directions: CallContract.Directions
) : ViewModel(), CallContract.ViewModel {

    @AssistedFactory
    interface Factory {
        fun create(@Assisted("callId") callId: String, video: Boolean?, @Assisted("chatId") chatId: String?): CallViewModel
    }

    /** Bir xil (type, id) har doim bir xil `Call` obyektini qaytaradi — kiruvchi qo'ng'iroqning holati ham shu yerda. */
    val call: Call? = StreamVideo.instanceOrNull()?.call(type = CALL_TYPE, id = callId)

    private val isVideo: Boolean = video ?: (call?.isVideoEnabled() ?: true)

    /** Ekran bir marta yopilsin (bir nechta "tugadi" signali kelishi mumkin). */
    private var finished = false

    /** Suhbatdosh ulanib, haqiqiy suhbat boshlangan vaqt — davomiylik shundan hisoblanadi (jiringlash vaqti kirmaydi). */
    private var answeredAt: Long? = null

    override val container =
        orbitContainer<CallContract.UiState, CallContract.SideEffect>(
            CallContract.UiState(isVideo = isVideo, unavailable = call == null)
        ) {
            if (call != null) {
                observeEnd(call)
                watchRingTimeout(call)
            }
        }

    init {
        // Audio qo'ng'iroqda kamera umuman yoqilmasin (join'dan oldin belgilanadi). ViewModel main thread'da yaratiladi.
        if (!isVideo) call?.camera?.setEnabled(false)
    }

    override fun onEventDispatcher(intent: CallContract.Intent) {
        val call = call
        if (call == null) {
            intent { directions.back() }
            return
        }
        when (intent) {
            is CallContract.Intent.OnCallAction -> onCallAction(call, intent.action)
            CallContract.Intent.OnBack -> onCallAction(call, backAction(call))
            // Slot'lar: rad etildi yoki server 15 s'da javobsiz deb tugatdi.
            CallContract.Intent.OnFinished -> intent { finish(call, outcomeFromRinging(call)) }
        }
    }

    private fun onCallAction(call: Call, action: CallAction) {
        Log.i(TAG, "action=${action::class.simpleName} call=${call.id} ringing=${call.state.ringingState.value::class.simpleName}")
        when (action) {
            is AcceptCall -> intent {
                val result = when (val accepted = call.accept()) {
                    is Result.Success -> call.join()
                    is Result.Failure -> accepted
                }
                if (result is Result.Failure) {
                    postSideEffect(CallContract.SideEffect.ShowError(callError(result.value.message)))
                    finish(call)
                } else if (!isVideo) {
                    withContext(Dispatchers.Main) { call.camera.setEnabled(false) }
                }
            }
            is DeclineCall -> intent {
                call.reject(RejectReason.Decline)
                finish(call, CallOutcome.DECLINED)
            }
            is CancelCall -> intent {
                call.reject(RejectReason.Cancel)
                finish(call, CallOutcome.CANCELED)
            }
            is LeaveCall -> intent { finish(call) }
            // Mikrofon, kamera, karnay, kamerani almashtirish — Stream'ning standart ishlovchisi (main thread'da).
            else -> DefaultOnCallActionHandler.onCallAction(call, action)
        }
    }

    /** Tizim "orqaga": kiruvchida — rad etish, chiquvchida — bekor qilish, faol qo'ng'iroqda — chiqish. */
    private fun backAction(call: Call): CallAction = when (val state = call.state.ringingState.value) {
        is RingingState.Incoming -> if (state.acceptedByMe) LeaveCall else DeclineCall
        is RingingState.Outgoing -> if (state.acceptedByCallee) LeaveCall else CancelCall
        else -> LeaveCall
    }

    /**
     * 1:1 qo'ng'iroq: suhbatdosh chiqib ketsa (u bor edi, endi yo'q) — Telegram'dagidek qo'ng'iroq men uchun ham
     * tugaydi. Aks holda bo'sh qo'ng'iroqda yolg'iz qolib ketardim.
     */
    private fun observeEnd(call: Call) = intent {
        var hadRemote = false
        call.state.remoteParticipants.collect { remotes ->
            if (remotes.isNotEmpty()) {
                hadRemote = true
                if (answeredAt == null) answeredAt = System.currentTimeMillis()
            } else if (hadRemote) {
                finish(call)
            }
        }
    }

    /**
     * Zaxira taymer: server 15 s'da qo'ng'iroqni o'zi tugatadi (RingSettings), lekin uning signali kechiksa yoki
     * kelmasa ham chiquvchi qo'ng'iroq ekranda osilib qolmasin — biroz kutib, hali javob yo'q bo'lsa bekor qilamiz.
     */
    private fun watchRingTimeout(call: Call) = intent {
        delay(RING_TIMEOUT_MS + TIMEOUT_GRACE_MS)
        val state = call.state.ringingState.value
        if (state is RingingState.Outgoing && !state.acceptedByCallee) {
            call.reject(RejectReason.Cancel)
            postSideEffect(CallContract.SideEffect.NoAnswer)
            finish(call, CallOutcome.MISSED)
        }
    }

    /** Jiringlash qanday tugadi: hamma rad etdi → rad etildi, aks holda (vaqt tugadi) → javobsiz. */
    private fun outcomeFromRinging(call: Call): CallOutcome =
        if (call.state.ringingState.value is RingingState.RejectedByAll) CallOutcome.DECLINED else CallOutcome.MISSED

    /**
     * Qo'ng'iroqni yopadi va (men qo'ng'iroq qilgan bo'lsam) chatga tarix yozuvini yuboradi.
     * [fallback] — suhbat bo'lmagan holat sababi; suhbat bo'lgan bo'lsa natija baribir "javob berildi" + davomiylik.
     * `leave()` sinxron, lekin SDK ichki obyektlari main thread'da yaratilgan — shuning uchun Main.
     */
    private suspend fun Syntax<CallContract.UiState, CallContract.SideEffect>.finish(
        call: Call,
        fallback: CallOutcome = CallOutcome.CANCELED
    ) {
        if (finished) return
        finished = true
        Log.i(TAG, "finish call=${call.id} answered=${answeredAt != null} fallback=$fallback")
        withContext(Dispatchers.Main) { call.leave() }
        saveCallLog(fallback)
        directions.back()
    }

    /**
     * Tarix faqat chiquvchi qo'ng'iroqda yoziladi ([chatId] faqat shunda bor) — ikkala tomon yozsa dublikat bo'lardi.
     * Oddiy matnli xabar sifatida outbox orqali ketadi: internet bo'lmasa ham yo'qolmaydi.
     */
    private suspend fun saveCallLog(fallback: CallOutcome) {
        val chatId = chatId ?: return
        val started = answeredAt
        val log = if (started != null) {
            CallLog(video = isVideo, outcome = CallOutcome.ANSWERED, durationSeconds = (System.currentTimeMillis() - started) / 1000)
        } else {
            CallLog(video = isVideo, outcome = fallback, durationSeconds = 0)
        }
        sendTextMessage(chatId, CallLogFormat.format(log), replyToClientMessageId = null)
    }

    private fun callError(message: String) = AppError.Api(0, ErrorCodes.CALL_FAILED, message, retryable = true)

    /** Ekrandan boshqa yo'l bilan chiqilsa ham (masalan, stek tozalansa) qo'ng'iroq osilib qolmasin. */
    override fun onCleared() {
        if (!finished) call?.leave()
    }

    private companion object {
        const val TAG = "SwiftChat.Call"
        const val CALL_TYPE = "default"
        /** Server sozlamasi bilan bir xil (CallRepositoryImpl.RING_TIMEOUT_MS). */
        const val RING_TIMEOUT_MS = 15_000L
        /** Server signalini kutish uchun qo'shimcha vaqt. */
        const val TIMEOUT_GRACE_MS = 2_000L
    }
}
