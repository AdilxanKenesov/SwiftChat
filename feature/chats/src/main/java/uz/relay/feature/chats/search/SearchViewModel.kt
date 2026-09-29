package uz.relay.feature.chats.search

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import org.orbitmvi.orbit.blockingIntent
import org.orbitmvi.orbit.viewmodel.orbitContainer
import uz.relay.core.common.result.AppResult
import uz.relay.core.common.result.isRetryable
import uz.relay.domain.usecase.chat.OpenDirectChatUseCase
import uz.relay.domain.usecase.user.SearchUsersUseCase
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchUsers: SearchUsersUseCase,
    private val openDirectChat: OpenDirectChatUseCase,
    private val directions: SearchContract.Directions
) : ViewModel(), SearchContract.ViewModel {

    override val container =
        orbitContainer<SearchContract.UiState, SearchContract.SideEffect>(SearchContract.UiState())

    private var searchJob: Job? = null

    override fun onEventDispatcher(intent: SearchContract.Intent) {
        when (intent) {
            is SearchContract.Intent.OnQueryChange -> onQueryChange(intent.query)
            SearchContract.Intent.OnClear -> onQueryChange("")
            SearchContract.Intent.OnBack -> intent { directions.back() }
            SearchContract.Intent.OnNewGroup -> intent { directions.navigateToGroupCreate() }
            is SearchContract.Intent.OnUserClick -> openChat(intent.user.id)
        }
    }

    /**
     * Matn sinxron yangilanadi (kursor sakramasin), qidiruv esa 300 ms "debounce" bilan (spec 3.6): har harfda
     * so'rov yuborilmaydi — sinf bitta IP'dan 300 so'rov/daqiqa limitini bo'lishadi. Yangi harf eski kutishni bekor qiladi.
     */
    private fun onQueryChange(query: String) {
        blockingIntent { reduce { state.copy(query = query) } }
        searchJob?.cancel()
        searchJob = intent {
            val normalized = state.normalizedQuery
            if (normalized.isEmpty()) {
                reduce { state.copy(results = emptyList(), searchedQuery = null, isSearching = false) }
                return@intent
            }
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
