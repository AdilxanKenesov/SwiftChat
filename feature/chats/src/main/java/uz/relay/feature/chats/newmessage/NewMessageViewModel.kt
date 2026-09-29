package uz.relay.feature.chats.newmessage

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import org.orbitmvi.orbit.viewmodel.orbitContainer
import uz.relay.core.common.result.AppResult
import uz.relay.domain.usecase.chat.OpenDirectChatUseCase
import uz.relay.domain.usecase.contact.ObserveContactsUseCase
import uz.relay.domain.usecase.contact.RemoveContactUseCase
import javax.inject.Inject

@HiltViewModel
class NewMessageViewModel @Inject constructor(
    private val observeContacts: ObserveContactsUseCase,
    private val removeContact: RemoveContactUseCase,
    private val openDirectChat: OpenDirectChatUseCase,
    private val directions: NewMessageContract.Directions
) : ViewModel(), NewMessageContract.ViewModel {

    override val container =
        orbitContainer<NewMessageContract.UiState, NewMessageContract.SideEffect>(NewMessageContract.UiState()) {
            observeData()
        }

    override fun onEventDispatcher(intent: NewMessageContract.Intent) {
        when (intent) {
            NewMessageContract.Intent.OnBack -> intent { directions.back() }
            NewMessageContract.Intent.OnNewGroup -> intent { directions.navigateToGroupCreate() }
            NewMessageContract.Intent.OnNewContact -> intent { directions.navigateToAddContact() }
            is NewMessageContract.Intent.OnContactClick -> openChat(intent.user.id)
            is NewMessageContract.Intent.OnRemoveContact -> intent { removeContact(intent.user.id) }
        }
    }

    /** Kontaktlar bazadan kuzatiladi: qo'shilsa/o'chirilsa yoki kimdir online bo'lsa ro'yxat o'zi yangilanadi. */
    private fun observeData() = intent {
        repeatOnSubscription {
            observeContacts().collect { contacts -> reduce { state.copy(contacts = contacts, isLoaded = true) } }
        }
    }

    private fun openChat(userId: String) = intent {
        if (state.openingUserId != null) return@intent
        reduce { state.copy(openingUserId = userId) }
        val result = openDirectChat(userId)
        reduce { state.copy(openingUserId = null) }
        when (result) {
            is AppResult.Success -> directions.navigateToChat(result.data)
            is AppResult.Error -> postSideEffect(NewMessageContract.SideEffect.ShowError(result.error))
        }
    }
}
