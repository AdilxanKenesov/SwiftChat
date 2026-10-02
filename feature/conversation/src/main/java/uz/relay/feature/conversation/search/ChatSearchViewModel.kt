package uz.relay.feature.conversation.search

import androidx.lifecycle.ViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import org.orbitmvi.orbit.blockingIntent
import org.orbitmvi.orbit.viewmodel.orbitContainer
import uz.relay.domain.usecase.message.SearchMessagesUseCase
import uz.relay.domain.usecase.user.ObserveUserNamesUseCase
import kotlin.time.Duration.Companion.milliseconds

/**
 * Chat ichida qidiruv. API'da xabar qidiruvi yo'q — shuning uchun faqat qurilmadagi (yuklangan) xabarlar
 * ichidan qidiriladi; hali yuklanmagan eski xabarlar topilmaydi (ekranda shu haqida izoh bor).
 *
 * `chatId` AssistedInject orqali Nav3 kalitidan beriladi (ChatViewModel'dagi kabi). Natija bosilganda
 * Directions chatni shu xabarga scroll qilingan holda qayta ochadi.
 */
@HiltViewModel(assistedFactory = ChatSearchViewModel.Factory::class)
class ChatSearchViewModel @AssistedInject constructor(
    @Assisted private val chatId: String,
    private val searchMessages: SearchMessagesUseCase,
    private val observeUserNames: ObserveUserNamesUseCase,
    private val directions: ChatSearchContract.Directions
) : ViewModel(), ChatSearchContract.ViewModel {

    @AssistedFactory
    interface Factory {
        fun create(chatId: String): ChatSearchViewModel
    }

    override val container =
        orbitContainer<ChatSearchContract.UiState, ChatSearchContract.SideEffect>(ChatSearchContract.UiState()) {
            observeNames()
        }

    /** Oldingi (hali tugamagan) qidiruv — yangi harf kiritilganda bekor qilinadi, eski natija yangisini bosib ketmasin. */
    private var searchJob: Job? = null

    override fun onEventDispatcher(intent: ChatSearchContract.Intent) {
        when (intent) {
            ChatSearchContract.Intent.OnBack -> intent { directions.back() }
            is ChatSearchContract.Intent.OnQueryChange -> onQueryChange(intent.query)
            ChatSearchContract.Intent.OnClear -> onQueryChange("")
            is ChatSearchContract.Intent.OnResultClick -> intent {
                directions.openMessage(chatId, intent.message.clientMessageId)
            }
        }
    }

    /** Natijalarda yuboruvchi ismini ko'rsatish uchun ismlar keshi kuzatiladi. */
    private fun observeNames() = intent {
        repeatOnSubscription {
            observeUserNames().collect { names -> reduce { state.copy(userNames = names) } }
        }
    }

    /** Lokal qidiruv tez, lekin har harfda bazaga murojaat qilmaslik uchun qisqa kutish (200 ms). */
    private fun onQueryChange(query: String) {
        blockingIntent { reduce { state.copy(query = query) } }
        searchJob?.cancel()
        searchJob = intent {
            val trimmed = query.trim()
            if (trimmed.isEmpty()) {
                reduce { state.copy(results = emptyList(), searchedQuery = null) }
                return@intent
            }
            delay(DEBOUNCE_MS.milliseconds)
            val results = searchMessages(chatId, trimmed)
            reduce { state.copy(results = results, searchedQuery = trimmed) }
        }
    }

    private companion object {
        const val DEBOUNCE_MS = 200L
    }
}
