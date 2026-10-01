package uz.relay.feature.calls.call

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
import kotlinx.coroutines.withContext
import org.orbitmvi.orbit.syntax.Syntax
import org.orbitmvi.orbit.viewmodel.orbitContainer
import uz.relay.core.common.result.AppError
import uz.relay.core.common.result.ErrorCodes

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
    @Assisted callId: String,
    @Assisted video: Boolean?,
    private val directions: CallContract.Directions
) : ViewModel(), CallContract.ViewModel {

    @AssistedFactory
    interface Factory {
        fun create(callId: String, video: Boolean?): CallViewModel
    }

    /** Bir xil (type, id) har doim bir xil `Call` obyektini qaytaradi — kiruvchi qo'ng'iroqning holati ham shu yerda. */
    val call: Call? = StreamVideo.instanceOrNull()?.call(type = CALL_TYPE, id = callId)

    private val isVideo: Boolean = video ?: (call?.isVideoEnabled() ?: true)

    /** Ekran bir marta yopilsin (bir nechta "tugadi" signali kelishi mumkin). */
    private var finished = false

    override val container =
        orbitContainer<CallContract.UiState, CallContract.SideEffect>(
            CallContract.UiState(isVideo = isVideo, unavailable = call == null)
        ) {
            if (call != null) observeEnd(call)
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
            CallContract.Intent.OnFinished -> intent { finish(call) }
        }
    }

    private fun onCallAction(call: Call, action: CallAction) {
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
                finish(call)
            }
            is CancelCall -> intent {
                call.reject(RejectReason.Cancel)
                finish(call)
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
            if (remotes.isNotEmpty()) hadRemote = true
            else if (hadRemote) finish(call)
        }
    }

    /** `leave()` sinxron, lekin SDK ichki obyektlari main thread'da yaratilgan — shuning uchun Main. */
    private suspend fun Syntax<CallContract.UiState, CallContract.SideEffect>.finish(call: Call) {
        if (finished) return
        finished = true
        withContext(Dispatchers.Main) { call.leave() }
        directions.back()
    }

    private fun callError(message: String) = AppError.Api(0, ErrorCodes.CALL_FAILED, message, retryable = true)

    /** Ekrandan boshqa yo'l bilan chiqilsa ham (masalan, stek tozalansa) qo'ng'iroq osilib qolmasin. */
    override fun onCleared() {
        if (!finished) call?.leave()
    }

    private companion object {
        const val CALL_TYPE = "default"
    }
}
