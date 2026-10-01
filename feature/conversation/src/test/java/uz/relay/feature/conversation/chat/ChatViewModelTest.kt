package uz.relay.feature.conversation.chat

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.orbitmvi.orbit.test.test
import uz.relay.core.common.result.AppError
import uz.relay.core.common.result.AppResult
import uz.relay.domain.model.Attachment
import uz.relay.domain.model.CallLog
import uz.relay.domain.model.CallLogFormat
import uz.relay.domain.model.CallOutcome
import uz.relay.domain.model.ChatType
import uz.relay.domain.model.DownloadState
import uz.relay.domain.model.MediaKind
import uz.relay.domain.model.MessageMedia
import uz.relay.domain.model.MessageType
import uz.relay.domain.testing.FakeCallRepository
import uz.relay.domain.testing.FakeChatRepository
import uz.relay.domain.testing.FakeGroupRepository
import uz.relay.domain.testing.FakeMediaRepository
import uz.relay.domain.testing.FakeMessageRepository
import uz.relay.domain.testing.FakeTypingRepository
import uz.relay.domain.testing.FakeUserRepository
import uz.relay.domain.testing.MainDispatcherRule
import uz.relay.domain.testing.TestData
import uz.relay.domain.usecase.call.ObserveGroupCallUseCase
import uz.relay.domain.usecase.call.PrepareGroupCallUseCase
import uz.relay.domain.usecase.call.StartCallUseCase
import uz.relay.domain.usecase.chat.ObserveChatUseCase
import uz.relay.domain.usecase.chat.ObserveTypingUseCase
import uz.relay.domain.usecase.group.ObserveMembersUseCase
import uz.relay.domain.usecase.group.RefreshMembersUseCase
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
 * Chat ekranining asosiy oqimlari: yuborish (oddiy / javob / tahrir), o'chirish, o'qildi belgisi, eski sahifa,
 * media, qo'ng'iroqlar. Bazani kuzatish (runOnCreate) faqat kerak bo'lgan testda — qolganlari tayyor holatdan.
 */
class ChatViewModelTest {

    @get:Rule val mainRule = MainDispatcherRule()

    private val messages = FakeMessageRepository()
    private val chats = FakeChatRepository()
    private val users = FakeUserRepository()
    private val typing = FakeTypingRepository()
    private val groups = FakeGroupRepository()
    private val media = FakeMediaRepository()
    private val calls = FakeCallRepository()
    private val navigation = mutableListOf<String>()
    private val directions = object : ChatContract.Directions {
        override suspend fun back() { navigation += "back" }
        override suspend fun navigateToGroupInfo(chatId: String) { navigation += "group-info:$chatId" }
        override suspend fun navigateToUserProfile(userId: String) { navigation += "user:$userId" }
        override suspend fun navigateToMediaViewer(chatId: String, clientMessageId: String) { navigation += "viewer:$clientMessageId" }
        override suspend fun navigateToCall(callId: String, video: Boolean, chatId: String) { navigation += "call:$callId:$video" }
        override suspend fun navigateToGroupCall(callId: String, chatId: String) { navigation += "group-call:$callId" }
    }

    private fun viewModel(chatId: String = "chat") = ChatViewModel(
        chatId = chatId,
        observeMessages = ObserveMessagesUseCase(messages),
        observeChat = ObserveChatUseCase(chats),
        observeUserNames = ObserveUserNamesUseCase(users),
        observeMe = ObserveMeUseCase(users),
        observeTyping = ObserveTypingUseCase(typing),
        loadLatestMessages = LoadLatestMessagesUseCase(messages),
        loadOlderMessages = LoadOlderMessagesUseCase(messages),
        sendTextMessage = SendTextMessageUseCase(messages),
        retryMessage = RetryMessageUseCase(messages),
        editMessage = EditMessageUseCase(messages),
        deleteMessage = DeleteMessageUseCase(messages),
        sendTyping = SendTypingUseCase(messages),
        markChatRead = MarkChatReadUseCase(messages),
        observeMembers = ObserveMembersUseCase(groups),
        refreshMembers = RefreshMembersUseCase(groups),
        sendMediaMessage = SendMediaMessageUseCase(messages),
        cancelUpload = CancelUploadUseCase(messages),
        downloadMedia = DownloadMediaUseCase(media),
        startCall = StartCallUseCase(calls),
        prepareGroupCall = PrepareGroupCallUseCase(calls),
        observeGroupCall = ObserveGroupCallUseCase(calls),
        directions = directions
    )

    private val direct = TestData.chat(id = "chat", peerUserId = "vali")
    private val group = TestData.chat(id = "chat", type = ChatType.GROUP, title = "Oila", peerUserId = null)
    private val original = message("m1", senderId = "vali", text = "Salom")

    @Test
    fun `send trims text clears composer and queues message`() = runTest {
        viewModel().test(this, ChatContract.UiState(composerText = "  Salom  ")) {
            expectInitialState()
            containerHost.onEventDispatcher(ChatContract.Intent.OnSend)
            expectState { copy(composerText = "") }
        }
        assertEquals(listOf(Triple("chat", "Salom", null as String?)), messages.sentTexts)
    }

    @Test
    fun `blank text is not sent`() = runTest {
        viewModel().test(this, ChatContract.UiState(composerText = "   ")) {
            expectInitialState()
            containerHost.onEventDispatcher(ChatContract.Intent.OnSend)
        }
        assertTrue(messages.sentTexts.isEmpty())
    }

    @Test
    fun `reply mode sends reply reference and exits mode`() = runTest {
        viewModel().test(this) {
            expectInitialState()
            containerHost.onEventDispatcher(ChatContract.Intent.OnReply(original))
            expectState { copy(composerMode = ChatContract.ComposerMode.Reply(original)) }
            containerHost.onEventDispatcher(ChatContract.Intent.OnTextChange("Qalaysan"))
            expectState { copy(composerText = "Qalaysan") }
            containerHost.onEventDispatcher(ChatContract.Intent.OnSend)
            expectState { copy(composerText = "", composerMode = ChatContract.ComposerMode.None) }
        }
        assertEquals(listOf(Triple("chat", "Qalaysan", "m1")), messages.sentTexts)
    }

    @Test
    fun `edit fills composer and saves via server id`() = runTest {
        val mine = message("m2", isMine = true, text = "Eski", serverId = 42)
        viewModel().test(this) {
            expectInitialState()
            containerHost.onEventDispatcher(ChatContract.Intent.OnEdit(mine))
            expectState { copy(composerMode = ChatContract.ComposerMode.Edit(mine), composerText = "Eski") }
            containerHost.onEventDispatcher(ChatContract.Intent.OnTextChange("Yangi"))
            expectState { copy(composerText = "Yangi") }
            containerHost.onEventDispatcher(ChatContract.Intent.OnSend)
            expectState { copy(composerText = "", composerMode = ChatContract.ComposerMode.None) }
        }
        assertEquals(listOf(42L to "Yangi"), messages.edits)
        assertTrue(messages.sentTexts.isEmpty())
        // Tahrir paytida "yozmoqda…" signali yuborilmaydi.
        assertEquals(0, messages.typingSignals)
    }

    @Test
    fun `cancelling edit clears text but cancelling reply keeps it`() = runTest {
        val mine = message("m2", isMine = true, text = "Eski", serverId = 42)
        viewModel().test(this, ChatContract.UiState(composerText = "Eski", composerMode = ChatContract.ComposerMode.Edit(mine))) {
            expectInitialState()
            containerHost.onEventDispatcher(ChatContract.Intent.OnCancelComposerMode)
            expectState { copy(composerText = "", composerMode = ChatContract.ComposerMode.None) }
        }
        viewModel().test(this, ChatContract.UiState(composerText = "Yozilgan", composerMode = ChatContract.ComposerMode.Reply(original))) {
            expectInitialState()
            containerHost.onEventDispatcher(ChatContract.Intent.OnCancelComposerMode)
            expectState { copy(composerMode = ChatContract.ComposerMode.None) }
        }
    }

    @Test
    fun `failed edit keeps composer and shows error`() = runTest {
        messages.editResult = AppResult.Error(AppError.Network)
        val mine = message("m2", isMine = true, serverId = 42)
        viewModel().test(this, ChatContract.UiState(composerText = "Yangi", composerMode = ChatContract.ComposerMode.Edit(mine))) {
            expectInitialState()
            containerHost.onEventDispatcher(ChatContract.Intent.OnSend)
            expectSideEffect(ChatContract.SideEffect.ShowError(AppError.Network))
        }
    }

    @Test
    fun `typing signal is sent only for non blank text`() = runTest {
        viewModel().test(this) {
            expectInitialState()
            containerHost.onEventDispatcher(ChatContract.Intent.OnTextChange("S"))
            expectState { copy(composerText = "S") }
            containerHost.onEventDispatcher(ChatContract.Intent.OnTextChange(" "))
            expectState { copy(composerText = " ") }
        }
        assertEquals(1, messages.typingSignals)
    }

    @Test
    fun `delete and retry reach repository and unauthorized errors stay silent`() = runTest {
        messages.deleteResult = AppResult.Error(AppError.Api(401, "UNAUTHORIZED", "", retryable = false))
        viewModel().test(this) {
            expectInitialState()
            containerHost.onEventDispatcher(ChatContract.Intent.OnDelete(message("m3", serverId = 7)))
            containerHost.onEventDispatcher(ChatContract.Intent.OnRetry(message("m4", serverId = null)))
        }
        assertEquals(listOf(7L), messages.deletes)
        assertEquals(listOf("m4"), messages.retries)
    }

    @Test
    fun `older page is not requested when nothing more`() = runTest {
        viewModel().test(this, ChatContract.UiState(hasMore = false)) {
            expectInitialState()
            containerHost.onEventDispatcher(ChatContract.Intent.OnLoadOlder)
        }
        assertEquals(0, messages.loadOlderCalls)
    }

    @Test
    fun `older page updates has more flag`() = runTest {
        messages.loadOlderResult = AppResult.Success(false)
        viewModel().test(this) {
            expectInitialState()
            containerHost.onEventDispatcher(ChatContract.Intent.OnLoadOlder)
            expectState { copy(isLoadingOlder = true) }
            expectState { copy(isLoadingOlder = false, hasMore = false) }
        }
    }

    @Test
    fun `read cursor is sent once per new message`() = runTest {
        val items = buildChatItems(listOf(message("m5", serverId = 10)), isGroup = false)
        viewModel().test(this, ChatContract.UiState(items = items)) {
            expectInitialState()
            containerHost.onEventDispatcher(ChatContract.Intent.OnBottomVisible)
            containerHost.onEventDispatcher(ChatContract.Intent.OnBottomVisible)
        }
        assertEquals(listOf("chat"), messages.markedRead)
    }

    @Test
    fun `media caption comes from composer and clears it`() = runTest {
        val photo = Attachment(uri = "content://photo", asFile = false)
        viewModel().test(this, ChatContract.UiState(composerText = "Mana rasm")) {
            expectInitialState()
            containerHost.onEventDispatcher(ChatContract.Intent.OnAttach(photo))
            expectState { copy(isPreparingMedia = true, composerText = "") }
            expectState { copy(isPreparingMedia = false) }
        }
        assertEquals(listOf(photo to "Mana rasm"), messages.sentMedia)
    }

    @Test
    fun `file attachment keeps composer text`() = runTest {
        val file = Attachment(uri = "content://doc", asFile = true)
        viewModel().test(this, ChatContract.UiState(composerText = "Matn")) {
            expectInitialState()
            containerHost.onEventDispatcher(ChatContract.Intent.OnAttach(file))
            expectState { copy(isPreparingMedia = true) }
            expectState { copy(isPreparingMedia = false) }
        }
        assertEquals(listOf(file to null as String?), messages.sentMedia)
    }

    @Test
    fun `image opens viewer and file is downloaded then opened`() = runTest {
        val imageMedia = MessageMedia("i1", MediaKind.IMAGE, "image/jpeg", 10, null, null, null, url = "u")
        val fileMedia = MessageMedia("f1", MediaKind.FILE, "application/pdf", 10, null, null, null, url = "u")
        media.downloadStates = listOf(DownloadState.Progress(5, 10), DownloadState.Done("/cache/a.pdf"))
        val image = message("img", type = MessageType.IMAGE).copy(media = listOf(imageMedia))
        val file = message("doc", type = MessageType.FILE, text = "a.pdf").copy(media = listOf(fileMedia))
        viewModel().test(this) {
            expectInitialState()
            containerHost.onEventDispatcher(ChatContract.Intent.OnMediaClick(image))
            containerHost.onEventDispatcher(ChatContract.Intent.OnMediaClick(file))
            expectState { copy(fileDownloads = mapOf("doc" to 0.5f)) }
            expectState { copy(fileDownloads = emptyMap()) }
            expectSideEffect(ChatContract.SideEffect.OpenFile("/cache/a.pdf", "application/pdf"))
        }
        assertEquals(listOf("viewer:img"), navigation)
    }

    @Test
    fun `direct call opens call screen`() = runTest {
        viewModel().test(this, ChatContract.UiState(chat = direct)) {
            expectInitialState()
            containerHost.onEventDispatcher(ChatContract.Intent.OnStartCall(video = true))
            expectState { copy(isStartingCall = true) }
            expectState { copy(isStartingCall = false) }
        }
        assertEquals(listOf("vali" to true), calls.started)
        assertEquals(listOf("call:call-1:true"), navigation)
    }

    @Test
    fun `starting new group call posts started log`() = runTest {
        viewModel().test(this, ChatContract.UiState(chat = group, groupCallCount = 0)) {
            expectInitialState()
            containerHost.onEventDispatcher(ChatContract.Intent.OnGroupCall)
            expectState { copy(isStartingCall = true) }
            expectState { copy(isStartingCall = false) }
        }
        val started = CallLogFormat.format(CallLog(video = true, outcome = CallOutcome.STARTED, durationSeconds = 0, group = true))
        assertEquals(listOf(Triple("chat", started, null as String?)), messages.sentTexts)
        assertEquals(listOf("group-call:group_chat"), navigation)
    }

    @Test
    fun `joining running group call does not post log`() = runTest {
        viewModel().test(this, ChatContract.UiState(chat = group, groupCallCount = 2)) {
            expectInitialState()
            containerHost.onEventDispatcher(ChatContract.Intent.OnGroupCall)
            expectState { copy(isStartingCall = true) }
            expectState { copy(isStartingCall = false) }
        }
        assertTrue(messages.sentTexts.isEmpty())
        assertEquals(listOf("group-call:group_chat"), navigation)
    }

    @Test
    fun `title opens group info or user profile`() = runTest {
        viewModel().test(this, ChatContract.UiState(chat = group)) {
            expectInitialState()
            containerHost.onEventDispatcher(ChatContract.Intent.OnOpenInfo)
        }
        viewModel().test(this, ChatContract.UiState(chat = direct)) {
            expectInitialState()
            containerHost.onEventDispatcher(ChatContract.Intent.OnOpenInfo)
        }
        assertEquals(listOf("group-info:chat", "user:vali"), navigation)
    }

    @Test
    fun `screen builds items from database`() = runTest {
        chats.chats.value = listOf(direct)
        messages.messages.value = mapOf("chat" to listOf(message("m1", text = "Salom")))
        viewModel().test(this) {
            expectInitialState()
            runOnCreate()
            // Parallel ishlaydigan "eng yangi sahifa" so'rovi ham holatni o'zgartiradi — chat kelguncha kutamiz.
            var state = awaitState()
            while (state.chat == null) state = awaitState()
            assertEquals(direct, state.chat)
            assertEquals("m1", (state.items.first() as ChatItem.Bubble).message.clientMessageId)
            cancelAndIgnoreRemainingItems()
        }
    }
}
