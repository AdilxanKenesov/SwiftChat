package uz.relay.feature.chats.search

import uz.relay.core.designsystem.component.SwiftSnackbarHost
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import uz.relay.core.designsystem.R as DesignR
import uz.relay.core.designsystem.component.Avatar
import uz.relay.core.designsystem.component.BrandTile
import uz.relay.core.designsystem.theme.FigtreeFontFamily
import uz.relay.core.designsystem.theme.SwiftChatTheme
import uz.relay.core.designsystem.theme.SwiftTheme
import uz.relay.domain.model.User
import uz.relay.feature.chats.R
import uz.relay.feature.chats.util.messageRes

/**
 * Qidiruv ekrani (stateful qobiq): holatni yig'adi, xatolarni snackbar'da ko'rsatadi va chizishni
 * holatsiz [SearchScreenContent]ga beradi. Snackbar klaviatura ustida turishi uchun `imePadding`.
 */
@Composable
internal fun SearchScreen(viewModel: SearchViewModel = hiltViewModel()) {
    val uiState by viewModel.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    viewModel.collectSideEffect { sideEffect ->
        when (sideEffect) {
            is SearchContract.SideEffect.ShowError ->
                snackbarHostState.showSnackbar(context.getString(sideEffect.error.messageRes()))
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        SearchScreenContent(uiState = uiState, onEventDispatcher = viewModel::onEventDispatcher)
        SwiftSnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

/**
 * Holatsiz UI: qidiruv paneli, progress chizig'i, "Yangi guruh" qatori va natijalar. Preview'da ViewModel'siz
 * ishlatiladi. "Yangi guruh" doim ro'yxat boshida turadi — qidiruv bo'sh bo'lsa ham.
 */
@Composable
private fun SearchScreenContent(
    uiState: SearchContract.UiState,
    onEventDispatcher: (SearchContract.Intent) -> Unit
) {
    val colors = SwiftTheme.colors

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .statusBarsPadding()
    ) {
        SearchBar(
            query = uiState.query,
            onQueryChange = { onEventDispatcher(SearchContract.Intent.OnQueryChange(it)) },
            onClear = { onEventDispatcher(SearchContract.Intent.OnClear) },
            onBack = { onEventDispatcher(SearchContract.Intent.OnBack) }
        )
        if (uiState.isSearching || uiState.openingUserId != null) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().height(2.dp),
                color = colors.primary,
                trackColor = colors.bg
            )
        }

        if (uiState.showNothingFound) {
            NothingFound(query = uiState.normalizedQuery)
            return@Column
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
        ) {
            item(key = "new-group") {
                NewGroupRow(onClick = { onEventDispatcher(SearchContract.Intent.OnNewGroup) })
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .background(colors.surface)
                )
            }
            if (uiState.results.isNotEmpty()) {
                item(key = "label") {
                    Text(
                        text = stringResource(R.string.users),
                        color = colors.primary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 6.dp)
                    )
                }
            }
            items(items = uiState.results, key = { it.id }) { user ->
                UserRow(
                    user = user,
                    query = uiState.normalizedQuery,
                    onClick = { onEventDispatcher(SearchContract.Intent.OnUserClick(user)) }
                )
            }
        }
    }
}

/** 64dp: orqaga · qidiruv maydoni (avtomatik fokus) · tozalash. Pastda chiziq. */
@Composable
private fun SearchBar(query: String, onQueryChange: (String) -> Unit, onClear: () -> Unit, onBack: () -> Unit) {
    val colors = SwiftTheme.colors
    val focusRequester = remember { FocusRequester() }
    // Ekran ochilishi bilan klaviatura chiqsin — foydalanuvchi darhol yoza boshlasin.
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    painter = painterResource(DesignR.drawable.ic_arrow_left),
                    contentDescription = stringResource(R.string.back),
                    tint = colors.text
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = TextStyle(color = colors.text, fontSize = 18.sp, fontFamily = FigtreeFontFamily),
                cursorBrush = SolidColor(colors.primary),
                // Ascii — username lotin harflarida; autoCorrect o'chiq, aks holda klaviatura so'rovni "tuzatib" yuboradi.
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, autoCorrectEnabled = false, imeAction = ImeAction.Search),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp)
                    .focusRequester(focusRequester),
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (query.isEmpty()) Text(stringResource(R.string.search_hint), color = colors.text2, fontSize = 18.sp)
                        inner()
                    }
                }
            )
            if (query.isNotEmpty()) {
                IconButton(onClick = onClear) {
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
    }
}

/** "Yangi guruh" qatori — guruh yaratish ekraniga olib boradi. */
@Composable
private fun NewGroupRow(onClick: () -> Unit) {
    val colors = SwiftTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(68.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BrandTile(icon = DesignR.drawable.ic_users, size = 48.dp, cornerRadius = 16.dp, iconSize = 22.dp)
        Text(text = stringResource(R.string.new_group), color = colors.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** 68dp: avatar 48 (+ online nuqta) · ism (16/600) · "@" + qidirilgan qism (qalin, primary) + qolgani. */
@Composable
private fun UserRow(user: User, query: String, onClick: () -> Unit) {
    val colors = SwiftTheme.colors
    val username = user.username.orEmpty()
    val matches = query.isNotEmpty() && username.startsWith(query, ignoreCase = true)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(68.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar(name = user.displayName, colorSeed = user.id, size = 48.dp, online = user.online)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = user.displayName,
                color = colors.text,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (username.isNotEmpty()) {
                Text(
                    text = buildAnnotatedString {
                        append("@")
                        if (matches) {
                            withStyle(SpanStyle(color = colors.primary, fontWeight = FontWeight.Bold)) {
                                append(username.take(query.length))
                            }
                            append(username.drop(query.length))
                        } else {
                            append(username)
                        }
                    },
                    color = colors.text2,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/** Natija yo'q: 96dp doira ichida ikonka + "Hech kim topilmadi" + «so'rov» boʻyicha natija yoʻq. */
@Composable
private fun NothingFound(query: String) {
    val colors = SwiftTheme.colors
    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .padding(start = 40.dp, end = 40.dp, bottom = 120.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .background(colors.surface, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(DesignR.drawable.ic_search_x),
                contentDescription = null,
                tint = colors.text2,
                modifier = Modifier.size(42.dp)
            )
        }
        Text(
            text = stringResource(R.string.nobody_found),
            color = colors.text,
            fontSize = 21.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 24.dp)
        )
        Text(
            text = stringResource(R.string.no_results_for, query),
            color = colors.text2,
            fontSize = 15.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}

// ---------------- Preview'lar ----------------

private val PreviewUsers = listOf(
    User("1", "ali_valiyev", "Ali Valiyev", null, 0, null, online = true),
    User("2", "alibek99", "Alibek Soatov", null, 0, null, online = true),
    User("3", "alisher_n", "Alisher Navroʻzov", null, 0, null),
    User("4", "aliya_s", "Aliya Sodiqova", null, 0, null)
)

/** Preview'lar: natijalar va "Hech kim topilmadi" holatlari, yorug' va qorong'i temada. */
@Composable
private fun SearchPreview(darkTheme: Boolean, state: SearchContract.UiState) {
    SwiftChatTheme(darkTheme = darkTheme) { SearchScreenContent(uiState = state, onEventDispatcher = {}) }
}

@Preview(name = "Results · Light", showSystemUi = true)
@Composable
private fun SearchLightPreview() =
    SearchPreview(false, SearchContract.UiState(query = "ali", results = PreviewUsers, searchedQuery = "ali"))

@Preview(name = "Results · Dark", showSystemUi = true)
@Composable
private fun SearchDarkPreview() =
    SearchPreview(true, SearchContract.UiState(query = "ali", results = PreviewUsers, searchedQuery = "ali"))

@Preview(name = "Nothing found · Light", showSystemUi = true)
@Composable
private fun SearchEmptyLightPreview() =
    SearchPreview(false, SearchContract.UiState(query = "xyzq", searchedQuery = "xyzq"))

@Preview(name = "Nothing found · Dark", showSystemUi = true)
@Composable
private fun SearchEmptyDarkPreview() =
    SearchPreview(true, SearchContract.UiState(query = "xyzq", searchedQuery = "xyzq"))
