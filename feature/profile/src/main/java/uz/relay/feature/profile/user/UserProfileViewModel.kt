package uz.relay.feature.profile.user

import androidx.lifecycle.ViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.combine
import org.orbitmvi.orbit.viewmodel.orbitContainer
import uz.relay.core.common.result.AppResult
import uz.relay.domain.usecase.chat.ObserveDirectChatUseCase
import uz.relay.domain.usecase.chat.OpenDirectChatUseCase
import uz.relay.domain.usecase.chat.SetChatMutedUseCase
import uz.relay.domain.usecase.contact.AddContactUseCase
import uz.relay.domain.usecase.contact.ObserveContactIdsUseCase
import uz.relay.domain.usecase.contact.RemoveContactUseCase
import uz.relay.domain.usecase.user.ObserveUserUseCase
import uz.relay.domain.usecase.user.RefreshUserUseCase

/**
 * Foydalanuvchi profili ViewModel'i. `userId` Nav3 kalitidan AssistedInject orqali keladi (runtime qiymat),
 * use case'lar esa Hilt'dan. Ma'lumot offline-first: avval lokal kesh ko'rsatiladi, fonda serverdan yangilanadi.
 */
@HiltViewModel(assistedFactory = UserProfileViewModel.Factory::class)
class UserProfileViewModel @AssistedInject constructor(
    @Assisted private val userId: String,
    private val observeUser: ObserveUserUseCase,
    private val refreshUser: RefreshUserUseCase,
    private val observeDirectChat: ObserveDirectChatUseCase,
    private val openDirectChat: OpenDirectChatUseCase,
    private val setChatMuted: SetChatMutedUseCase,
    private val observeContactIds: ObserveContactIdsUseCase,
    private val addContact: AddContactUseCase,
    private val removeContact: RemoveContactUseCase,
    private val directions: UserProfileContract.Directions
) : ViewModel(), UserProfileContract.ViewModel {

    /** UserProfileScreen shu factory orqali ViewModel'ni `userId` bilan yaratadi. */
    @AssistedFactory
    interface Factory {
        fun create(userId: String): UserProfileViewModel
    }

    override val container =
        orbitContainer<UserProfileContract.UiState, UserProfileContract.SideEffect>(UserProfileContract.UiState()) {
            observeData()
            refresh()
        }

    /** Screen'dan keladigan barcha Intent'lar uchun yagona kirish nuqtasi. */
    override fun onEventDispatcher(intent: UserProfileContract.Intent) {
        when (intent) {
            UserProfileContract.Intent.OnBack -> intent { directions.back() }
            UserProfileContract.Intent.OnMessage -> openChat()
            UserProfileContract.Intent.OnToggleMute -> toggleMute()
            UserProfileContract.Intent.OnToggleContact -> toggleContact()
        }
    }

    /**
     * Profil va chat lokal bazadan kuzatiladi: online holat socket'dagi `presence` bilan, ovozsiz belgisi
     * esa boshqa qurilmadan kelgan `chat` update bilan ham o'zi yangilanadi.
     */
    private fun observeData() = intent {
        repeatOnSubscription {
            combine(observeUser(userId), observeDirectChat(userId), observeContactIds()) { user, chat, contacts ->
                Triple(user, chat, userId in contacts)
            }.collect { (user, chat, isContact) -> reduce { state.copy(user = user, chat = chat, isContact = isContact) } }
        }
    }

    /** Keshda bo'lsa ham yangilanadi. Xato faqat ko'rsatadigan hech narsa bo'lmasa (kesh bo'sh) chiqariladi. */
    private fun refresh() = intent {
        val result = refreshUser(userId)
        if (result is AppResult.Error && state.user == null) {
            postSideEffect(UserProfileContract.SideEffect.ShowError(result.error))
        }
    }

    /** Shaxsiy chat bo'lsa — darhol ochiladi, bo'lmasa serverda yaratilib (idempotent), keyin ochiladi. */
    private fun openChat() = intent {
        val chatId = state.chat?.id ?: when (val result = openDirectChat(userId)) {
            is AppResult.Success -> result.data
            is AppResult.Error -> {
                postSideEffect(UserProfileContract.SideEffect.ShowError(result.error))
                return@intent
            }
        }
        directions.navigateToChat(chatId)
    }

    /**
     * Ovozsiz qilish chat sozlamasi (`PUT /v1/chats/{id}/settings`). Hali chat bo'lmasa, avval yaratiladi —
     * server DIRECT chatni idempotent yaratadi, ya'ni bir juftlik uchun doim bitta chat.
     */
    private fun toggleMute() = intent {
        if (state.isBusy) return@intent
        reduce { state.copy(isBusy = true) }
        val muted = state.muted
        val chatId = state.chat?.id ?: when (val created = openDirectChat(userId)) {
            is AppResult.Success -> created.data
            is AppResult.Error -> {
                reduce { state.copy(isBusy = false) }
                postSideEffect(UserProfileContract.SideEffect.ShowError(created.error))
                return@intent
            }
        }
        val result = setChatMuted(chatId, !muted)
        reduce { state.copy(isBusy = false) }
        if (result is AppResult.Error) postSideEffect(UserProfileContract.SideEffect.ShowError(result.error))
    }

    /** Kontaktlar faqat shu qurilmada. O'chirish tasdiqni UI so'raydi (dialog), bu yerga tasdiqlangan amal keladi. */
    private fun toggleContact() = intent {
        if (state.isBusy) return@intent
        if (state.isContact) {
            removeContact(userId)
            return@intent
        }
        reduce { state.copy(isBusy = true) }
        val result = addContact(userId)
        reduce { state.copy(isBusy = false) }
        when (result) {
            is AppResult.Success -> postSideEffect(UserProfileContract.SideEffect.ContactAdded)
            is AppResult.Error -> postSideEffect(UserProfileContract.SideEffect.ShowError(result.error))
        }
    }
}
