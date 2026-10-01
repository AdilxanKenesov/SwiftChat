package uz.relay.feature.calls.call

import io.getstream.video.android.core.call.state.CallAction
import org.orbitmvi.orbit.OrbitContainerHost
import uz.relay.core.common.result.AppError

/**
 * Qo'ng'iroq ekrani (Stream Video). Ekranning o'zi Stream'ning tayyor komponentlari: `RingingCallContent` jiringlash
 * holatiga qarab kiruvchi / chiquvchi / faol ko'rinishni o'zi tanlaydi. Bu yerdagi mantiq — tugmalar amallari
 * (qabul qilish, rad etish, bekor qilish, chiqish) va qo'ng'iroq tugaganda ekranni yopish.
 */
interface CallContract {

    interface ViewModel : OrbitContainerHost<UiState, UiState, SideEffect> {
        fun onEventDispatcher(intent: Intent)
    }

    sealed interface Intent {
        /** Stream UI'dagi tugma (mikrofon, kamera, qabul qilish, rad etish, chiqish ...). */
        data class OnCallAction(val action: CallAction) : Intent
        /** Tizim "orqaga" tugmasi — holatga qarab rad etish / bekor qilish / chiqish. */
        object OnBack : Intent
        /** Rad etildi yoki javob berilmadi — ekran yopiladi. */
        object OnFinished : Intent
    }

    sealed interface SideEffect {
        data class ShowError(val error: AppError) : SideEffect
        /** 15 s ichida javob bo'lmadi. */
        object NoAnswer : SideEffect
    }

    data class UiState(
        /** Video qo'ng'iroqmi (aks holda faqat ovoz — kamera o'chiq, faqat mikrofon ruxsati so'raladi). */
        val isVideo: Boolean = true,
        /** Stream client yo'q (API key yo'q yoki hali ulanmagan) — qo'ng'iroq ko'rsatilmaydi. */
        val unavailable: Boolean = false
    )

    interface Directions {
        suspend fun back()
    }
}
