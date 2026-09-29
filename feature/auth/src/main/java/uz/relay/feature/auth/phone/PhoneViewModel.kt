package uz.relay.feature.auth.phone

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import org.orbitmvi.orbit.blockingIntent
import org.orbitmvi.orbit.viewmodel.orbitContainer
import uz.relay.core.common.result.AppError
import uz.relay.core.common.result.AppResult
import uz.relay.core.common.result.ErrorCodes
import uz.relay.domain.usecase.auth.RequestOtpUseCase
import uz.relay.feature.auth.util.UZ_PHONE_DIGITS
import uz.relay.feature.auth.util.UZ_PREFIX
import javax.inject.Inject

@HiltViewModel
class PhoneViewModel @Inject constructor(
    private val requestOtp: RequestOtpUseCase,
    private val directions: PhoneContract.Directions
) : ViewModel(), PhoneContract.ViewModel {

    override val container =
        orbitContainer<PhoneContract.UiState, PhoneContract.SideEffect>(PhoneContract.UiState())

    override fun onEventDispatcher(intent: PhoneContract.Intent) {
        when (intent) {
            is PhoneContract.Intent.OnPhoneChange -> setDigits(intent.digits)
            PhoneContract.Intent.OnGetCode -> sendCode()
            PhoneContract.Intent.OnOpenBot -> openBot()
            // Guide: never re-request automatically; the user taps once the phone is linked.
            PhoneContract.Intent.OnResendCode -> sendCode()
            PhoneContract.Intent.OnDismissTelegramSheet -> dismissSheet()
        }
    }

    // Text input must update synchronously, otherwise fast typing makes the cursor jump.
    private fun setDigits(digits: String) = blockingIntent {
        reduce { state.copy(digits = digits.filter { it.isDigit() }.take(UZ_PHONE_DIGITS)) }
    }

    private fun sendCode() = intent {
        if (!state.continueEnabled || state.loading) return@intent

        val phone = UZ_PREFIX + state.digits
        reduce { state.copy(loading = true) }

        when (val result = requestOtp(phone)) {
            is AppResult.Success -> {
                reduce { state.copy(loading = false, botUrl = null) }
                directions.navigateToOtp(phone)
            }

            is AppResult.Error -> {
                val botUrl = (result.error as? AppError.Api)
                    ?.takeIf { it.code == ErrorCodes.TELEGRAM_NOT_LINKED }
                    ?.botUrl
                if (botUrl != null) {
                    reduce { state.copy(loading = false, botUrl = botUrl) }
                } else {
                    reduce { state.copy(loading = false) }
                    postSideEffect(PhoneContract.SideEffect.ShowError(result.error))
                }
            }
        }
    }

    private fun openBot() = intent {
        state.botUrl?.let { postSideEffect(PhoneContract.SideEffect.OpenUrl(it)) }
    }

    private fun dismissSheet() = intent {
        reduce { state.copy(botUrl = null) }
    }
}
