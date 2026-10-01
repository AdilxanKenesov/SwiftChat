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
        /** Emoji reaksiya — hamma ekranida mening plitkamda bir lahza chiqadi. */
        data class OnSendReaction(val emoji: String) : Intent
        /** Qo'l ko'tarish / tushirish (navbat so'rash) — boshqalarda tepada "✋ Ism" ko'rinadi. */
        object OnToggleHand : Intent
        /** Tizim "ekranni yozib olish" ruxsatini berdi — [data] shu ruxsat natijasi (MediaProjection). */
        data class OnStartScreenShare(val data: android.content.Intent) : Intent
        object OnStopScreenShare : Intent
        /** Kamera orqa foni: yo'q, xiralashtirish yoki tayyor rasm. */
        data class OnSelectBackground(val background: CallBackground) : Intent
    }

    sealed interface SideEffect {
        data class ShowError(val error: AppError) : SideEffect
        /** 15 s ichida javob bo'lmadi. */
        object NoAnswer : SideEffect
    }

    data class UiState(
        /** Video qo'ng'iroqmi (aks holda faqat ovoz — kamera o'chiq, faqat mikrofon ruxsati so'raladi). */
        val isVideo: Boolean = true,
        /** Guruh video chati — jiringlash ekrani yo'q, darhol xona ko'rinadi. */
        val isGroup: Boolean = false,
        /** Stream client yo'q (API key yo'q yoki hali ulanmagan) — qo'ng'iroq ko'rsatilmaydi. */
        val unavailable: Boolean = false,
        /** Qo'l ko'targan boshqa ishtirokchilar: `userId → ism` (ko'targan tartibida). */
        val raisedHands: Map<String, String> = emptyMap(),
        /** Men qo'l ko'targanmanmi. */
        val myHandRaised: Boolean = false,
        /** Kameramning hozirgi orqa foni. */
        val background: CallBackground = CallBackground.NONE
    )

    interface Directions {
        suspend fun back()
    }
}

/**
 * Kamera orqa foni. Rasmli variantlar ilova ichidagi rasmlar (`res/drawable-nodpi/call_bg_*`): Stream filtri rasmni
 * faqat resurs id'si bilan qabul qiladi, shuning uchun galereyadan tanlash imkoni yo'q.
 */
enum class CallBackground { NONE, BLUR, BRAND, SUNSET, NIGHT, NATURE }
