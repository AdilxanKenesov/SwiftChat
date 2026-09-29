package uz.relay.feature.chats.list

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.combine
import org.orbitmvi.orbit.viewmodel.orbitContainer
import uz.relay.core.common.result.AppResult
import uz.relay.core.common.result.isRetryable
import uz.relay.domain.usecase.chat.ObserveChatsUseCase
import uz.relay.domain.usecase.chat.ObserveConnectionStatusUseCase
import uz.relay.domain.usecase.chat.ObserveTypingUseCase
import uz.relay.domain.usecase.chat.ObserveSyncStatusUseCase
import uz.relay.domain.usecase.chat.RefreshChatsUseCase
import uz.relay.domain.usecase.chat.SetChatMutedUseCase
import uz.relay.domain.usecase.user.ObserveMeUseCase
import uz.relay.domain.usecase.user.ObserveUserNamesUseCase
import uz.relay.domain.usecase.user.RefreshMeUseCase
import javax.inject.Inject

@HiltViewModel
class ChatsViewModel @Inject constructor(
    private val observeChats: ObserveChatsUseCase,
    private val observeSyncStatus: ObserveSyncStatusUseCase,
    private val observeUserNames: ObserveUserNamesUseCase,
    private val observeMe: ObserveMeUseCase,
    private val observeConnectionStatus: ObserveConnectionStatusUseCase,
    private val observeTyping: ObserveTypingUseCase,
    private val refreshChats: RefreshChatsUseCase,
    private val refreshMe: RefreshMeUseCase,
    private val setChatMuted: SetChatMutedUseCase,
    private val directions: ChatsContract.Directions
) : ViewModel(), ChatsContract.ViewModel {

    override val container =
        orbitContainer<ChatsContract.UiState, ChatsContract.SideEffect>(ChatsContract.UiState()) {
            observeData()
            sync()
            loadMe()
        }

    override fun onEventDispatcher(intent: ChatsContract.Intent) {
        when (intent) {
            ChatsContract.Intent.OnRetrySync -> sync()
            is ChatsContract.Intent.OnChatClick -> intent { directions.navigateToChat(intent.chatId) }
            ChatsContract.Intent.OnSearchClick -> intent { directions.navigateToSearch() }
            ChatsContract.Intent.OnMyProfileClick -> intent { directions.navigateToMyProfile() }
            is ChatsContract.Intent.OnMute -> mute { setChatMuted(intent.chatId, intent.duration) }
            is ChatsContract.Intent.OnUnmute -> mute { setChatMuted(intent.chatId, muted = false) }
        }
    }

    /**
     * Ekran faqat lokal bazani kuzatadi (offline-first): sync va WebSocket bazani yangilaydi, ro'yxat esa
     * o'zi o'zgaradi.
     * `repeatOnSubscription` — ekran ko'rinmay turganda (fon) kuzatish to'xtaydi va resurs tejaladi,
     * qaytib kelganda yana davom etadi.
     */
    private fun observeData() = intent {
        repeatOnSubscription {
            // combine ko'pi bilan 5 ta oqimni tiplangan holda qabul qiladi — shuning uchun ikki bosqichda.
            val data = combine(observeChats(), observeUserNames(), observeMe()) { chats, names, me ->
                Triple(chats, names, me)
            }
            val status = combine(observeSyncStatus(), observeConnectionStatus(), observeTyping()) { sync, connection, typing ->
                Triple(sync, connection, typing)
            }
            combine(data, status) { (chats, names, me), (sync, connection, typing) ->
                ChatsContract.UiState(
                    chats = chats,
                    userNames = names,
                    me = me,
                    connectionStatus = connection,
                    // O'zimning boshqa qurilmamdagi yozishim ro'yxatda ko'rinmasin.
                    typing = typing.mapValues { (_, users) -> users - me?.id.orEmpty() }.filterValues { it.isNotEmpty() },
                    isBootstrapped = sync.isBootstrapped
                )
            }.collect { newState -> reduce { newState } }
        }
    }

    /**
     * Server bilan sinxronlash. Faqat vaqtinchalik xato (internet yo'q, 5xx, 429) ko'rsatiladi — ular uchun
     * "Qayta urinish" ma'noli. Sessiya tugagan bo'lsa (401), login'ga qaytishni MainViewModel o'zi qiladi.
     */
    private fun sync() = intent {
        when (val result = refreshChats()) {
            is AppResult.Success -> Unit
            is AppResult.Error ->
                if (result.error.isRetryable) postSideEffect(ChatsContract.SideEffect.ShowError(result.error))
        }
    }

    /** Natija (belgi, badge rangi) ro'yxatga bazadan keladi — server javobi darhol yoziladi. Bu yerda faqat xato. */
    private fun mute(block: suspend () -> AppResult<Unit>) = intent {
        val result = block()
        if (result is AppResult.Error) postSideEffect(ChatsContract.SideEffect.ShowActionError(result.error))
    }

    /** O'z profilim (app bar avatari uchun). Xato jimgina o'tkaziladi — avatar keyingi safar yuklanadi. */
    private fun loadMe() = intent {
        refreshMe()
    }
}
