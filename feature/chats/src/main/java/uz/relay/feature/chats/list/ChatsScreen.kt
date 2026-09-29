package uz.relay.feature.chats.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import kotlinx.coroutines.launch
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import uz.relay.core.designsystem.R as DesignR
import uz.relay.core.designsystem.component.OfflineBanner
import uz.relay.core.designsystem.component.SkeletonChatRow
import uz.relay.core.designsystem.component.SwiftFab
import uz.relay.core.designsystem.theme.SwiftChatTheme
import uz.relay.core.designsystem.theme.SwiftTheme
import uz.relay.domain.model.ChatSummary
import uz.relay.domain.model.ChatType
import uz.relay.domain.model.ConnectionStatus
import uz.relay.domain.model.LastMessage
import uz.relay.domain.model.MessageStatus
import uz.relay.domain.model.MessageType
import uz.relay.domain.model.User
import uz.relay.feature.chats.R
import uz.relay.feature.chats.list.components.ChatRow
import uz.relay.feature.chats.list.components.ChatsTabs
import uz.relay.feature.chats.list.components.ChatsTopBar
import uz.relay.feature.chats.list.components.EmptyChats
import uz.relay.feature.chats.list.components.MuteSheet
import uz.relay.feature.chats.util.messageRes

@Composable
internal fun ChatsScreen(viewModel: ChatsViewModel = hiltViewModel()) {
    val uiState by viewModel.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    viewModel.collectSideEffect { sideEffect ->
        when (sideEffect) {
            is ChatsContract.SideEffect.ShowError -> {
                val result = snackbarHostState.showSnackbar(
                    message = context.getString(sideEffect.error.messageRes()),
                    actionLabel = context.getString(R.string.retry),
                    duration = SnackbarDuration.Long
                )
                if (result == SnackbarResult.ActionPerformed) {
                    viewModel.onEventDispatcher(ChatsContract.Intent.OnRetrySync)
                }
            }

            is ChatsContract.SideEffect.ShowActionError ->
                snackbarHostState.showSnackbar(context.getString(sideEffect.error.messageRes()))
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        ChatsScreenContent(uiState = uiState, onEventDispatcher = viewModel::onEventDispatcher)

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
        )
    }
}

@Composable
private fun ChatsScreenContent(
    uiState: ChatsContract.UiState,
    onEventDispatcher: (ChatsContract.Intent) -> Unit
) {
    val colors = SwiftTheme.colors
    // Sheet faqat id'ni eslaydi: chat qatori bazadan yangilansa (masalan, mute holati), sheet ham yangisini ko'rsatadi.
    var muteSheetChatId by rememberSaveable { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            ChatsTopBar(
                me = uiState.me,
                connectionStatus = uiState.connectionStatus,
                onSearchClick = { onEventDispatcher(ChatsContract.Intent.OnSearchClick) },
                onMyProfileClick = { onEventDispatcher(ChatsContract.Intent.OnMyProfileClick) }
            )
            if (uiState.connectionStatus == ConnectionStatus.OFFLINE) {
                // Offline'da ham ro'yxat ko'rinadi (lokal bazadan) — banner faqat ogohlantiradi.
                OfflineBanner(text = stringResource(R.string.no_internet))
            }

            when {
                uiState.showSkeleton -> SkeletonList()
                uiState.showEmpty -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyChats(onNewChatClick = { onEventDispatcher(ChatsContract.Intent.OnSearchClick) })
                }
                else -> ChatsPager(
                    uiState = uiState,
                    onEventDispatcher = onEventDispatcher,
                    onChatLongClick = { muteSheetChatId = it }
                )
            }
        }

        // Bo'sh holatda FAB yashiriladi — o'rnida "Yangi chat" tugmasi bor (spec 4-bo'lim).
        if (!uiState.showEmpty) {
            SwiftFab(
                icon = DesignR.drawable.ic_pencil,
                contentDescription = stringResource(R.string.new_chat),
                onClick = { onEventDispatcher(ChatsContract.Intent.OnSearchClick) },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(end = 16.dp, bottom = 20.dp)
            )
        }
    }

    // Chat o'chib ketsa (masalan, guruhdan chiqarildim) — sheet o'zi yopiladi.
    uiState.chats.firstOrNull { it.id == muteSheetChatId }?.let { chat ->
        MuteSheet(
            chat = chat,
            onDismiss = { muteSheetChatId = null },
            onMute = { duration ->
                muteSheetChatId = null
                onEventDispatcher(ChatsContract.Intent.OnMute(chat.id, duration))
            },
            onUnmute = {
                muteSheetChatId = null
                onEventDispatcher(ChatsContract.Intent.OnUnmute(chat.id))
            }
        )
    }
}

/** Tablar va ular ostidagi sahifalar: tabni bosish ham, chapga-o'ngga surish ham ishlaydi. */
@Composable
private fun ChatsPager(
    uiState: ChatsContract.UiState,
    onEventDispatcher: (ChatsContract.Intent) -> Unit,
    onChatLongClick: (chatId: String) -> Unit
) {
    val tabs = ChatTab.entries
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val scope = rememberCoroutineScope()

    ChatsTabs(
        selected = tabs[pagerState.currentPage],
        unreadChats = uiState::unreadChatsIn,
        onSelect = { tab -> scope.launch { pagerState.animateScrollToPage(tab.ordinal) } }
    )
    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
        // Qo'shni sahifa oldindan tayyorlanadi — surishda "bo'sh kadr" ko'rinmasin.
        beyondViewportPageCount = 1
    ) { page ->
        val chats = uiState.chatsFor(tabs[page])
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            // Oxirgi qator FAB va navigatsiya paneli ostida qolmasin.
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            items(items = chats, key = { it.id }) { chat ->
                ChatRow(
                    chat = chat,
                    userNames = uiState.userNames,
                    typingUserIds = uiState.typing[chat.id].orEmpty(),
                    onClick = { onEventDispatcher(ChatsContract.Intent.OnChatClick(chat.id)) },
                    onLongClick = { onChatLongClick(chat.id) }
                )
            }
        }
    }
}

@Composable
private fun SkeletonList() {
    Column {
        SkeletonWidths.forEach { (title, subtitle) ->
            SkeletonChatRow(titleWidth = title.dp, subtitleWidth = subtitle.dp)
        }
    }
}

// Dizayndagi turli uzunliklar: bir xil chiziqlar sun'iy ko'rinadi.
private val SkeletonWidths = listOf(
    120 to 200, 96 to 170, 140 to 220, 110 to 150, 130 to 190, 90 to 210, 124 to 160, 104 to 184, 136 to 176
)

// ---------------- Preview'lar ----------------

private val PreviewMe = User("me", "dawran", "Dawran", null, 0, null)

private fun previewChat(
    id: String,
    title: String,
    type: ChatType = ChatType.DIRECT,
    text: String? = null,
    messageType: MessageType = MessageType.TEXT,
    senderId: String = "peer_$id",
    unread: Int = 0,
    muted: Boolean = false,
    online: Boolean = false,
    deleted: Boolean = false,
    status: MessageStatus = MessageStatus.SENT,
    minutesAgo: Long = 10
): ChatSummary {
    val time = System.currentTimeMillis() - minutesAgo * 60_000
    return ChatSummary(
        id = id,
        type = type,
        title = title,
        peerUserId = if (type == ChatType.DIRECT) "peer_$id" else null,
        peerOnline = online,
        peerLastSeenAt = null,
        lastMessage = LastMessage(
            serverId = 1,
            senderId = senderId,
            isMine = senderId == PreviewMe.id,
            type = messageType,
            text = text,
            systemEvent = null,
            isDeleted = deleted,
            createdAt = time,
            status = status
        ),
        lastActivityAt = time,
        unreadCount = unread,
        muted = muted
    )
}

private val PreviewChats = listOf(
    previewChat("1", "Jasur Aliyev", text = "Ertaga soat 10 da koʻrishamiz", unread = 2, online = true, minutesAgo = 5),
    previewChat("2", "Loyiha jamoasi", ChatType.GROUP, text = "Maket tayyor, koʻrib chiqing", senderId = "malika", unread = 14, muted = true, minutesAgo = 20),
    previewChat("3", "Dilnoza", deleted = true, minutesAgo = 90),
    previewChat("4", "Sardor Rahimov", messageType = MessageType.IMAGE, senderId = "me", status = MessageStatus.DELIVERED, minutesAgo = 60 * 26),
    previewChat("5", "Kurs guruhi", ChatType.GROUP, text = "Hisobot.pdf", senderId = "me", status = MessageStatus.READ, minutesAgo = 60 * 24 * 3),
    previewChat("6", "Bekzod Tursunov", text = "Rahmat, oldim!", minutesAgo = 60 * 24 * 9)
)

private val PreviewNames = mapOf("malika" to "Malika")

@Composable
private fun ChatsPreview(darkTheme: Boolean, state: ChatsContract.UiState) {
    SwiftChatTheme(darkTheme = darkTheme) { ChatsScreenContent(uiState = state, onEventDispatcher = {}) }
}

@Preview(name = "List · Light", showSystemUi = true)
@Composable
private fun ChatsListLightPreview() = ChatsPreview(
    false,
    ChatsContract.UiState(chats = PreviewChats, userNames = PreviewNames, me = PreviewMe, isBootstrapped = true)
)

@Preview(name = "List · Dark", showSystemUi = true)
@Composable
private fun ChatsListDarkPreview() = ChatsPreview(
    true,
    ChatsContract.UiState(chats = PreviewChats, userNames = PreviewNames, me = PreviewMe, isBootstrapped = true)
)

@Preview(name = "Offline · Light", showSystemUi = true)
@Composable
private fun ChatsOfflineLightPreview() = ChatsPreview(
    false,
    ChatsContract.UiState(
        chats = PreviewChats,
        userNames = PreviewNames,
        me = PreviewMe,
        connectionStatus = ConnectionStatus.OFFLINE,
        typing = mapOf("2" to setOf("malika")),
        isBootstrapped = true
    )
)

@Preview(name = "Offline · Dark", showSystemUi = true)
@Composable
private fun ChatsOfflineDarkPreview() = ChatsPreview(
    true,
    ChatsContract.UiState(
        chats = PreviewChats,
        userNames = PreviewNames,
        me = PreviewMe,
        connectionStatus = ConnectionStatus.OFFLINE,
        typing = mapOf("2" to setOf("malika")),
        isBootstrapped = true
    )
)

@Preview(name = "Loading · Light", showSystemUi = true)
@Composable
private fun ChatsLoadingLightPreview() = ChatsPreview(false, ChatsContract.UiState(me = PreviewMe, connectionStatus = ConnectionStatus.UPDATING))

@Preview(name = "Loading · Dark", showSystemUi = true)
@Composable
private fun ChatsLoadingDarkPreview() = ChatsPreview(true, ChatsContract.UiState(me = PreviewMe, connectionStatus = ConnectionStatus.UPDATING))

@Preview(name = "Empty · Light", showSystemUi = true)
@Composable
private fun ChatsEmptyLightPreview() = ChatsPreview(false, ChatsContract.UiState(me = PreviewMe, isBootstrapped = true))

@Preview(name = "Empty · Dark", showSystemUi = true)
@Composable
private fun ChatsEmptyDarkPreview() = ChatsPreview(true, ChatsContract.UiState(me = PreviewMe, isBootstrapped = true))
