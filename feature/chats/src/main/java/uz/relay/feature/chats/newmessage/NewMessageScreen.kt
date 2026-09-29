package uz.relay.feature.chats.newmessage

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import uz.relay.core.designsystem.R as DesignR
import uz.relay.core.designsystem.component.Avatar
import uz.relay.core.designsystem.component.BrandTile
import uz.relay.core.designsystem.component.SwiftDialog
import uz.relay.core.designsystem.component.SwiftSnackbarHost
import uz.relay.core.designsystem.theme.SwiftChatTheme
import uz.relay.core.designsystem.theme.SwiftTheme
import uz.relay.core.designsystem.util.formatPresence
import uz.relay.domain.model.User
import uz.relay.feature.chats.R
import uz.relay.feature.chats.search.SectionLabel
import uz.relay.feature.chats.util.messageRes

/** "Yangi xabar" ekrani — ViewModel va snackbar'ni ulaydi, chizishni [NewMessageContent]ga beradi. */
@Composable
internal fun NewMessageScreen(viewModel: NewMessageViewModel = hiltViewModel()) {
    val uiState by viewModel.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    viewModel.collectSideEffect { sideEffect ->
        when (sideEffect) {
            is NewMessageContract.SideEffect.ShowError ->
                snackbarHostState.showSnackbar(context.getString(sideEffect.error.messageRes()))
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        NewMessageContent(uiState = uiState, onEventDispatcher = viewModel::onEventDispatcher)
        SwiftSnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.TopCenter))
    }
}

/**
 * Tepada ikki amal ("Yangi guruh", "Yangi kontakt"), keyin "Kontaktlar". Kontaktni o'chirish long-press va
 * tasdiq dialogi orqali — tasodifiy bosishdan himoya. Dialog holati faqat UI'ga tegishli.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NewMessageContent(
    uiState: NewMessageContract.UiState,
    onEventDispatcher: (NewMessageContract.Intent) -> Unit
) {
    val colors = SwiftTheme.colors
    val resources = LocalContext.current.resources
    var removeTarget by remember { mutableStateOf<User?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { onEventDispatcher(NewMessageContract.Intent.OnBack) }) {
                Icon(painter = painterResource(DesignR.drawable.ic_arrow_left), contentDescription = stringResource(R.string.back), tint = colors.text)
            }
            Text(
                text = stringResource(R.string.new_message),
                color = colors.text,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
        if (uiState.openingUserId != null) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(2.dp), color = colors.primary, trackColor = colors.bg)
        }

        LazyColumn(modifier = Modifier.fillMaxSize().navigationBarsPadding()) {
            item(key = "new-group") {
                ActionRow(
                    tile = { BrandTile(icon = DesignR.drawable.ic_users, size = 48.dp, cornerRadius = 16.dp, iconSize = 22.dp) },
                    label = stringResource(R.string.new_group),
                    onClick = { onEventDispatcher(NewMessageContract.Intent.OnNewGroup) }
                )
            }
            item(key = "new-contact") {
                ActionRow(
                    tile = { ColorTile(icon = DesignR.drawable.ic_user_plus, color = ContactTileColor) },
                    label = stringResource(R.string.new_contact),
                    onClick = { onEventDispatcher(NewMessageContract.Intent.OnNewContact) }
                )
                Spacer(modifier = Modifier.fillMaxWidth().padding(top = 4.dp).height(8.dp).background(colors.surface))
            }

            item(key = "contacts-label") { SectionLabel(text = stringResource(R.string.contacts)) }
            if (uiState.isLoaded && uiState.contacts.isEmpty()) {
                item(key = "empty") {
                    Text(
                        text = stringResource(R.string.no_contacts),
                        color = colors.text2,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(24.dp)
                    )
                }
            }
            items(items = uiState.contacts, key = { it.id }) { user ->
                ContactRow(
                    user = user,
                    status = formatPresence(user.online, user.lastSeenAt, resources),
                    modifier = Modifier.combinedClickable(
                        onClick = { onEventDispatcher(NewMessageContract.Intent.OnContactClick(user)) },
                        onLongClick = { removeTarget = user }
                    )
                )
            }
        }
    }

    removeTarget?.let { user ->
        SwiftDialog(
            title = stringResource(R.string.remove_contact_title, user.displayName),
            confirmText = stringResource(R.string.remove),
            dismissText = stringResource(R.string.cancel),
            icon = DesignR.drawable.ic_user_minus,
            destructive = true,
            onConfirm = {
                removeTarget = null
                onEventDispatcher(NewMessageContract.Intent.OnRemoveContact(user))
            },
            onDismiss = { removeTarget = null }
        )
    }
}

/** Kontakt plitkasi rangi (profil sozlamalaridagi "username" yashili bilan bir xil). */
private val ContactTileColor = Color(0xFF3D8A4F)

/** 68dp amal qatori: plitka 48 · nom (16/600, primary). */
@Composable
private fun ActionRow(tile: @Composable () -> Unit, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(68.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        tile()
        Text(text = label, color = SwiftTheme.colors.primary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** BrandTile kabi, lekin ixtiyoriy rangda (48, r16, oq ikonka). */
@Composable
private fun ColorTile(icon: Int, color: Color) {
    Box(
        modifier = Modifier.size(48.dp).background(color, RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(painter = painterResource(icon), contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
    }
}

/** 68dp kontakt qatori: avatar 48 (+ online nuqta) · ism · holat ("online" primary, aks holda "oxirgi marta…"). */
@Composable
private fun ContactRow(user: User, status: String, modifier: Modifier = Modifier) {
    val colors = SwiftTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(68.dp)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar(name = user.displayName, colorSeed = user.id, size = 48.dp, online = user.online)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = user.displayName, color = colors.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(text = status, color = if (user.online) colors.primary else colors.text2, fontSize = 14.sp, maxLines = 1)
        }
    }
}

// ---------------- Preview'lar ----------------
// Kontaktlar bor va bo'sh holat, yorug'/tungi temada.

private val PreviewContacts = listOf(
    User("1", "ali_valiyev", "Ali Valiyev", null, 0, null, online = true),
    User("2", "malika", "Malika Yusupova", null, 0, null, lastSeenAt = System.currentTimeMillis() - 600_000)
)

@Composable
private fun NewMessagePreview(darkTheme: Boolean, contacts: List<User>) {
    SwiftChatTheme(darkTheme = darkTheme) {
        NewMessageContent(uiState = NewMessageContract.UiState(contacts = contacts, isLoaded = true), onEventDispatcher = {})
    }
}

@Preview(name = "Contacts · Light", showSystemUi = true)
@Composable
private fun NewMessageLightPreview() = NewMessagePreview(false, PreviewContacts)

@Preview(name = "Contacts · Dark", showSystemUi = true)
@Composable
private fun NewMessageDarkPreview() = NewMessagePreview(true, PreviewContacts)

@Preview(name = "Empty · Light", showSystemUi = true)
@Composable
private fun NewMessageEmptyPreview() = NewMessagePreview(false, emptyList())
