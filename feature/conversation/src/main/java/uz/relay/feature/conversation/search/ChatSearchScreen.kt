package uz.relay.feature.conversation.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import org.orbitmvi.orbit.compose.collectAsState
import uz.relay.core.designsystem.R as DesignR
import uz.relay.core.designsystem.component.Avatar
import uz.relay.core.designsystem.theme.FigtreeFontFamily
import uz.relay.core.designsystem.theme.SwiftChatTheme
import uz.relay.core.designsystem.theme.SwiftTheme
import uz.relay.domain.model.Message
import uz.relay.domain.model.MessageStatus
import uz.relay.domain.model.MessageType
import uz.relay.feature.conversation.R
import uz.relay.feature.conversation.util.formatMessageTime

/**
 * Chat ichida qidiruv ekrani (Nav3 entry: ChatSearchKey). ViewModel AssistedInject factory bilan `chatId`ni oladi;
 * chizish [ChatSearchContent]da — Preview ViewModel'siz ishlashi uchun.
 */
@Composable
internal fun ChatSearchScreen(chatId: String) {
    val viewModel = hiltViewModel<ChatSearchViewModel, ChatSearchViewModel.Factory>(
        creationCallback = { factory -> factory.create(chatId) }
    )
    val uiState by viewModel.collectAsState()
    ChatSearchContent(uiState = uiState, onEventDispatcher = viewModel::onEventDispatcher)
}

/**
 * Qidiruv UI'si: yuqorida so'rov maydoni (ekran ochilishi bilan fokus va klaviatura), ostida izoh va natijalar.
 * BasicTextField ishlatilgan — Material TextField'ning ramka/label'i dizayndagi "toza" sarlavha qatoriga to'g'ri kelmaydi.
 */
@Composable
internal fun ChatSearchContent(
    uiState: ChatSearchContract.UiState,
    onEventDispatcher: (ChatSearchContract.Intent) -> Unit
) {
    val colors = SwiftTheme.colors
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .statusBarsPadding()
            .imePadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { onEventDispatcher(ChatSearchContract.Intent.OnBack) }) {
                Icon(painter = painterResource(DesignR.drawable.ic_arrow_left), contentDescription = stringResource(R.string.back), tint = colors.text)
            }
            BasicTextField(
                value = uiState.query,
                onValueChange = { onEventDispatcher(ChatSearchContract.Intent.OnQueryChange(it)) },
                singleLine = true,
                textStyle = TextStyle(color = colors.text, fontSize = 18.sp, fontFamily = FigtreeFontFamily),
                cursorBrush = SolidColor(colors.primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp)
                    .focusRequester(focusRequester),
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (uiState.query.isEmpty()) Text(stringResource(R.string.search_in_chat_hint), color = colors.text2, fontSize = 18.sp)
                        inner()
                    }
                }
            )
            if (uiState.query.isNotEmpty()) {
                IconButton(onClick = { onEventDispatcher(ChatSearchContract.Intent.OnClear) }) {
                    Icon(
                        painter = painterResource(DesignR.drawable.ic_close),
                        contentDescription = stringResource(R.string.clear),
                        tint = colors.text2,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
        HorizontalDivider(thickness = 1.dp, color = colors.line)

        // Izoh: qidiruv serverda emas, faqat qurilmadagi xabarlar ichida.
        Text(
            text = stringResource(if (uiState.showNothingFound) R.string.nothing_found else R.string.search_local_note),
            color = colors.text2,
            fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
        )

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(items = uiState.results, key = { it.clientMessageId }) { message ->
                ResultRow(
                    message = message,
                    senderName = uiState.userNames[message.senderId],
                    query = uiState.query.trim(),
                    onClick = { onEventDispatcher(ChatSearchContract.Intent.OnResultClick(message)) }
                )
            }
        }
    }
}

/** Natija: avatar 40 · yuboruvchi + vaqt · matn (topilgan qism qalin, primary). */
@Composable
private fun ResultRow(message: Message, senderName: String?, query: String, onClick: () -> Unit) {
    val colors = SwiftTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar(name = senderName, colorSeed = message.senderId, size = 40.dp)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = senderName.orEmpty(),
                    color = colors.text,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(text = formatMessageTime(message.createdAt), color = colors.text2, fontSize = 13.sp)
            }
            Text(
                text = highlight(message.text.orEmpty().replace('\n', ' '), query, colors.primary),
                color = colors.text2,
                fontSize = 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** Matndagi birinchi moslikni qalin va rangli qiladi (katta-kichik harf farqsiz). */
private fun highlight(text: String, query: String, color: Color): AnnotatedString {
    val index = if (query.isEmpty()) -1 else text.indexOf(query, ignoreCase = true)
    if (index < 0) return AnnotatedString(text)
    return buildAnnotatedString {
        append(text.substring(0, index))
        withStyle(SpanStyle(color = color, fontWeight = FontWeight.Bold)) { append(text.substring(index, index + query.length)) }
        append(text.substring(index + query.length))
    }
}

// Preview: bitta topilgan natija bilan (moslik ajratib ko'rsatiladi).
@Preview(name = "Light", showSystemUi = true)
@Composable
private fun ChatSearchLightPreview() {
    SwiftChatTheme(darkTheme = false) {
        ChatSearchContent(
            uiState = ChatSearchContract.UiState(
                query = "maket",
                searchedQuery = "maket",
                results = listOf(
                    Message("1", 1, "c", "u1", false, 10, MessageType.TEXT, "Maket tayyor, koʻrib chiqing", null, null,
                        System.currentTimeMillis(), false, false, MessageStatus.SENT)
                ),
                userNames = mapOf("u1" to "Malika Yusupova")
            ),
            onEventDispatcher = {}
        )
    }
}
