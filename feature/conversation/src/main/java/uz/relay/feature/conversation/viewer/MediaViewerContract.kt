package uz.relay.feature.conversation.viewer

import org.orbitmvi.orbit.OrbitContainerHost
import uz.relay.core.common.result.AppError
import uz.relay.domain.model.Message
import uz.relay.domain.model.MessageMedia

/**
 * To'liq ekranli media ko'ruvchining Orbit MVI shartnomasi. Chat'da rasm/video bubble bosilganda ochiladi
 * (ChatContract.Intent.OnMediaClick → MediaViewerKey) va chatdagi barcha rasm/videolar orasida surib ko'rish imkonini beradi.
 * "Saqlandi"/xato Snackbar'lari bir martalik bo'lgani uchun SideEffect orqali keladi.
 */
interface MediaViewerContract {

    /** Screen ko'radigan ViewModel interfeysi. Pleyer esa alohida xususiyat sifatida ViewModel'ning o'zida. */
    interface ViewModel : OrbitContainerHost<UiState, UiState, SideEffect> {
        fun onEventDispatcher(intent: Intent)
    }

    /** Foydalanuvchi harakatlari: yopish, sahifa almashishi, galereyaga saqlash. */
    sealed interface Intent {
        object OnBack : Intent
        /** Sahifa almashdi — video bo'lsa pleyerga yuklanadi, oldingi video to'xtaydi. */
        data class OnPageChange(val index: Int) : Intent
        data class OnSave(val item: ViewerItem) : Intent
    }

    /** Bir martalik natijalar — Screen ularni Snackbar'ga aylantiradi. */
    sealed interface SideEffect {
        object Saved : SideEffect
        data class ShowError(val error: AppError) : SideEffect
    }

    /** Ko'ruvchi holati: sahifalar ro'yxati, boshlang'ich sahifa, ismlar va saqlash jarayoni. */
    data class UiState(
        /** Chatdagi rasm va videolar, eskidan yangiga (chapdan o'ngga surish vaqt bo'yicha). */
        val items: List<ViewerItem> = emptyList(),
        /** Ochilgan xabarning indeksi — ro'yxat birinchi marta kelganda bir marta hisoblanadi. */
        val initialIndex: Int? = null,
        val userNames: Map<String, String> = emptyMap(),
        val myUserId: String? = null
    )

    /** Ko'ruvchidan faqat orqaga (chatga) qaytiladi. */
    interface Directions {
        suspend fun back()
    }
}

/** Ko'ruvchidagi bitta sahifa: xabar (yuboruvchi, vaqt, izoh uchun) va uning media'si. [key] — pager uchun barqaror kalit. */
data class ViewerItem(val message: Message, val media: MessageMedia) {
    val key: String get() = message.clientMessageId
}
