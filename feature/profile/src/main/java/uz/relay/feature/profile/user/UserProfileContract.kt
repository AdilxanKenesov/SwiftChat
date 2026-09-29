package uz.relay.feature.profile.user

import org.orbitmvi.orbit.OrbitContainerHost
import uz.relay.core.common.result.AppError
import uz.relay.domain.model.ChatSummary
import uz.relay.domain.model.User

/**
 * Boshqa foydalanuvchi profili ekranining Orbit MVI shartnomasi. Shaxsiy chat sarlavhasidan, guruh a'zolari
 * ro'yxatidan yoki kontaktlardan ochiladi; bu yerdan shu odam bilan shaxsiy chatga o'tiladi yoki chat ovozsiz qilinadi.
 */
interface UserProfileContract {

    /** Screen ko'radigan ViewModel interfeysi: state/sideEffect va yagona [onEventDispatcher]. */
    interface ViewModel : OrbitContainerHost<UiState, UiState, SideEffect> {
        fun onEventDispatcher(intent: Intent)
    }

    /** Foydalanuvchi harakatlari: orqaga, "Xabar", ovozsiz qilish/qaytarish. */
    sealed interface Intent {
        object OnBack : Intent
        /** "Xabar" — shu odam bilan shaxsiy chat (bo'lmasa yaratiladi). */
        object OnMessage : Intent
        object OnToggleMute : Intent
    }

    /** Bir martalik xatolar — Screen ularni Snackbar'ga aylantiradi. */
    sealed interface SideEffect {
        data class ShowError(val error: AppError) : SideEffect
    }

    /** Ekran holati: foydalanuvchi (kesh), u bilan shaxsiy chat (bo'lsa) va band bayrog'i. */
    data class UiState(
        val user: User? = null,
        /** Shu odam bilan shaxsiy chat. Hali yozishilmagan bo'lsa `null`. */
        val chat: ChatSummary? = null,
        /** Biror amal bajarilmoqda (ikki marta bosishdan himoya). */
        val isBusy: Boolean = false
    ) {
        val muted: Boolean get() = chat?.muted == true
    }

    /** Profildan chiqish yo'llari — [UserProfileDirectionsImpl]da AppNavigator orqali amalga oshadi. */
    interface Directions {
        suspend fun back()
        /** Chat stekda bo'lsa (chat → profil) — unga qaytadi, aks holda ochadi. */
        suspend fun navigateToChat(chatId: String)
    }
}
