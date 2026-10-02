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

/**
 * OTP ekranining ViewModel'i: kodni tekshiradi, noto'g'ri urinishlarni sanaydi va qayta yuborish taymerini boshqaradi.
 *
 * `phone` runtime'da Nav3 key'dan keladi, use case'lar esa Hilt'dan: AssistedInject ikkalasini birlashtiradi.
 * Shu tufayli raqamni SavedStateHandle yoki global holat orqali uzatish shart emas.
 */
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

    // Container yaratilishi bilan qayta yuborish taymeri ishga tushadi (kod hozirgina yuborilgan).
    override val container =
        orbitContainer<OtpContract.UiState, OtpContract.SideEffect>(
            OtpContract.UiState(phone = phone, codeLength = OtpRules.CODE_LENGTH)
        ) {
            startTimer()
        }

    // Joriy kod uchun noto'g'ri urinishlar; server 5 tadan keyin kodni bloklaydi va sonini qaytarmaydi.
    private var wrongAttempts = 0
    private var timerJob: Job? = null

    override fun onEventDispatcher(intent: OtpContract.Intent) {
        when (intent) {
            is OtpContract.Intent.OnCodeChange -> setCode(intent.code)
            OtpContract.Intent.OnResendCode -> resend()
            OtpContract.Intent.OnBack -> intent { directions.back() }
        }
    }

    // Matn kiritish sinxron yangilanishi kerak (blockingIntent), aks holda tez yozganda raqamlar tushib qoladi.
    private fun setCode(input: String) {
        var complete: String? = null
        blockingIntent {
            if (!state.inputEnabled) return@blockingIntent
            val code = input.filter { it.isDigit() }.take(state.codeLength)
            reduce { state.copy(code = code, status = OtpContract.Status.Input) }
            if (code.length == state.codeLength) complete = code
        }
        // Tekshiruv blockingIntent'dan tashqarida alohida intent sifatida boshlanadi - tarmoq so'rovi kiritishni bloklamasligi uchun.
        complete?.let(::verify)
    }

    /** Kod to'liq kiritilganda avtomatik chaqiriladi. Natija `true` bo'lsa - profil hali to'ldirilmagan. */
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

    /** Server xato kodlarini ekran holatiga aylantiradi. */
    private fun onVerifyError(error: AppError) = intent {
        when ((error as? AppError.Api)?.code) {
            ErrorCodes.INVALID_OTP -> {
                wrongAttempts++
                val left = MAX_ATTEMPTS - wrongAttempts
                reduce {
                    // 5-xatodan keyin server kodni bloklagan; 6-urinishni kutish shart emas.
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
                // Yangi kodga yana 5 ta urinish beriladi (server ham hisoblagichini nolga tushiradi).
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

    /** Qayta yuborish tugmasi uchun teskari sanoq. Oldingi taymer bekor qilinadi, shunda ikkita taymer parallel ishlamaydi. */
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
