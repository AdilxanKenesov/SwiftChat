package uz.relay.feature.chats.list

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.combine
import org.orbitmvi.orbit.viewmodel.orbitContainer
import uz.relay.core.common.result.AppResult
import uz.relay.core.common.result.isRetryable
import uz.relay.domain.usecase.chat.ObserveChatsUseCase
import uz.relay.domain.usecase.chat.ObserveSyncStatusUseCase
import uz.relay.domain.usecase.chat.RefreshChatsUseCase
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
    private val refreshChats: RefreshChatsUseCase,
    private val refreshMe: RefreshMeUseCase
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
        }
    }

    /**
     * Ekran faqat lokal bazani kuzatadi (offline-first): sync bazani yangilaydi, ro'yxat esa o'zi o'zgaradi.
     * `repeatOnSubscription` — ekran ko'rinmay turganda (fon) kuzatish to'xtaydi va resurs tejaladi,
     * qaytib kelganda yana davom etadi.
     */
    private fun observeData() = intent {
        repeatOnSubscription {
            combine(observeChats(), observeSyncStatus(), observeUserNames(), observeMe()) { chats, sync, names, me ->
                ChatsContract.UiState(
                    chats = chats,
                    userNames = names,
                    me = me,
                    isSyncing = sync.isSyncing,
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

    /** O'z profilim (app bar avatari uchun). Xato jimgina o'tkaziladi — avatar keyingi safar yuklanadi. */
    private fun loadMe() = intent {
        refreshMe()
    }
}
