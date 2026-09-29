package uz.relay.feature.auth.phone

import org.orbitmvi.orbit.OrbitContainerHost
import uz.relay.core.common.result.AppError

/**
 * Telefon raqamini kiritish ekranining Orbit MVI kontrakti.
 *
 * Orbit MVI tanlangan, chunki ekranning butun holati bitta o'zgarmas [UiState] da, foydalanuvchi
 * harakatlari [Intent] sifatida, bir martalik hodisalar (snackbar, brauzer ochish) esa [SideEffect]
 * sifatida keladi - bu oqimni oldindan aytib bo'ladigan, test qilinadigan qiladi va ViewModel
 * konfiguratsiya o'zgarishida holatni saqlab qoladi. Contract hammasini bitta joyda guruhlaydi.
 *
 * Oqim: Splash (sessiya yo'q) -> shu ekran -> OTP ekrani.
 */
interface PhoneContract {

    /** UI faqat shu interfeysni biladi: holatni o'qiydi va Intent'larni [onEventDispatcher] ga beradi. */
    interface ViewModel : OrbitContainerHost<UiState, UiState, SideEffect> {
        fun onEventDispatcher(intent: Intent)
    }

    /** Foydalanuvchi harakatlari. */
    sealed interface Intent {
        data class OnPhoneChange(val digits: String) : Intent
        object OnGetCode : Intent
        object OnOpenBot : Intent
        object OnResendCode : Intent
        object OnDismissTelegramSheet : Intent
    }

    /** Bir martalik hodisalar - state'da saqlanmaydi, aks holda ekran aylanganda qayta ko'rsatilardi. */
    sealed interface SideEffect {
        data class ShowError(val error: AppError) : SideEffect
        data class OpenUrl(val url: String) : SideEffect
    }

    data class UiState(
        /** Faqat mahalliy qism, 9 tagacha raqam (+998 prefiksisiz). */
        val digits: String = "",
        val loading: Boolean = false,
        /** "Telegram'ni bog'lash" sheet'i ko'rinib turganda null emas (409 TELEGRAM_NOT_LINKED). */
        val botUrl: String? = null
    ) {
        // Hamma 9 ta raqam kiritilgandagina "Kod olish" tugmasi faol bo'ladi.
        val continueEnabled: Boolean get() = digits.length == 9
    }

    /** Navigatsiya ViewModel'dan yashirilgan - u faqat "OTP ga o'tish" kerakligini biladi. */
    interface Directions {
        suspend fun navigateToOtp(phone: String)
    }
}
