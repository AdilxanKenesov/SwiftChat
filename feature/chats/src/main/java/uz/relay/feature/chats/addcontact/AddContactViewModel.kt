package uz.relay.feature.chats.addcontact

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import org.orbitmvi.orbit.blockingIntent
import org.orbitmvi.orbit.viewmodel.orbitContainer
import uz.relay.core.common.result.AppResult
import uz.relay.core.common.result.isRetryable
import uz.relay.domain.model.User
import uz.relay.domain.usecase.contact.AddContactUseCase
import uz.relay.domain.usecase.contact.ObserveContactIdsUseCase
import uz.relay.domain.usecase.user.SearchUsersUseCase
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

@HiltViewModel
class AddContactViewModel @Inject constructor(
    private val searchUsers: SearchUsersUseCase,
    private val addContact: AddContactUseCase,
    private val observeContactIds: ObserveContactIdsUseCase,
    private val directions: AddContactContract.Directions
) : ViewModel(), AddContactContract.ViewModel {

    override val container =
        orbitContainer<AddContactContract.UiState, AddContactContract.SideEffect>(AddContactContract.UiState()) {
            observeContacts()
        }

    // Oxirgi (debounce kutayotgan yoki ketayotgan) qidiruv — yangi harf kelganda bekor qilinadi.
    private var searchJob: Job? = null

    override fun onEventDispatcher(intent: AddContactContract.Intent) {
        when (intent) {
            is AddContactContract.Intent.OnQueryChange -> onQueryChange(intent.query)
            AddContactContract.Intent.OnClear -> onQueryChange("")
            AddContactContract.Intent.OnBack -> intent { directions.back() }
            is AddContactContract.Intent.OnAdd -> add(intent.user)
            is AddContactContract.Intent.OnUserClick -> intent { directions.navigateToUserProfile(intent.user.id) }
        }
    }

    /** Qo'shilgan kontaktlar id'lari — natijadagi ✓ belgisi shundan (boshqa ekranda o'zgarsa ham to'g'ri). */
    private fun observeContacts() = intent {
        repeatOnSubscription {
            observeContactIds().collect { ids -> reduce { state.copy(contactIds = ids) } }
        }
    }

    /** Qidiruv ekranidagi bilan bir xil: sinxron matn (`blockingIntent`) + 300 ms debounce + eski so'rovni bekor qilish. */
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
                is AppResult.Success -> reduce { state.copy(results = result.data, searchedQuery = normalized, isSearching = false) }
                is AppResult.Error -> {
                    reduce { state.copy(isSearching = false) }
                    if (result.error.isRetryable) postSideEffect(AddContactContract.SideEffect.ShowError(result.error))
                }
            }
        }
    }

    private fun add(user: User) = intent {
        if (state.addingUserId != null || user.id in state.contactIds) return@intent
        reduce { state.copy(addingUserId = user.id) }
        val result = addContact(user.id)
        reduce { state.copy(addingUserId = null) }
        when (result) {
            is AppResult.Success -> postSideEffect(AddContactContract.SideEffect.Added(user.displayName))
            is AppResult.Error -> postSideEffect(AddContactContract.SideEffect.ShowError(result.error))
        }
    }

    private companion object {
        const val DEBOUNCE_MS = 300L
    }
}
