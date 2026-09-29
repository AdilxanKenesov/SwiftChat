package uz.relay.feature.group.create

import androidx.lifecycle.ViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import org.orbitmvi.orbit.blockingIntent
import org.orbitmvi.orbit.viewmodel.orbitContainer
import uz.relay.core.common.result.AppResult
import uz.relay.domain.model.User
import uz.relay.domain.usecase.group.AddMembersUseCase
import uz.relay.domain.usecase.group.CreateGroupUseCase
import uz.relay.domain.usecase.group.ObserveMembersUseCase
import uz.relay.domain.usecase.user.ObserveKnownUsersUseCase
import uz.relay.domain.usecase.user.SearchUsersUseCase
import kotlin.time.Duration.Companion.milliseconds

/**
 * "Yangi guruh" / "A'zo qo'shish" ekranining ViewModel'i (Orbit MVI).
 *
 * `addToChatId` Nav3 kalitidan keladi: `null` — yangi guruh, aks holda mavjud guruhga a'zo qo'shish.
 * U runtime argument bo'lgani uchun Hilt uni o'zi bilolmaydi — shu sababli `@AssistedInject` + [Factory]
 * ishlatiladi: qolgan bog'liqliklarni Hilt beradi, `addToChatId` ni esa ekran `creationCallback` orqali uzatadi.
 *
 * Nega Orbit: bitta o'zgarmas [GroupCreateContract.UiState], Intent'lar va SideEffect'lar — holat bir joyda,
 * ViewModel konfiguratsiya o'zgarishida (ekran aylanishi) saqlanib qoladi va unit-test qilish oson.
 */
@HiltViewModel(assistedFactory = GroupCreateViewModel.Factory::class)
class GroupCreateViewModel @AssistedInject constructor(
    @Assisted private val addToChatId: String?,
    private val observeKnownUsers: ObserveKnownUsersUseCase,
    private val observeMembers: ObserveMembersUseCase,
    private val searchUsers: SearchUsersUseCase,
    private val createGroup: CreateGroupUseCase,
    private val addMembers: AddMembersUseCase,
    private val directions: GroupCreateContract.Directions
) : ViewModel(), GroupCreateContract.ViewModel {

    /** Hilt generatsiya qiladigan factory: ekran `hiltViewModel(creationCallback = ...)` ichida chaqiradi. */
    @AssistedFactory
    interface Factory {
        fun create(addToChatId: String?): GroupCreateViewModel
    }

    // Container yaratilganda (birinchi obunada) nomzodlar ro'yxatini kuzatish boshlanadi.
    override val container =
        orbitContainer<GroupCreateContract.UiState, GroupCreateContract.SideEffect>(
            GroupCreateContract.UiState(addToChatId = addToChatId)
        ) {
            observeCandidates()
        }

    /** Server qidiruvining oxirgi natijasi — tanish odamlar bilan birlashtiriladi. */
    private val searchResults = MutableStateFlow<List<User>>(emptyList())
    private val queryFlow = MutableStateFlow("")
    private var searchJob: Job? = null

    /** UI'dan keladigan barcha Intent'lar uchun yagona kirish nuqtasi. */
    override fun onEventDispatcher(intent: GroupCreateContract.Intent) {
        when (intent) {
            GroupCreateContract.Intent.OnBack -> onBack()
            is GroupCreateContract.Intent.OnQueryChange -> onQueryChange(intent.query)
            is GroupCreateContract.Intent.OnToggle -> toggle(intent.user)
            GroupCreateContract.Intent.OnNext -> next()
            // Matn kiritish `blockingIntent` bilan: holat darhol, sinxron yangilanadi — aks holda tez yozganda
            // TextField kursori sakrashi yoki harf yo'qolishi mumkin.
            is GroupCreateContract.Intent.OnTitleChange -> blockingIntent {
                reduce { state.copy(title = intent.title.take(TITLE_MAX)) }
            }
            GroupCreateContract.Intent.OnCreate -> create()
        }
    }

    /**
     * Tanlash ro'yxati = tanish odamlar (so'rov bo'yicha lokal filtr) + server qidiruvi natijalari. A'zo qo'shish
     * rejimida guruhdagilar ro'yxatdan chiqarib tashlanadi — ularni qayta qo'shib bo'lmaydi.
     */
    private fun observeCandidates() = intent {
        val existingIds = addToChatId?.let { chatId -> observeMembers(chatId).first().mapTo(HashSet()) { it.userId } }.orEmpty()
        // `repeatOnSubscription`: ekran ko'rinib turgandagina kuzatadi, fonda keraksiz ishlamaydi.
        repeatOnSubscription {
            combine(observeKnownUsers(), searchResults, queryFlow) { known, found, query ->
                val q = query.trim().removePrefix("@")
                val localMatches = if (q.isEmpty()) known else known.filter { user ->
                    user.displayName.contains(q, ignoreCase = true) || user.username.orEmpty().startsWith(q, ignoreCase = true)
                }
                (localMatches + found).distinctBy { it.id }.filterNot { it.id in existingIds }
            }.collect { candidates -> reduce { state.copy(candidates = candidates) } }
        }
    }

    /**
     * Qidiruv 300 ms debounce bilan (spec 3.6) — har harfda serverga so'rov yuborilmaydi.
     * Oldingi qidiruv `Job` bekor qilinadi, shuning uchun eskirgan javob yangi natijani ustidan yozib yubormaydi.
     */
    private fun onQueryChange(query: String) {
        blockingIntent { reduce { state.copy(query = query) } }
        queryFlow.value = query
        searchJob?.cancel()
        searchJob = intent {
            val normalized = query.trim().removePrefix("@")
            if (normalized.isEmpty()) {
                searchResults.value = emptyList()
                return@intent
            }
            delay(DEBOUNCE_MS.milliseconds)
            when (val result = searchUsers(normalized)) {
                is AppResult.Success -> searchResults.value = result.data
                // Qidiruv xatosi ro'yxatni buzmaydi — lokal natijalar qoladi.
                is AppResult.Error -> Unit
            }
        }
    }

    /** Odamni tanlash / tanlovdan chiqarish (ro'yxat qatori yoki chip bosilganda). */
    private fun toggle(user: User) = intent {
        reduce {
            val selected = if (user.id in state.selectedIds) state.selected.filterNot { it.id == user.id } else state.selected + user
            state.copy(selected = selected)
        }
    }

    private fun onBack() = intent {
        // 2-qadamdan orqaga — tanlash qadamiga (tanlanganlar saqlanadi).
        if (state.step == GroupCreateContract.Step.NAME) {
            reduce { state.copy(step = GroupCreateContract.Step.PICK) }
        } else {
            directions.back()
        }
    }

    /** 1-qadamdagi FAB: yangi guruhda — nom qadamiga o'tadi, a'zo qo'shish rejimida — serverga yuborib, orqaga qaytadi. */
    private fun next() = intent {
        if (!state.canProceed) return@intent
        val chatId = state.addToChatId
        if (chatId == null) {
            reduce { state.copy(step = GroupCreateContract.Step.NAME) }
            return@intent
        }
        reduce { state.copy(isSubmitting = true) }
        when (val result = addMembers(chatId, state.selected.map { it.id })) {
            is AppResult.Success -> {
                reduce { state.copy(isSubmitting = false) }
                directions.back()
            }
            is AppResult.Error -> {
                reduce { state.copy(isSubmitting = false) }
                postSideEffect(GroupCreateContract.SideEffect.ShowError(result.error))
            }
        }
    }

    /** Guruhni yaratadi va muvaffaqiyatda yangi guruh chatini ochadi; xatoda snackbar ko'rsatiladi. */
    private fun create() = intent {
        if (!state.canCreate) return@intent
        reduce { state.copy(isSubmitting = true) }
        when (val result = createGroup(state.title, state.selected.map { it.id })) {
            is AppResult.Success -> {
                reduce { state.copy(isSubmitting = false) }
                directions.openCreatedChat(result.data)
            }
            is AppResult.Error -> {
                reduce { state.copy(isSubmitting = false) }
                postSideEffect(GroupCreateContract.SideEffect.ShowError(result.error))
            }
        }
    }

    private companion object {
        const val DEBOUNCE_MS = 300L
        const val TITLE_MAX = 128
    }
}
