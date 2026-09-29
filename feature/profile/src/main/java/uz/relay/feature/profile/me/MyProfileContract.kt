package uz.relay.feature.profile.me

import org.orbitmvi.orbit.OrbitContainerHost
import uz.relay.core.common.result.AppError
import uz.relay.domain.model.AppLanguage
import uz.relay.domain.model.ConnectionStatus
import uz.relay.domain.model.ThemeMode
import uz.relay.domain.model.User

/**
 * "Mening profilim" ekranining Orbit MVI shartnomasi: profil ma'lumoti, ulanish holati va ilova sozlamalari
 * (bildirishnomalar, tungi rejim, til) hamda chiqish. Chatlar ro'yxatidagi profil tugmasidan ochiladi,
 * bu yerdan profilni tahrirlashga o'tiladi.
 *
 * Sozlamalar state'da faqat aks ettiriladi — haqiqiy qiymat sozlamalar ombori (DataStore) da, shuning uchun
 * switch bosilganda state'ni qo'lda o'zgartirmaymiz: use case yozadi, Flow yangi qiymatni qaytaradi.
 */
interface MyProfileContract {

    /** Screen ko'radigan ViewModel interfeysi: state/sideEffect va yagona [onEventDispatcher]. */
    interface ViewModel : OrbitContainerHost<UiState, UiState, SideEffect> {
        fun onEventDispatcher(intent: Intent)
    }

    /** Foydalanuvchi harakatlari: navigatsiya, sozlamalarni o'zgartirish va chiqish. */
    sealed interface Intent {
        object OnBack : Intent
        object OnEdit : Intent
        data class OnNotificationsChange(val enabled: Boolean) : Intent
        /** Switch'ning yangi holati: yoniq — tungi, o'chiq — kunduzgi rejim. */
        data class OnDarkModeChange(val enabled: Boolean) : Intent
        data class OnLanguageChange(val language: AppLanguage) : Intent
        /** Tasdiq dialogidan keyin. */
        object OnLogout : Intent
    }

    /** Bir martalik xatolar — Screen ularni Snackbar'ga aylantiradi. */
    sealed interface SideEffect {
        data class ShowError(val error: AppError) : SideEffect
    }

    /** Ekran holati; `me == null` — kesh hali bo'sh (masalan, birinchi ishga tushishda). */
    data class UiState(
        val me: User? = null,
        /** Ism tagidagi holat: ulangan bo'lsam "online", aks holda "Ulanmoqda…" / "Internet aloqasi yoʻq". */
        val connectionStatus: ConnectionStatus = ConnectionStatus.CONNECTED,
        val themeMode: ThemeMode = ThemeMode.LIGHT,
        val notificationsEnabled: Boolean = true,
        val language: AppLanguage = AppLanguage.UZ,
        /** Chiqish ketmoqda — tugma qayta bosilmasin, ichida progress. */
        val loggingOut: Boolean = false
    )

    // Logout'dan keyingi o'tish bu yerda yo'q: sessiya o'chishi bilan MainViewModel o'zi login ekraniga olib boradi.
    interface Directions {
        suspend fun back()
        suspend fun navigateToEditProfile()
    }
}
