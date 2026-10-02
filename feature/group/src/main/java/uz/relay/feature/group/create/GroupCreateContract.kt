package uz.relay.feature.group.create

import org.orbitmvi.orbit.OrbitContainerHost
import uz.relay.core.common.result.AppError
import uz.relay.domain.model.User

/**
 * "Yangi guruh" / "A'zo qo'shish" ekranining Orbit MVI shartnomasi (contract).
 *
 * Nega bitta interfeys ichida: ekranga tegishli hamma narsa — [ViewModel], [Intent], [UiState], [SideEffect]
 * va [Directions] — bir joyda turadi, o'qish va o'zgartirish oson (Uzum uslubi). UI faqat shu shartnomani biladi,
 * ViewModel realizatsiyasini emas.
 *
 * Ekran ikki rejimda ishlaydi: chatlar qidiruvidan "Yangi guruh" orqali ochilsa — 2 qadamli yaratish
 * (odam tanlash → nom berish); guruh ma'lumotlari ekranidan "Qo'shish" orqali ochilsa — faqat tanlash qadami.
 */
interface GroupCreateContract {

    /** UI ViewModel bilan faqat shu interfeys orqali gaplashadi: holat oqimi + bitta kirish nuqtasi [onEventDispatcher]. */
    interface ViewModel : OrbitContainerHost<UiState, UiState, SideEffect> {
        fun onEventDispatcher(intent: Intent)
    }

    /** Foydalanuvchi harakatlari (bosish, matn kiritish). Sealed — `when` barcha holatlarni tekshirishga majbur qiladi. */
    sealed interface Intent {
        object OnBack : Intent
        data class OnQueryChange(val query: String) : Intent
        data class OnToggle(val user: User) : Intent
        /** 1-qadamdagi FAB: yangi guruhda — 2-qadamga, a'zo qo'shish rejimida — darhol qo'shish. */
        object OnNext : Intent
        data class OnTitleChange(val title: String) : Intent
        object OnCreate : Intent
    }

    /** Bir martalik hodisalar (snackbar) — holatda saqlanmaydi, aks holda ekran aylanganda qayta ko'rinardi. */
    sealed interface SideEffect {
        data class ShowError(val error: AppError) : SideEffect
    }

    /** Yaratish qadamlari: PICK — odamlarni tanlash, NAME — guruh nomini kiritish. */
    enum class Step { PICK, NAME }

    /** Ekranning yagona o'zgarmas (immutable) holati — UI faqat shundan chiziladi. */
    data class UiState(
        /** `null` — yangi guruh; aks holda shu guruhga a'zo qo'shilmoqda. */
        val addToChatId: String? = null,
        val step: Step = Step.PICK,
        val query: String = "",
        /** Tanlash ro'yxati: tanish odamlar (kesh) + server qidiruvi natijalari, takrorlarsiz. */
        val candidates: List<User> = emptyList(),
        /** Tanlanganlar — tanlash tartibida (chip'lar shu tartibda). */
        val selected: List<User> = emptyList(),
        val title: String = "",
        val isSubmitting: Boolean = false
    ) {
        val isAddMode: Boolean get() = addToChatId != null
        val selectedIds: Set<String> get() = selected.mapTo(HashSet()) { it.id }
        val canProceed: Boolean get() = selected.isNotEmpty() && !isSubmitting
        /** Server qoidasi: nom 1..128 belgi. */
        val canCreate: Boolean get() = title.trim().length in 1..128 && selected.isNotEmpty() && !isSubmitting
    }

    /**
     * Ekrandan chiqish yo'llari. ViewModel navigatsiya tafsilotlarini (qaysi kalit, stack qanday tozalanadi) bilmaydi —
     * faqat "orqaga" yoki "yaratilgan chatni och" deydi; testda esa buni soxta Directions bilan tekshirish oson.
     */
    interface Directions {
        suspend fun back()
        /** Yangi guruh yaratildi: chatlar ro'yxatiga qaytib, shu guruhni ochadi. */
        suspend fun openCreatedChat(chatId: String)
    }
}
