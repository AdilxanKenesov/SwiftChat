package uz.relay.feature.auth.otp

import androidx.lifecycle.ViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import org.orbitmvi.orbit.blockingIntent
import org.orbitmvi.orbit.viewmodel.orbitContainer
import uz.relay.core.common.result.AppError
import uz.relay.core.common.result.AppResult
import uz.relay.core.common.result.ErrorCodes
import uz.relay.domain.model.OtpRules
import uz.relay.domain.usecase.auth.RequestOtpUseCase
import uz.relay.domain.usecase.auth.VerifyOtpUseCase
import uz.relay.feature.auth.otp.OtpContract.Companion.MAX_ATTEMPTS
import uz.relay.feature.auth.otp.OtpContract.Companion.RESEND_SECONDS
import kotlin.time.Duration.Companion.seconds

/** `phone` comes from the Nav3 key at runtime, the use cases from Hilt: AssistedInject joins them. */
@HiltViewModel(assistedFactory = OtpViewModel.Factory::class)
class OtpViewModel @AssistedInject constructor(
    @Assisted phone: String,
    private val requestOtp: RequestOtpUseCase,
    private val verifyOtp: VerifyOtpUseCase,
    private val directions: OtpContract.Directions
) : ViewModel(), OtpContract.ViewModel {

    @AssistedFactory
    interface Factory {
        fun create(phone: String): OtpViewModel
    }

    override val container =
        orbitContainer<OtpContract.UiState, OtpContract.SideEffect>(
            OtpContract.UiState(phone = phone, codeLength = OtpRules.CODE_LENGTH)
        ) {
            startTimer()
        }

    // Wrong guesses for the current code; the server locks it after 5 and does not report the count.
    private var wrongAttempts = 0
    private var timerJob: Job? = null

    override fun onEventDispatcher(intent: OtpContract.Intent) {
        when (intent) {
            is OtpContract.Intent.OnCodeChange -> setCode(intent.code)
            OtpContract.Intent.OnResendCode -> resend()
            OtpContract.Intent.OnBack -> intent { directions.back() }
        }
    }

    // Text input must update synchronously, otherwise fast typing drops digits.
    private fun setCode(input: String) {
        var complete: String? = null
        blockingIntent {
            if (!state.inputEnabled) return@blockingIntent
            val code = input.filter { it.isDigit() }.take(state.codeLength)
            reduce { state.copy(code = code, status = OtpContract.Status.Input) }
            if (code.length == state.codeLength) complete = code
        }
        complete?.let(::verify)
    }

    private fun verify(code: String) = intent {
        if (state.verifying) return@intent
        reduce { state.copy(verifying = true) }

        when (val result = verifyOtp(state.phone, code)) {
            is AppResult.Success -> {
                reduce { state.copy(verifying = false) }
                if (result.data) directions.navigateToProfileSetup() else directions.navigateToChats()
            }

            is AppResult.Error -> onVerifyError(result.error)
        }
    }

    private fun onVerifyError(error: AppError) = intent {
        when ((error as? AppError.Api)?.code) {
            ErrorCodes.INVALID_OTP -> {
                wrongAttempts++
                val left = MAX_ATTEMPTS - wrongAttempts
                reduce {
                    // After the 5th miss the server has locked the code; no need to wait for a 6th try.
                    if (left > 0) state.copy(verifying = false, status = OtpContract.Status.Wrong(left))
                    else state.copy(verifying = false, code = "", status = OtpContract.Status.Locked)
                }
                postSideEffect(OtpContract.SideEffect.Shake)
            }

            ErrorCodes.OTP_EXPIRED -> reduce { state.copy(verifying = false, status = OtpContract.Status.Expired) }
            ErrorCodes.OTP_LOCKED -> reduce {
                state.copy(verifying = false, code = "", status = OtpContract.Status.Locked)
            }

            else -> {
                reduce { state.copy(verifying = false, code = "") }
                postSideEffect(OtpContract.SideEffect.ShowError(error))
            }
        }
    }

    private fun resend() = intent {
        if (state.resending) return@intent
        reduce { state.copy(resending = true) }

        when (val result = requestOtp(state.phone)) {
            is AppResult.Success -> {
                // A new code gets 5 fresh attempts (the server resets its counter too).
                wrongAttempts = 0
                reduce { state.copy(resending = false, code = "", status = OtpContract.Status.Input) }
                startTimer()
            }

            is AppResult.Error -> {
                reduce { state.copy(resending = false) }
                postSideEffect(OtpContract.SideEffect.ShowError(result.error))
            }
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = intent {
            for (seconds in RESEND_SECONDS downTo 0) {
                reduce { state.copy(secondsLeft = seconds) }
                if (seconds > 0) delay(1.seconds)
            }
        }
    }
}
