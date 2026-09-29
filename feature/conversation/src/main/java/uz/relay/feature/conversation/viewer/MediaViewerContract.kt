package uz.relay.feature.conversation.viewer

import org.orbitmvi.orbit.OrbitContainerHost
import uz.relay.core.common.result.AppError
import uz.relay.domain.model.Message
import uz.relay.domain.model.MessageMedia

interface MediaViewerContract {

    interface ViewModel : OrbitContainerHost<UiState, UiState, SideEffect> {
        fun onEventDispatcher(intent: Intent)
    }

    sealed interface Intent {
        object OnBack : Intent
        /** Sahifa almashdi — video bo'lsa pleyerga yuklanadi, oldingi video to'xtaydi. */
        data class OnPageChange(val index: Int) : Intent
        data class OnSave(val item: ViewerItem) : Intent
    }

    sealed interface SideEffect {
        object Saved : SideEffect
        data class ShowError(val error: AppError) : SideEffect
    }

    data class UiState(
        /** Chatdagi rasm va videolar, eskidan yangiga (chapdan o'ngga surish vaqt bo'yicha). */
        val items: List<ViewerItem> = emptyList(),
        /** Ochilgan xabarning indeksi — ro'yxat birinchi marta kelganda bir marta hisoblanadi. */
        val initialIndex: Int? = null,
        val userNames: Map<String, String> = emptyMap(),
        val myUserId: String? = null,
        val isSaving: Boolean = false
    )

    interface Directions {
        suspend fun back()
    }
}

/** Ko'ruvchidagi bitta sahifa. */
data class ViewerItem(val message: Message, val media: MessageMedia) {
    val key: String get() = message.clientMessageId
}
