package uz.relay.feature.chats.search

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import org.orbitmvi.orbit.blockingIntent
import org.orbitmvi.orbit.viewmodel.orbitContainer
import uz.relay.core.common.result.AppResult
import uz.relay.core.common.result.isRetryable
import kotlinx.coroutines.flow.first
import uz.relay.domain.usecase.chat.ObserveChatsUseCase
import uz.relay.domain.usecase.chat.OpenDirectChatUseCase
import uz.relay.domain.usecase.user.SearchUsersUseCase
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

/**
 * Foydalanuvchi qidiruvi ViewModel'i (Orbit MVI). Username bo'yicha debounce qilingan server qidiruvi va
 * tanlangan odam bilan DIRECT chatni ochish (get-or-create) shu yerda.
 * Runtime argument yo'q — oddiy @Inject yetarli.
 */
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchUsers: SearchUsersUseCase,
    private val openDirectChat: OpenDirectChatUseCase,
    private val observeChats: ObserveChatsUseCase,
    private val directions: SearchContract.Directions
) : ViewModel(), SearchContract.ViewModel {

    override val container =
        orbitContainer<SearchContract.UiState, SearchContract.SideEffect>(SearchContract.UiState())

    // Oxirgi (debounce kutayotgan yoki ketayotgan) qidiruv — yangi harf kelganda bekor qilinadi.
    private var searchJob: Job? = null

    /** UI'dan kelgan Intent'larni tegishli amalga yo'naltiradi. */
    override fun onEventDispatcher(intent: SearchContract.Intent) {
        when (intent) {
            is SearchContract.Intent.OnQueryChange -> onQueryChange(intent.query)
            SearchContract.Intent.OnClear -> onQueryChange("")
            SearchContract.Intent.OnBack -> intent { directions.back() }
            is SearchContract.Intent.OnChatClick -> intent { directions.navigateToChat(intent.chatId) }
            is SearchContract.Intent.OnUserClick -> openChat(intent.user.id)
        }
    }

    /**
     * Matn sinxron yangilanadi (kursor sakramasin), qidiruv esa 300 ms "debounce" bilan (spec 3.6): har harfda
     * so'rov yuborilmaydi — sinf bitta IP'dan 300 so'rov/daqiqa limitini bo'lishadi. Yangi harf eski kutishni bekor qiladi.
     */
    private fun onQueryChange(query: String) {
        // blockingIntent — holat darhol (sinxron) yangilanadi; oddiy intent asinxron bo'lgani uchun
        // tez yozilganda TextField kursori sakrashi yoki harf yo'qolishi mumkin edi.
        blockingIntent { reduce { state.copy(query = query) } }
        searchJob?.cancel()
        searchJob = intent {
            val normalized = state.normalizedQuery
            if (normalized.isEmpty()) {
                reduce { state.copy(results = emptyList(), chatResults = emptyList(), searchedQuery = null, isSearching = false) }
                return@intent
            }
            // Chatlar lokal bazadan, nomi bo'yicha — tarmoqqa chiqmasdan, debounce'siz darhol ko'rinadi.
            val chats = observeChats().first().filter { it.title?.contains(normalized, ignoreCase = true) == true }
            reduce { state.copy(chatResults = chats) }
            // Debounce: shu vaqt ichida yangi harf kelsa, bu job bekor bo'ladi va so'rov ketmaydi.
            delay(DEBOUNCE_MS.milliseconds)
            reduce { state.copy(isSearching = true) }
            when (val result = searchUsers(normalized)) {
                is AppResult.Success -> reduce {
                    state.copy(results = result.data, searchedQuery = normalized, isSearching = false)
                }
                is AppResult.Error -> {
                    reduce { state.copy(isSearching = false) }
                    if (result.error.isRetryable) postSideEffect(SearchContract.SideEffect.ShowError(result.error))
                }
            }
        }
    }

    /** DIRECT chat — get-or-create: bu odam bilan chat bo'lsa o'sha ochiladi, bo'lmasa yangisi yaratiladi. */
    private fun openChat(userId: String) = intent {
        // Ikki marta bosishdan himoya: bitta chat ochilayotgan bo'lsa, yangisi boshlanmaydi.
        if (state.openingUserId != null) return@intent
        reduce { state.copy(openingUserId = userId) }
        when (val result = openDirectChat(userId)) {
            is AppResult.Success -> {
                reduce { state.copy(openingUserId = null) }
                directions.navigateToChat(result.data)
            }
            is AppResult.Error -> {
                reduce { state.copy(openingUserId = null) }
                postSideEffect(SearchContract.SideEffect.ShowError(result.error))
            }
        }
    }

    private companion object {
        const val DEBOUNCE_MS = 300L
    }
}
