package uz.relay.feature.chats.addcontact

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import uz.relay.core.designsystem.R as DesignR
import uz.relay.core.designsystem.component.SwiftSnackbarHost
import uz.relay.core.designsystem.theme.SwiftChatTheme
import uz.relay.core.designsystem.theme.SwiftTheme
import uz.relay.domain.model.User
import uz.relay.feature.chats.R
import uz.relay.feature.chats.search.SearchBar
import uz.relay.feature.chats.search.UserRow
import uz.relay.feature.chats.util.messageRes

/** "Yangi kontakt" ekrani — ViewModel va snackbar'ni ulaydi. */
@Composable
internal fun AddContactScreen(viewModel: AddContactViewModel = hiltViewModel()) {
    val uiState by viewModel.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    viewModel.collectSideEffect { sideEffect ->
        when (sideEffect) {
            is AddContactContract.SideEffect.ShowError ->
                snackbarHostState.showSnackbar(context.getString(sideEffect.error.messageRes()))
            is AddContactContract.SideEffect.Added ->
                snackbarHostState.showSnackbar(context.getString(R.string.contact_added, sideEffect.name))
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AddContactContent(uiState = uiState, onEventDispatcher = viewModel::onEventDispatcher)
        SwiftSnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.TopCenter))
    }
}

/** Qidiruv qatori (qidiruv ekranidagi bilan bir xil) va natijalar: har qatorda "Qo'shish" yoki ✓. */
@Composable
private fun AddContactContent(
    uiState: AddContactContract.UiState,
    onEventDispatcher: (AddContactContract.Intent) -> Unit
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
            onQueryChange = { onEventDispatcher(AddContactContract.Intent.OnQueryChange(it)) },
            onClear = { onEventDispatcher(AddContactContract.Intent.OnClear) },
            onBack = { onEventDispatcher(AddContactContract.Intent.OnBack) }
        )
        if (uiState.isSearching) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(2.dp), color = colors.primary, trackColor = colors.bg)
        }
        if (uiState.showNothingFound) {
            Text(
                text = stringResource(R.string.nothing_found),
                color = colors.text2,
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(32.dp)
            )
            return@Column
        }
        LazyColumn(modifier = Modifier.fillMaxSize().imePadding()) {
            items(items = uiState.results, key = { it.id }) { user ->
                UserRow(
                    user = user,
                    query = uiState.normalizedQuery,
                    onClick = { onEventDispatcher(AddContactContract.Intent.OnUserClick(user)) },
                    trailing = {
                        when {
                            user.id in uiState.contactIds -> Icon(
                                painter = painterResource(DesignR.drawable.ic_check),
                                contentDescription = stringResource(R.string.in_contacts),
                                tint = colors.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            uiState.addingUserId == user.id ->
                                CircularProgressIndicator(color = colors.primary, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
                            else -> AddPill(onClick = { onEventDispatcher(AddContactContract.Intent.OnAdd(user)) })
                        }
                    }
                )
            }
        }
    }
}

/** Kichik "Qo'shish" tugmasi (primaryContainer pill) — qatorning o'ng chetida. */
@Composable
private fun AddPill(onClick: () -> Unit) {
    val colors = SwiftTheme.colors
    Text(
        text = stringResource(R.string.add),
        color = colors.onPrimaryContainer,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(colors.primaryContainer)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp)
    )
}

// ---------------- Preview'lar ----------------

private val PreviewResults = listOf(
    User("1", "ali_valiyev", "Ali Valiyev", null, 0, null, online = true),
    User("2", "alibek99", "Alibek Soatov", null, 0, null)
)

@Composable
private fun AddContactPreview(darkTheme: Boolean) {
    SwiftChatTheme(darkTheme = darkTheme) {
        AddContactContent(
            uiState = AddContactContract.UiState(query = "ali", results = PreviewResults, contactIds = setOf("1"), searchedQuery = "ali"),
            onEventDispatcher = {}
        )
    }
}

@Preview(name = "Light", showSystemUi = true)
@Composable
private fun AddContactLightPreview() = AddContactPreview(false)

@Preview(name = "Dark", showSystemUi = true)
@Composable
private fun AddContactDarkPreview() = AddContactPreview(true)
