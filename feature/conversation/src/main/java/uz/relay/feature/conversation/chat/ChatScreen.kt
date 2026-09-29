package uz.relay.feature.conversation.chat

import uz.relay.core.designsystem.R as DesignR
import uz.relay.core.designsystem.component.SwiftDialog
import uz.relay.core.designsystem.component.SwiftSnackbarHost
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.core.content.FileProvider
import uz.relay.feature.conversation.chat.components.AttachSheet
import java.io.File
import android.content.ClipData
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import uz.relay.core.designsystem.theme.SwiftChatTheme
import uz.relay.core.designsystem.theme.SwiftTheme
import uz.relay.domain.model.ChatSummary
import uz.relay.domain.model.ChatType
import uz.relay.domain.model.Message
import uz.relay.domain.model.MessageStatus
import uz.relay.domain.model.MessageType
import uz.relay.domain.model.SystemEvent
import uz.relay.feature.conversation.R
import uz.relay.feature.conversation.chat.components.ChatTopBar
import uz.relay.feature.conversation.chat.components.Composer
import uz.relay.feature.conversation.chat.components.DateChip
import uz.relay.feature.conversation.chat.components.MenuTarget
import uz.relay.feature.conversation.chat.components.MessageMenuOverlay
import uz.relay.feature.conversation.chat.components.MessageRow
import uz.relay.feature.conversation.chat.components.SystemChip
import uz.relay.feature.conversation.chat.components.snippetOf
import uz.relay.feature.conversation.util.formatDateSeparator
import uz.relay.feature.conversation.util.messageRes
import uz.relay.feature.conversation.util.systemText

/**
 * Suhbat ekrani (Nav3 entry: ChatKey). Chatlar ro'yxatidan, push'dan, profil yoki qidiruvdan ochiladi; bu yerdan
 * guruh ma'lumoti, foydalanuvchi profili va media ko'ruvchiga o'tiladi.
 *
 * Bu "stateful" qism: ViewModel'ni (AssistedInject factory orqali `chatId` bilan) oladi, state'ni kuzatadi va
 * SideEffect'larni Snackbar yoki tashqi Intent'ga aylantiradi. Chizish esa [ChatScreenContent]da — u faqat
 * state va callback oladi, shuning uchun Preview'da ViewModel'siz ishlaydi.
 *
 * @param focusMessageId qidiruvdan kelinganda shu xabarga bir marta scroll qilinadi.
 */
@Composable
internal fun ChatScreen(chatId: String, focusMessageId: String? = null) {
    val viewModel = hiltViewModel<ChatViewModel, ChatViewModel.Factory>(
        creationCallback = { factory -> factory.create(chatId) }
    )
    val uiState by viewModel.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    viewModel.collectSideEffect { sideEffect ->
        when (sideEffect) {
            is ChatContract.SideEffect.ShowError ->
                snackbarHostState.showSnackbar(context.getString(sideEffect.error.messageRes()))

            is ChatContract.SideEffect.OpenFile ->
                if (!openFile(context, sideEffect.path, sideEffect.mimeType)) {
                    snackbarHostState.showSnackbar(context.getString(R.string.no_app_to_open))
                }
        }
    }

    ChatScreenContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        focusMessageId = focusMessageId,
        onEventDispatcher = viewModel::onEventDispatcher
    )
}

/**
 * Chat UI'si: sarlavha, xabarlar ro'yxati, yozish paneli va ustki qatlamlar (biriktirish sheet'i, xabar menyusi,
 * o'chirish dialogi).
 *
 * Ro'yxat `reverseLayout = true` LazyColumn: 0-element (eng yangi xabar) pastda turadi. Shunday qilinganda
 * chat ochilganda qo'shimcha scroll kerak emas, yangi xabar qo'shilganda pozitsiya sakramaydi va eski sahifalar
 * ro'yxat oxiriga (ekranda yuqoriga) qo'shiladi. Menyu/dialog kabi vaqtinchalik UI holati ViewModel'ga emas,
 * shu yerda `remember` ichida saqlanadi — bu faqat ko'rinishga tegishli.
 */
@Composable
private fun ChatScreenContent(
    uiState: ChatContract.UiState,
    snackbarHostState: SnackbarHostState,
    onEventDispatcher: (ChatContract.Intent) -> Unit,
    focusMessageId: String? = null
) {
    val colors = SwiftTheme.colors
    val context = LocalContext.current
    val resources = context.resources
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val items = uiState.items

    var menuTarget by remember { mutableStateOf<MenuTarget?>(null) }
    // Saveable: kamera/fayl tanlovchi ochiq paytda Activity qayta yaratilsa ham natija shu sheet'ga qaytadi.
    var showAttach by rememberSaveable { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<Message?>(null) }

    // reverseLayout: 0-element ekranning eng pastida. Pastda turibmizmi — o'qildi kvitansiyasi shunga bog'liq.
    val atBottom by remember { derivedStateOf { listState.firstVisibleItemIndex == 0 } }
    val firstKey = items.firstOrNull()?.key

    // Qidiruvdan kelindi: kerakli xabar ro'yxatda paydo bo'lishi bilan unga BIR MARTA scroll qilamiz.
    var focusHandled by remember(focusMessageId) { mutableStateOf(focusMessageId == null) }
    LaunchedEffect(focusMessageId, items.size) {
        if (focusHandled) return@LaunchedEffect
        val index = items.indexOfFirst { it.key == focusMessageId }
        if (index >= 0) {
            focusHandled = true
            listState.scrollToItem(index)
        }
    }

    // Yangi xabar qo'shildi: pastda edik yoki o'zim yubordim — yangi xabarga tushamiz (aks holda u ko'rinmay qoladi).
    LaunchedEffect(firstKey) {
        val first = items.firstOrNull() as? ChatItem.Bubble
        val mineJustSent = first?.message?.isMine == true && first.message.status == MessageStatus.SENDING
        if (!focusHandled) return@LaunchedEffect
        if (listState.firstVisibleItemIndex <= 1 || mineJustSent) listState.animateScrollToItem(0)
    }
    // Pastda turib yangi xabarlarni ko'ryapmiz — o'qildi deb belgilaymiz (spec: "when bottom visible").
    LaunchedEffect(atBottom, firstKey) {
        if (atBottom && firstKey != null) onEventDispatcher(ChatContract.Intent.OnBottomVisible)
    }
    // Yuqoriga (eski xabarlarga) yaqinlashdik — keyingi sahifani oldindan yuklaymiz.
    LaunchedEffect(listState) {
        snapshotFlow {
            val total = listState.layoutInfo.totalItemsCount
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            total > 0 && lastVisible >= total - LOAD_OLDER_THRESHOLD
        }
            .distinctUntilChanged()
            .filter { it }
            .collect { onEventDispatcher(ChatContract.Intent.OnLoadOlder) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.wall)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            ChatTopBar(
                chat = uiState.chat,
                typingUserIds = uiState.typingUserIds,
                names = uiState.userNames,
                memberCount = uiState.memberCount,
                onBack = { onEventDispatcher(ChatContract.Intent.OnBack) },
                // Guruhda — guruh ma'lumoti, shaxsiy chatda — suhbatdoshning profili (tanlovni ViewModel qiladi).
                onTitleClick = { onEventDispatcher(ChatContract.Intent.OnOpenInfo) },
                onMoreClick = { onEventDispatcher(ChatContract.Intent.OnOpenInfo) },
                modifier = Modifier
                    .background(colors.bg)
                    .statusBarsPadding()
            )

            LazyColumn(
                state = listState,
                reverseLayout = true,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
            ) {
                items(items = items, key = { it.key }) { item ->
                    when (item) {
                        is ChatItem.DateSeparator -> DateChip(text = formatDateSeparator(item.dayStart, resources))
                        is ChatItem.System -> SystemChip(
                            text = item.message.systemEvent
                                ?.let { systemText(it, uiState.myUserId, uiState.userNames, resources) }
                                .orEmpty()
                        )
                        is ChatItem.Bubble -> MessageRow(
                            item = item,
                            isGroup = uiState.isGroup,
                            names = uiState.userNames,
                            onLongPress = { bounds -> menuTarget = MenuTarget(item, bounds) },
                            onReplyClick = {
                                val index = items.indexOfFirst { it.key == item.message.replyToClientMessageId }
                                if (index >= 0) scope.launch { listState.animateScrollToItem(index) }
                            },
                            onRetry = { onEventDispatcher(ChatContract.Intent.OnRetry(item.message)) },
                            onMediaClick = { onEventDispatcher(ChatContract.Intent.OnMediaClick(item.message)) },
                            onCancelUpload = { onEventDispatcher(ChatContract.Intent.OnCancelUpload(item.message)) },
                            downloadProgress = uiState.fileDownloads[item.message.clientMessageId]
                        )
                    }
                }
                if (uiState.isLoadingOlder) {
                    item(key = "loading-older") {
                        Box(modifier = Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp), color = colors.primary, strokeWidth = 2.5.dp)
                        }
                    }
                }
            }

            val modeMessage = when (val mode = uiState.composerMode) {
                is ChatContract.ComposerMode.Reply -> mode.message
                is ChatContract.ComposerMode.Edit -> mode.message
                ChatContract.ComposerMode.None -> null
            }
            Composer(
                text = uiState.composerText,
                mode = uiState.composerMode,
                modeSenderName = modeMessage?.let { uiState.userNames[it.senderId] },
                modeSnippet = modeMessage?.let { snippetOf(it) },
                onTextChange = { onEventDispatcher(ChatContract.Intent.OnTextChange(it)) },
                onSend = { onEventDispatcher(ChatContract.Intent.OnSend) },
                onCancelMode = { onEventDispatcher(ChatContract.Intent.OnCancelComposerMode) },
                onAttach = { showAttach = true },
                // Klaviatura ochiq bo'lsa uning balandligi, yopiq bo'lsa navigatsiya paneli — qaysi katta bo'lsa.
                modifier = Modifier
                    .background(colors.bg)
                    .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars).only(WindowInsetsSides.Bottom))
            )
        }

        SwiftSnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.TopCenter)
        )

        if (showAttach) {
            AttachSheet(
                onDismiss = { showAttach = false },
                onPicked = { attachment ->
                    showAttach = false
                    onEventDispatcher(ChatContract.Intent.OnAttach(attachment))
                },
                onCameraUnavailable = {
                    showAttach = false
                    scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.no_camera_app)) }
                }
            )
        }

        menuTarget?.let { target ->
            MessageMenuOverlay(
                target = target,
                names = uiState.userNames,
                canDeleteOthers = uiState.canDeleteOthers,
                onReply = {
                    menuTarget = null
                    onEventDispatcher(ChatContract.Intent.OnReply(it))
                },
                onEdit = {
                    menuTarget = null
                    onEventDispatcher(ChatContract.Intent.OnEdit(it))
                },
                onCopy = { message ->
                    menuTarget = null
                    scope.launch {
                        clipboard.setClipEntry(ClipEntry(ClipData.newPlainText(null, message.text.orEmpty())))
                        // Android 13+ nusxalanganini o'zi ko'rsatadi — ikki marta xabar bermaymiz.
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                            snackbarHostState.showSnackbar(context.getString(R.string.copied))
                        }
                    }
                },
                onDelete = {
                    menuTarget = null
                    deleteTarget = it
                },
                onDismiss = { menuTarget = null }
            )
        }

        deleteTarget?.let { message ->
            // Qisqa savol + bir qatorli izoh ("hamma uchun o'chiriladi" — qaytarib bo'lmasligini bildiradi).
            SwiftDialog(
                title = stringResource(R.string.delete_title),
                text = stringResource(R.string.delete_text),
                confirmText = stringResource(R.string.delete),
                dismissText = stringResource(R.string.cancel),
                icon = DesignR.drawable.ic_trash,
                destructive = true,
                onConfirm = {
                    deleteTarget = null
                    onEventDispatcher(ChatContract.Intent.OnDelete(message))
                },
                onDismiss = { deleteTarget = null }
            )
        }
    }
}

/** Oxirgi ko'rinayotgan element ro'yxat oxiridan shuncha yaqin bo'lsa, eskiroq sahifa yuklanadi. */
private const val LOAD_OLDER_THRESHOLD = 8

// ---------------- Preview'lar ----------------
// Shaxsiy chat: turli holatdagi xabarlar (yuborilmoqda, xato, o'chirilgan, tahrirlangan, javob), javob va tahrir rejimlari.

private const val ME = "me"
private const val PEER = "jasur"

private fun previewMessage(
    id: String,
    text: String,
    minutesAgo: Long,
    sender: String = PEER,
    status: MessageStatus = MessageStatus.READ,
    edited: Boolean = false,
    deleted: Boolean = false,
    replyTo: String? = null,
    type: MessageType = MessageType.TEXT,
    systemEvent: SystemEvent? = null
) = Message(
    clientMessageId = id,
    serverId = id.hashCode().toLong(),
    chatId = "c",
    senderId = sender,
    isMine = sender == ME,
    serverSeq = 100 - minutesAgo,
    type = type,
    text = text,
    systemEvent = systemEvent,
    replyToClientMessageId = replyTo,
    createdAt = System.currentTimeMillis() - minutesAgo * 60_000,
    isEdited = edited,
    isDeleted = deleted,
    status = status
)

private val PreviewMessages = listOf(
    previewMessage("m9", "Biroz kechikishim mumkin", 1, ME, MessageStatus.FAILED),
    previewMessage("m8", "Hujjatlarni ham olaman", 2, ME, MessageStatus.SENDING),
    previewMessage("m7", "Yetib borgach yozaman", 3, ME, MessageStatus.SENT),
    previewMessage("m6", "Tushunarli, rahmat", 4, ME, MessageStatus.DELIVERED),
    previewMessage("m5", "", 5, deleted = true),
    previewMessage("m4", "Joy oʻzgardi: Amir Temur 15", 6, edited = true),
    previewMessage("m3", "Ha, soat 10 da. Manzilni yuboraman", 7, ME, replyTo = "m2"),
    previewMessage("m2", "Salom! Ertangi uchrashuv kuchdami?", 8),
    previewMessage("m1", "Taqdimot tayyor boʻldimi?", 60 * 24, ME)
)

private val PreviewChat = ChatSummary(
    id = "c",
    type = ChatType.DIRECT,
    title = "Jasur Aliyev",
    peerUserId = PEER,
    peerOnline = true,
    peerLastSeenAt = null,
    lastMessage = null,
    lastActivityAt = 0,
    unreadCount = 0,
    muted = false
)

private fun previewState(mode: ChatContract.ComposerMode = ChatContract.ComposerMode.None, text: String = "") =
    ChatContract.UiState(
        chat = PreviewChat,
        items = buildChatItems(PreviewMessages, isGroup = false),
        userNames = mapOf(PEER to "Jasur Aliyev", ME to "Dawran"),
        myUserId = ME,
        composerText = text,
        composerMode = mode,
        hasMore = false
    )

@Composable
private fun ChatPreview(darkTheme: Boolean, state: ChatContract.UiState) {
    SwiftChatTheme(darkTheme = darkTheme) {
        ChatScreenContent(uiState = state, snackbarHostState = remember { SnackbarHostState() }, onEventDispatcher = {})
    }
}

@Preview(name = "Direct · Light", showSystemUi = true)
@Composable
private fun ChatLightPreview() = ChatPreview(false, previewState())

@Preview(name = "Direct · Dark", showSystemUi = true)
@Composable
private fun ChatDarkPreview() = ChatPreview(true, previewState())

@Preview(name = "Reply · Light", showSystemUi = true)
@Composable
private fun ChatReplyLightPreview() =
    ChatPreview(false, previewState(ChatContract.ComposerMode.Reply(PreviewMessages[7]), "Hozir koʻrib chiqaman"))

@Preview(name = "Edit · Dark", showSystemUi = true)
@Composable
private fun ChatEditDarkPreview() =
    ChatPreview(true, previewState(ChatContract.ComposerMode.Edit(PreviewMessages[2]), "Yetib borgach darhol yozaman"))

/**
 * Faylni tizimdagi mos ilovada ochadi (PDF ko'ruvchi, Excel...). `file://` boshqa ilovaga berilmaydi —
 * FileProvider `content://` beradi va faqat o'qish ruxsatini vaqtincha ulashadi.
 * @return `false` — bunday faylni ochadigan ilova yo'q.
 */
private fun openFile(context: Context, path: String, mimeType: String): Boolean {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", File(path))
    val intent = Intent(Intent.ACTION_VIEW)
        .setDataAndType(uri, mimeType)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    return try {
        context.startActivity(Intent.createChooser(intent, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (e: ActivityNotFoundException) {
        false
    }
}
