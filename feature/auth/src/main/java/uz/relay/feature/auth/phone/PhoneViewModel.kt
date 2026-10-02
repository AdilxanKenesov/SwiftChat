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

/**
 * Telefon ekranining ViewModel'i: raqamni qabul qiladi, OTP so'raydi va OTP ekraniga o'tkazadi.
 *
 * Agar raqam Telegram bot'ga bog'lanmagan bo'lsa (409 TELEGRAM_NOT_LINKED), server bot havolasini
 * qaytaradi - u state'ga yoziladi va ekran [uz.relay.feature.auth.phone.components.TelegramLinkSheet] ni ko'rsatadi.
 */
@HiltViewModel
class PhoneViewModel @Inject constructor(
    private val requestOtp: RequestOtpUseCase,
    private val directions: PhoneContract.Directions
) : ViewModel(), PhoneContract.ViewModel {

    // Boshlang'ich yuklash yo'q, shuning uchun onCreate bloki kerak emas.
    override val container =
        orbitContainer<PhoneContract.UiState, PhoneContract.SideEffect>(PhoneContract.UiState())

    override fun onEventDispatcher(intent: PhoneContract.Intent) {
        when (intent) {
            is PhoneContract.Intent.OnPhoneChange -> setDigits(intent.digits)
            PhoneContract.Intent.OnGetCode -> sendCode()
            PhoneContract.Intent.OnOpenBot -> openBot()
            // Qoidaga ko'ra hech qachon avtomatik qayta so'ramaymiz; raqam bog'langach foydalanuvchi o'zi bosadi.
            PhoneContract.Intent.OnResendCode -> sendCode()
            PhoneContract.Intent.OnDismissTelegramSheet -> dismissSheet()
        }
    }

    // Matn kiritish sinxron yangilanishi kerak (blockingIntent), aks holda tez yozganda kursor sakraydi.
    private fun setDigits(digits: String) = blockingIntent {
        reduce { state.copy(digits = digits.filter { it.isDigit() }.take(UZ_PHONE_DIGITS)) }
    }

    private fun sendCode() = intent {
        // Takroriy bosish yoki to'liq bo'lmagan raqam bilan so'rov yubormaymiz.
        if (!state.continueEnabled || state.loading) return@intent

        val phone = UZ_PREFIX + state.digits
        reduce { state.copy(loading = true) }

        when (val result = requestOtp(phone)) {
            is AppResult.Success -> {
                reduce { state.copy(loading = false, botUrl = null) }
                directions.navigateToOtp(phone)
            }

            is AppResult.Error -> {
                // Faqat TELEGRAM_NOT_LINKED xatosida bot havolasi keladi - boshqa xatolar snackbar orqali ko'rsatiladi.
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

    // Brauzer/Telegram'ni ochish Context talab qiladi, shuning uchun uni UI SideEffect orqali bajaradi.
    private fun openBot() = intent {
        state.botUrl?.let { postSideEffect(PhoneContract.SideEffect.OpenUrl(it)) }
    }

    private fun dismissSheet() = intent {
        reduce { state.copy(botUrl = null) }
    }
}
