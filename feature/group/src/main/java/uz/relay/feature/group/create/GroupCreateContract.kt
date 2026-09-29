package uz.relay.feature.group.create

import org.orbitmvi.orbit.OrbitContainerHost
import uz.relay.core.common.result.AppError
import uz.relay.domain.model.User

interface GroupCreateContract {

    interface ViewModel : OrbitContainerHost<UiState, UiState, SideEffect> {
        fun onEventDispatcher(intent: Intent)
    }

    sealed interface Intent {
        object OnBack : Intent
        data class OnQueryChange(val query: String) : Intent
        data class OnToggle(val user: User) : Intent
        /** 1-qadamdagi FAB: yangi guruhda — 2-qadamga, a'zo qo'shish rejimida — darhol qo'shish. */
        object OnNext : Intent
        data class OnTitleChange(val title: String) : Intent
        object OnCreate : Intent
    }

    sealed interface SideEffect {
        data class ShowError(val error: AppError) : SideEffect
    }

    enum class Step { PICK, NAME }

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

    interface Directions {
        suspend fun back()
        /** Yangi guruh yaratildi: chatlar ro'yxatiga qaytib, shu guruhni ochadi. */
        suspend fun openCreatedChat(chatId: String)
    }
}
