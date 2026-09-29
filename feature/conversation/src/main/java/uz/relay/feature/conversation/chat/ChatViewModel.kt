package uz.relay.feature.conversation.chat

import androidx.lifecycle.ViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.combine
import org.orbitmvi.orbit.blockingIntent
import org.orbitmvi.orbit.syntax.Syntax
import org.orbitmvi.orbit.viewmodel.orbitContainer
import uz.relay.core.common.result.AppError
import uz.relay.core.common.result.AppResult
import uz.relay.domain.model.ChatSummary
import uz.relay.domain.model.ChatType
import uz.relay.domain.usecase.chat.ObserveChatUseCase
import uz.relay.domain.usecase.chat.ObserveTypingUseCase
import uz.relay.domain.usecase.message.DeleteMessageUseCase
import uz.relay.domain.usecase.message.EditMessageUseCase
import uz.relay.domain.usecase.message.LoadLatestMessagesUseCase
import uz.relay.domain.usecase.message.LoadOlderMessagesUseCase
import uz.relay.domain.usecase.message.MarkChatReadUseCase
import uz.relay.domain.usecase.message.ObserveMessagesUseCase
import uz.relay.domain.usecase.message.RetryMessageUseCase
import uz.relay.domain.usecase.message.SendTextMessageUseCase
import uz.relay.domain.usecase.message.SendTypingUseCase
import uz.relay.domain.usecase.user.ObserveMeUseCase
import uz.relay.domain.usecase.user.ObserveUserNamesUseCase

/** `chatId` Nav3 kalitidan keladi (runtime qiymat), use-case'lar Hilt'dan — AssistedInject ularni birlashtiradi. */
@HiltViewModel(assistedFactory = ChatViewModel.Factory::class)
class ChatViewModel @AssistedInject constructor(
    @Assisted private val chatId: String,
    private val observeMessages: ObserveMessagesUseCase,
    private val observeChat: ObserveChatUseCase,
    private val observeUserNames: ObserveUserNamesUseCase,
    private val observeMe: ObserveMeUseCase,
    private val observeTyping: ObserveTypingUseCase,
    private val loadLatestMessages: LoadLatestMessagesUseCase,
    private val loadOlderMessages: LoadOlderMessagesUseCase,
    private val sendTextMessage: SendTextMessageUseCase,
    private val retryMessage: RetryMessageUseCase,
    private val editMessage: EditMessageUseCase,
    private val deleteMessage: DeleteMessageUseCase,
    private val sendTyping: SendTypingUseCase,
    private val markChatRead: MarkChatReadUseCase,
    private val directions: ChatContract.Directions
) : ViewModel(), ChatContract.ViewModel {

    @AssistedFactory
    interface Factory {
        fun create(chatId: String): ChatViewModel
    }

    override val container =
        orbitContainer<ChatContract.UiState, ChatContract.SideEffect>(ChatContract.UiState()) {
            observeData()
            loadLatest()
        }

    /** Serverga oxirgi yuborilgan o'qish kursori — bir xil qiymatni qayta-qayta yubormaslik uchun. */
    private var lastReadSeq = -1L

    override fun onEventDispatcher(intent: ChatContract.Intent) {
        when (intent) {
            ChatContract.Intent.OnBack -> intent { directions.back() }
            is ChatContract.Intent.OnTextChange -> onTextChange(intent.text)
            ChatContract.Intent.OnSend -> send()
            is ChatContract.Intent.OnReply -> intent {
                reduce { state.copy(composerMode = ChatContract.ComposerMode.Reply(intent.message)) }
            }
            is ChatContract.Intent.OnEdit -> blockingIntent {
                // Tahrir boshlanganda matn maydoni asl matn bilan to'ldiriladi.
                reduce {
                    state.copy(
                        composerMode = ChatContract.ComposerMode.Edit(intent.message),
                        composerText = intent.message.text.orEmpty()
                    )
                }
            }
            ChatContract.Intent.OnCancelComposerMode -> blockingIntent {
                reduce {
                    val wasEditing = state.composerMode is ChatContract.ComposerMode.Edit
                    // Tahrir bekor qilinsa, maydondagi asl matn ham tozalanadi; javobda esa yozilgan matn qoladi.
                    state.copy(
                        composerMode = ChatContract.ComposerMode.None,
                        composerText = if (wasEditing) "" else state.composerText
                    )
                }
            }
            is ChatContract.Intent.OnDelete -> delete(intent.message.serverId)
            is ChatContract.Intent.OnRetry -> intent { retryMessage(intent.message.clientMessageId) }
            ChatContract.Intent.OnLoadOlder -> loadOlder()
            ChatContract.Intent.OnBottomVisible -> markRead()
        }
    }

    /**
     * Ekran lokal bazani kuzatadi: yangi xabar (socket, sync yoki o'zim yuborgan) bazaga tushishi bilan
     * ro'yxat o'zi yangilanadi. `repeatOnSubscription` — ekran ko'rinmaganda kuzatish to'xtaydi.
     */
    private fun observeData() = intent {
        repeatOnSubscription {
            val content = combine(observeMessages(chatId), observeChat(chatId), observeUserNames()) { messages, chat, names ->
                Triple(messages, chat, names)
            }
            combine(content, observeMe(), observeTyping()) { (messages, chat, names), me, typing ->
                ChatData(
                    chat = chat,
                    items = buildChatItems(messages, isGroup = chat?.type == ChatType.GROUP),
                    names = names,
                    myUserId = me?.id,
                    typing = typing[chatId].orEmpty() - me?.id.orEmpty()
                )
            }.collect { data ->
                // Yozish paneli holati (matn, rejim) saqlanadi — faqat ma'lumot qismi yangilanadi.
                reduce {
                    state.copy(
                        chat = data.chat,
                        items = data.items,
                        userNames = data.names,
                        myUserId = data.myUserId,
                        typingUserIds = data.typing
                    )
                }
            }
        }
    }

    private data class ChatData(
        val chat: ChatSummary?,
        val items: List<ChatItem>,
        val names: Map<String, String>,
        val myUserId: String?,
        val typing: Set<String>
    )

    /** Chat ochilganda eng yangi sahifa serverdan olinadi (lokal nusxa eskirgan yoki bo'sh bo'lishi mumkin). */
    private fun loadLatest() = intent {
        when (val result = loadLatestMessages(chatId)) {
            is AppResult.Success -> reduce { state.copy(hasMore = result.data) }
            is AppResult.Error -> showError(result.error)
        }
    }

    private fun loadOlder() = intent {
        if (!state.hasMore || state.isLoadingOlder) return@intent
        reduce { state.copy(isLoadingOlder = true) }
        when (val result = loadOlderMessages(chatId)) {
            is AppResult.Success -> reduce { state.copy(isLoadingOlder = false, hasMore = result.data) }
            is AppResult.Error -> {
                reduce { state.copy(isLoadingOlder = false) }
                showError(result.error)
            }
        }
    }

    /**
     * Matn maydoni sinxron yangilanadi (tez yozganda kursor sakramasin). "Yozmoqda…" signali har o'zgarishda
     * yuboriladi — server uni o'zi 3 s da bittaga cheklaydi (spec: klientda qo'shimcha cheklov kerak emas).
     */
    private fun onTextChange(text: String) {
        var shouldSignal = false
        blockingIntent {
            reduce { state.copy(composerText = text) }
            shouldSignal = text.isNotBlank() && state.composerMode !is ChatContract.ComposerMode.Edit
        }
        if (shouldSignal) sendTyping(chatId)
    }

    private fun send() = intent {
        val text = state.composerText.trim()
        if (text.isEmpty()) return@intent

        when (val mode = state.composerMode) {
            is ChatContract.ComposerMode.Edit -> {
                val serverId = mode.message.serverId ?: return@intent
                when (val result = editMessage(serverId, text)) {
                    is AppResult.Success -> reduce {
                        state.copy(composerText = "", composerMode = ChatContract.ComposerMode.None)
                    }
                    is AppResult.Error -> showError(result.error)
                }
            }

            else -> {
                val replyTo = (mode as? ChatContract.ComposerMode.Reply)?.message?.clientMessageId
                // Maydon darhol tozalanadi: xabar bazaga yoziladi va ro'yxatda "yuborilmoqda" bo'lib ko'rinadi,
                // tarmoq javobini kutish shart emas (offline'da ham shunday).
                reduce { state.copy(composerText = "", composerMode = ChatContract.ComposerMode.None) }
                sendTextMessage(chatId, text, replyTo)
            }
        }
    }

    private fun delete(serverId: Long?) = intent {
        if (serverId == null) return@intent
        when (val result = deleteMessage(serverId)) {
            is AppResult.Success -> Unit
            is AppResult.Error -> showError(result.error)
        }
    }

    /** Faqat yangi xabar ko'ringandagina serverga `read` yuboriladi (max-wins bo'lsa ham, keraksiz so'rov yo'q). */
    private fun markRead() = intent {
        val latestSeq = state.items.firstNotNullOfOrNull { item ->
            when (item) {
                is ChatItem.Bubble -> item.message.serverSeq
                is ChatItem.System -> item.message.serverSeq
                is ChatItem.DateSeparator -> null
            }
        } ?: return@intent
        if (latestSeq <= lastReadSeq) return@intent
        lastReadSeq = latestSeq
        markChatRead(chatId)
    }

    /** Sessiya tugagan (401) holatni MainViewModel o'zi hal qiladi — foydalanuvchiga ko'rsatilmaydi. */
    private suspend fun Syntax<ChatContract.UiState, ChatContract.SideEffect>.showError(error: AppError) {
        if (error is AppError.Api && error.httpStatus == 401) return
        postSideEffect(ChatContract.SideEffect.ShowError(error))
    }
}
