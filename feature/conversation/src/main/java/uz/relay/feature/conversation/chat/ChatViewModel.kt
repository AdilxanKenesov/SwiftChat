package uz.relay.feature.conversation.chat

import androidx.lifecycle.ViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import org.orbitmvi.orbit.blockingIntent
import org.orbitmvi.orbit.syntax.Syntax
import org.orbitmvi.orbit.viewmodel.orbitContainer
import uz.relay.core.common.result.AppError
import uz.relay.core.common.result.AppResult
import uz.relay.domain.model.ChatSummary
import uz.relay.domain.model.ChatType
import uz.relay.domain.model.DownloadState
import uz.relay.domain.model.Message
import uz.relay.domain.model.MessageMedia
import uz.relay.domain.model.MessageType
import uz.relay.domain.model.MemberRole
import uz.relay.domain.usecase.chat.ObserveChatUseCase
import uz.relay.domain.usecase.chat.ObserveTypingUseCase
import uz.relay.domain.usecase.group.ObserveMembersUseCase
import uz.relay.domain.usecase.group.RefreshMembersUseCase
import uz.relay.domain.usecase.call.StartCallUseCase
import uz.relay.domain.usecase.media.CancelUploadUseCase
import uz.relay.domain.usecase.media.DownloadMediaUseCase
import uz.relay.domain.usecase.media.SendMediaMessageUseCase
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

/**
 * Chat ekranining ViewModel'i (Orbit MVI): xabarlar, sarlavha, yozish paneli, media yuborish/ochish.
 *
 * `chatId` Nav3 kalitidan keladi (runtime qiymat), use-case'lar Hilt'dan — AssistedInject ularni birlashtiradi.
 * SavedStateHandle o'rniga AssistedInject tanlangan: Nav3 kaliti allaqachon tipli obyekt, uni qator argumentlarga
 * aylantirib qayta o'qish shart emas va `chatId` konstruktorda majburiy (null bo'la olmaydi).
 *
 * Ma'lumot oqimi offline-first: ekran faqat lokal bazani kuzatadi, serverdan kelgan har narsa (sahifalar,
 * socket update'lari, o'zim yuborgan xabar) avval bazaga yoziladi va shu yerdan UI'ga tushadi.
 * Ro'yxat elementlari ([ChatItem]) shu yerda [buildChatItems] bilan tuziladi — UI faqat tayyor ro'yxatni chizadi.
 */
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
    private val observeMembers: ObserveMembersUseCase,
    private val refreshMembers: RefreshMembersUseCase,
    private val sendMediaMessage: SendMediaMessageUseCase,
    private val cancelUpload: CancelUploadUseCase,
    private val downloadMedia: DownloadMediaUseCase,
    private val startCall: StartCallUseCase,
    private val directions: ChatContract.Directions
) : ViewModel(), ChatContract.ViewModel {

    /** ChatScreen `hiltViewModel(creationCallback = ...)` orqali shu factory bilan `chatId`ni beradi. */
    @AssistedFactory
    interface Factory {
        fun create(chatId: String): ChatViewModel
    }

    // Container yaratilganda: bazani kuzatish boshlanadi va parallel ravishda eng yangi sahifa serverdan so'raladi.
    override val container =
        orbitContainer<ChatContract.UiState, ChatContract.SideEffect>(ChatContract.UiState()) {
            observeData()
            loadLatest()
        }

    /** Serverga oxirgi yuborilgan o'qish kursori — bir xil qiymatni qayta-qayta yubormaslik uchun. */
    private var lastReadSeq = -1L

    /**
     * Screen'dan keladigan barcha Intent'lar uchun yagona kirish nuqtasi. Matn va composer rejimi
     * `blockingIntent` bilan sinxron yangilanadi (TextField kursori sakramasligi uchun), qolganlari oddiy `intent`.
     */
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
            // Guruh — guruh ma'lumoti, shaxsiy chat — suhbatdoshning profili.
            ChatContract.Intent.OnOpenInfo -> intent {
                val chat = state.chat ?: return@intent
                val peerUserId = chat.peerUserId
                when {
                    state.isGroup -> directions.navigateToGroupInfo(chatId)
                    peerUserId != null -> directions.navigateToUserProfile(peerUserId)
                }
            }
            is ChatContract.Intent.OnAttach -> sendMedia(intent)
            is ChatContract.Intent.OnCancelUpload -> intent { cancelUpload(intent.message.clientMessageId) }
            is ChatContract.Intent.OnMediaClick -> openMedia(intent.message)
            is ChatContract.Intent.OnStartCall -> startCall(intent.video)
        }
    }

    /**
     * Rasm/video uchun maydondagi matn izoh bo'ladi (javob rejimi ham saqlanadi). Fayl uchun izoh yo'q —
     * `body`da fayl nomi ketadi, shuning uchun matn maydonda qoladi.
     */
    private fun sendMedia(intent: ChatContract.Intent.OnAttach) = intent {
        if (state.isPreparingMedia) return@intent
        val mode = state.composerMode
        val replyTo = (mode as? ChatContract.ComposerMode.Reply)?.message?.clientMessageId
        val caption = if (intent.attachment.asFile || mode is ChatContract.ComposerMode.Edit) null else state.composerText.trim()
        reduce {
            state.copy(
                isPreparingMedia = true,
                composerText = if (caption != null) "" else state.composerText,
                composerMode = if (mode is ChatContract.ComposerMode.Reply) ChatContract.ComposerMode.None else mode
            )
        }
        val result = sendMediaMessage(chatId, intent.attachment, caption, replyTo)
        reduce { state.copy(isPreparingMedia = false) }
        if (result is AppResult.Error) showError(result.error)
    }

    /** Rasm/video — to'liq ekranli ko'ruvchiga o'tiladi; fayl — yuklab olinib, tashqi ilovada ochiladi. */
    /**
     * Qo'ng'iroq Stream Video orqali (Relay'da qo'ng'iroq yo'q). Yaratilgach qo'ng'iroq ekrani ochiladi — u yerda
     * suhbatdosh javob berguncha "chiquvchi qo'ng'iroq" ko'rinadi. Faqat shaxsiy chatda (peer bor).
     */
    private fun startCall(video: Boolean) = intent {
        val peerUserId = state.chat?.peerUserId ?: return@intent
        if (state.isStartingCall) return@intent
        reduce { state.copy(isStartingCall = true) }
        val result = startCall(peerUserId, video)
        reduce { state.copy(isStartingCall = false) }
        when (result) {
            is AppResult.Success -> directions.navigateToCall(result.data, video)
            is AppResult.Error -> showError(result.error)
        }
    }

    private fun openMedia(message: Message) = intent {
        val media = message.media.firstOrNull() ?: return@intent
        when (message.type) {
            MessageType.IMAGE, MessageType.VIDEO -> directions.navigateToMediaViewer(chatId, message.clientMessageId)
            MessageType.FILE -> downloadAndOpen(message, media)
            else -> Unit
        }
    }

    /**
     * Fayl keshga yuklab olinadi (progress bubble'da), keyin ochiladi. Oldin yuklangan bo'lsa — darhol ochiladi.
     * Bir fayl ikki marta bosilsa, ikkinchi yuklash boshlanmaydi.
     */
    private fun downloadAndOpen(message: Message, media: MessageMedia) = intent {
        val id = message.clientMessageId
        if (id in state.fileDownloads) return@intent
        val fileName = message.text?.takeIf { it.isNotBlank() } ?: media.mediaId ?: id
        downloadMedia(media, fileName)
            .catch {
                reduce { state.copy(fileDownloads = state.fileDownloads - id) }
                showError(AppError.Network)
            }
            .collect { download ->
                when (download) {
                    is DownloadState.Progress -> reduce {
                        state.copy(fileDownloads = state.fileDownloads + (id to download.downloadedBytes.toFloat() / download.totalBytes.coerceAtLeast(1)))
                    }
                    is DownloadState.Done -> {
                        reduce { state.copy(fileDownloads = state.fileDownloads - id) }
                        postSideEffect(ChatContract.SideEffect.OpenFile(download.path, media.mimeType))
                    }
                }
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
            combine(content, observeMe(), observeTyping(), observeMembers(chatId)) { (messages, chat, names), me, typing, members ->
                ChatData(
                    chat = chat,
                    items = buildChatItems(messages, isGroup = chat?.type == ChatType.GROUP),
                    names = names,
                    myUserId = me?.id,
                    typing = typing[chatId].orEmpty() - me?.id.orEmpty(),
                    memberCount = members.size,
                    myRole = members.firstOrNull { it.isMe }?.role
                )
            }.collect { data ->
                // Guruh ekanligi birinchi marta bilinganda a'zolar ro'yxati yangilanadi (soni va rolim uchun).
                if (data.chat?.type == ChatType.GROUP && !membersRequested) {
                    membersRequested = true
                    refreshGroupMembers()
                }
                // Yozish paneli holati (matn, rejim) saqlanadi — faqat ma'lumot qismi yangilanadi.
                reduce {
                    state.copy(
                        chat = data.chat,
                        items = data.items,
                        userNames = data.names,
                        myUserId = data.myUserId,
                        typingUserIds = data.typing,
                        memberCount = data.memberCount,
                        myRole = data.myRole
                    )
                }
            }
        }
    }

    /** `combine` natijalarini bitta `reduce`ga yig'ish uchun oraliq konteyner. */
    private data class ChatData(
        val chat: ChatSummary?,
        val items: List<ChatItem>,
        val names: Map<String, String>,
        val myUserId: String?,
        val typing: Set<String>,
        val memberCount: Int,
        val myRole: MemberRole?
    )

    /** A'zolar faqat bir marta so'raladi — keyingi o'zgarishlar update'lar orqali bazaga keladi. */
    private var membersRequested = false

    /** Xato jimgina o'tkaziladi: a'zolar soni ko'rinmasa ham suhbat ishlayveradi. */
    private fun refreshGroupMembers() = intent { refreshMembers(chatId) }

    /** Chat ochilganda eng yangi sahifa serverdan olinadi (lokal nusxa eskirgan yoki bo'sh bo'lishi mumkin). */
    private fun loadLatest() = intent {
        when (val result = loadLatestMessages(chatId)) {
            is AppResult.Success -> reduce { state.copy(hasMore = result.data) }
            is AppResult.Error -> showError(result.error)
        }
    }

    /** Eski xabarlar sahifasi; parallel so'rovlar va oxiriga yetilgach qayta so'rash bloklanadi. */
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

    /** Tahrir rejimida — mavjud xabarni tahrirlaydi; aks holda yangi matnli xabar (javob bo'lsa replyTo bilan) yuboradi. */
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

    /** O'chirish serverda bajariladi; tombstone update'i kelgach bazada xabar "o'chirilgan" bo'ladi. */
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
