package uz.relay.feature.profile.user

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import uz.relay.core.designsystem.component.SwiftDialog
import uz.relay.feature.profile.components.DangerRow
import uz.relay.feature.profile.components.SettingRow
import uz.relay.core.designsystem.component.SwiftSnackbarHost
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import uz.relay.core.designsystem.R as DesignR
import uz.relay.core.designsystem.theme.SwiftChatTheme
import uz.relay.core.designsystem.theme.SwiftTheme
import uz.relay.core.designsystem.util.formatPresence
import uz.relay.domain.model.User
import uz.relay.feature.profile.R
import uz.relay.feature.profile.components.InfoRow
import uz.relay.feature.profile.components.ProfileActionCard
import uz.relay.feature.profile.components.ProfileCard
import uz.relay.feature.profile.components.ProfileHeader
import uz.relay.feature.profile.components.ProfileTopBar
import uz.relay.feature.profile.components.TileColors
import uz.relay.feature.profile.util.messageRes

/**
 * Foydalanuvchi profili ekrani (Nav3 entry: UserProfileKey). ViewModel AssistedInject factory bilan `userId`ni
 * kalitdan oladi; SideEffect'lar Snackbar'ga aylanadi, chizish [UserProfileContent]da.
 */
@Composable
internal fun UserProfileScreen(userId: String) {
    val viewModel = hiltViewModel<UserProfileViewModel, UserProfileViewModel.Factory>(
        creationCallback = { factory -> factory.create(userId) }
    )
    val uiState by viewModel.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    viewModel.collectSideEffect { sideEffect ->
        when (sideEffect) {
            is UserProfileContract.SideEffect.ShowError ->
                snackbarHostState.showSnackbar(context.getString(sideEffect.error.messageRes()))
            UserProfileContract.SideEffect.ContactAdded ->
                snackbarHostState.showSnackbar(context.getString(R.string.contact_added))
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        UserProfileContent(userId = userId, uiState = uiState, onEventDispatcher = viewModel::onEventDispatcher)
        SwiftSnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

/** Avatar · ism · holat, "Xabar" va "Ovozsiz qilish" kartalari, username va kontaktga qo'shish/o'chirish. */
@Composable
internal fun UserProfileContent(
    userId: String,
    uiState: UserProfileContract.UiState,
    onEventDispatcher: (UserProfileContract.Intent) -> Unit
) {
    val colors = SwiftTheme.colors
    val resources = LocalContext.current.resources
    val user = uiState.user
    var confirmRemoveContact by rememberSaveable { mutableStateOf(false) }
    val status = user?.let { formatPresence(it.online, it.lastSeenAt, resources) }.orEmpty()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.page)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        ProfileTopBar(onBack = { onEventDispatcher(UserProfileContract.Intent.OnBack) })

        // Avatar rangi id'dan — profil hali yuklanmagan bo'lsa ham rang to'g'ri bo'ladi.
        ProfileHeader(
            name = user?.displayName,
            colorSeed = userId,
            status = status,
            statusColor = if (user?.online == true) colors.primary else colors.text2
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
        ) {
            ProfileActionCard(
                icon = DesignR.drawable.ic_message_circle,
                label = stringResource(R.string.message),
                primary = true,
                onClick = { onEventDispatcher(UserProfileContract.Intent.OnMessage) }
            )
            ProfileActionCard(
                icon = if (uiState.muted) DesignR.drawable.ic_bell_off else DesignR.drawable.ic_bell,
                label = stringResource(if (uiState.muted) R.string.unmute else R.string.mute),
                enabled = !uiState.isBusy,
                onClick = { onEventDispatcher(UserProfileContract.Intent.OnToggleMute) }
            )
        }

        user?.username?.let { username ->
            ProfileCard {
                InfoRow(icon = DesignR.drawable.ic_at_sign, tileColor = TileColors.Username, value = username, caption = stringResource(R.string.username))
            }
        }

        // Kontaktlar (faqat shu qurilmada): qo'shish — oddiy qator, o'chirish — qizil qator + tasdiq dialogi.
        if (user != null) {
            ProfileCard(modifier = Modifier.padding(top = 12.dp, bottom = 16.dp)) {
                if (uiState.isContact) {
                    DangerRow(
                        icon = DesignR.drawable.ic_user_minus,
                        label = stringResource(R.string.remove_from_contacts),
                        onClick = { confirmRemoveContact = true }
                    )
                } else {
                    SettingRow(
                        icon = DesignR.drawable.ic_user_plus,
                        tileColor = TileColors.Username,
                        label = stringResource(R.string.add_to_contacts),
                        onClick = { onEventDispatcher(UserProfileContract.Intent.OnToggleContact) }
                    ) {}
                }
            }
        }
    }

    if (confirmRemoveContact) {
        SwiftDialog(
            title = stringResource(R.string.remove_contact_title, user?.displayName.orEmpty()),
            confirmText = stringResource(R.string.remove),
            dismissText = stringResource(R.string.cancel),
            icon = DesignR.drawable.ic_user_minus,
            destructive = true,
            onConfirm = {
                confirmRemoveContact = false
                onEventDispatcher(UserProfileContract.Intent.OnToggleContact)
            },
            onDismiss = { confirmRemoveContact = false }
        )
    }
}

// ---------------- Preview'lar ----------------
// Yorug' va qorong'i temada, "oxirgi marta 5 daqiqa oldin" holatidagi foydalanuvchi.

private val PreviewUser = User(
    id = "u1",
    username = "jasur_aliyev",
    displayName = "Jasur Aliyev",
    avatarMediaId = null,
    avatarVersion = 0,
    phone = null,
    online = false,
    lastSeenAt = System.currentTimeMillis() - 5 * 60_000
)

@Composable
private fun UserProfilePreview(darkTheme: Boolean) {
    SwiftChatTheme(darkTheme = darkTheme) {
        UserProfileContent(userId = "u1", uiState = UserProfileContract.UiState(user = PreviewUser), onEventDispatcher = {})
    }
}

@Preview(name = "Light", showSystemUi = true)
@Composable
private fun UserProfileLightPreview() = UserProfilePreview(false)

@Preview(name = "Dark", showSystemUi = true)
@Composable
private fun UserProfileDarkPreview() = UserProfilePreview(true)
