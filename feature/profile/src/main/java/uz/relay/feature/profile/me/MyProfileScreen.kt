package uz.relay.feature.profile.me

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import uz.relay.core.designsystem.R as DesignR
import uz.relay.core.designsystem.theme.SwiftChatTheme
import uz.relay.core.designsystem.theme.SwiftTheme
import uz.relay.domain.model.AppLanguage
import uz.relay.domain.model.ConnectionStatus
import uz.relay.domain.model.ThemeMode
import uz.relay.domain.model.User
import uz.relay.feature.profile.R
import uz.relay.feature.profile.components.InfoRow
import uz.relay.feature.profile.components.ProfileCard
import uz.relay.feature.profile.components.ProfileHeader
import uz.relay.feature.profile.components.ProfileTopBar
import uz.relay.feature.profile.components.SettingRow
import uz.relay.feature.profile.components.SwiftSwitch
import uz.relay.feature.profile.components.TileColors
import uz.relay.feature.profile.util.formatPhone
import uz.relay.feature.profile.util.messageRes

@Composable
internal fun MyProfileScreen(viewModel: MyProfileViewModel = hiltViewModel()) {
    val uiState by viewModel.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    viewModel.collectSideEffect { sideEffect ->
        when (sideEffect) {
            is MyProfileContract.SideEffect.ShowError ->
                snackbarHostState.showSnackbar(context.getString(sideEffect.error.messageRes()))
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        MyProfileContent(uiState = uiState, onEventDispatcher = viewModel::onEventDispatcher)
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 80.dp)
        )
    }
}

@Composable
private fun MyProfileContent(
    uiState: MyProfileContract.UiState,
    onEventDispatcher: (MyProfileContract.Intent) -> Unit
) {
    val colors = SwiftTheme.colors
    val me = uiState.me
    val darkChecked = uiState.themeMode == ThemeMode.DARK
    // Dialog faqat ko'rinishga tegishli; burilishda yo'qolmasin.
    var showLogoutDialog by rememberSaveable { mutableStateOf(false) }
    var showLanguageSheet by rememberSaveable { mutableStateOf(false) }

    val (status, statusColor) = when (uiState.connectionStatus) {
        ConnectionStatus.CONNECTED, ConnectionStatus.UPDATING -> stringResource(R.string.online) to colors.primary
        ConnectionStatus.CONNECTING -> stringResource(R.string.connecting) to colors.text2
        ConnectionStatus.OFFLINE -> stringResource(R.string.no_internet) to colors.text2
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.page)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        ProfileTopBar(onBack = { onEventDispatcher(MyProfileContract.Intent.OnBack) }) {
            IconButton(onClick = { onEventDispatcher(MyProfileContract.Intent.OnEdit) }) {
                Icon(
                    painter = painterResource(DesignR.drawable.ic_pencil),
                    contentDescription = stringResource(R.string.edit_profile),
                    tint = colors.text,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            ProfileHeader(name = me?.displayName, colorSeed = me?.id.orEmpty(), status = status, statusColor = statusColor)

            ProfileCard {
                me?.phone?.let { phone ->
                    InfoRow(icon = DesignR.drawable.ic_phone, tileColor = TileColors.Phone, value = formatPhone(phone), caption = stringResource(R.string.phone))
                }
                me?.username?.let { username ->
                    InfoRow(icon = DesignR.drawable.ic_at_sign, tileColor = TileColors.Username, value = username, caption = stringResource(R.string.username))
                }
            }

            ProfileCard(modifier = Modifier.padding(top = 12.dp, bottom = 16.dp)) {
                SettingRow(
                    icon = DesignR.drawable.ic_bell,
                    tileColor = TileColors.Notifications,
                    label = stringResource(R.string.notifications),
                    onClick = { onEventDispatcher(MyProfileContract.Intent.OnNotificationsChange(!uiState.notificationsEnabled)) }
                ) {
                    SwiftSwitch(
                        checked = uiState.notificationsEnabled,
                        onCheckedChange = { onEventDispatcher(MyProfileContract.Intent.OnNotificationsChange(it)) }
                    )
                }
                SettingRow(
                    icon = DesignR.drawable.ic_moon,
                    tileColor = TileColors.DarkMode,
                    label = stringResource(R.string.dark_mode),
                    onClick = { onEventDispatcher(MyProfileContract.Intent.OnDarkModeChange(!darkChecked)) }
                ) {
                    SwiftSwitch(
                        checked = darkChecked,
                        onCheckedChange = { onEventDispatcher(MyProfileContract.Intent.OnDarkModeChange(it)) }
                    )
                }
                SettingRow(
                    icon = DesignR.drawable.ic_globe,
                    tileColor = TileColors.Language,
                    label = stringResource(R.string.language),
                    onClick = { showLanguageSheet = true }
                ) {
                    Text(text = stringResource(uiState.language.labelRes()), color = colors.text2, fontSize = 14.sp)
                }
            }
        }

        LogoutButton(loading = uiState.loggingOut, onClick = { showLogoutDialog = true })
    }

    if (showLanguageSheet) {
        LanguageSheet(
            selected = uiState.language,
            onSelect = { language ->
                showLanguageSheet = false
                if (language != uiState.language) onEventDispatcher(MyProfileContract.Intent.OnLanguageChange(language))
            },
            onDismiss = { showLanguageSheet = false }
        )
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            containerColor = colors.menu,
            title = { Text(stringResource(R.string.logout_title), color = colors.text) },
            text = { Text(stringResource(R.string.logout_text), color = colors.text2) },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutDialog = false
                    onEventDispatcher(MyProfileContract.Intent.OnLogout)
                }) {
                    Text(stringResource(R.string.logout), color = colors.error, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text(stringResource(R.string.cancel), color = colors.primary)
                }
            }
        )
    }
}

/** Til nomi o'z tilida ("Русский" — ruscha interfeysda ham, o'zbekchada ham). */
private fun AppLanguage.labelRes(): Int = when (this) {
    AppLanguage.UZ -> R.string.lang_uz
    AppLanguage.RU -> R.string.lang_ru
    AppLanguage.EN -> R.string.lang_en
}

/** Uchta til, tanlangani yonida ✓ (primary). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguageSheet(selected: AppLanguage, onSelect: (AppLanguage) -> Unit, onDismiss: () -> Unit) {
    val colors = SwiftTheme.colors
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = colors.bg,
        scrimColor = colors.scrim,
        dragHandle = { BottomSheetDefaults.DragHandle(color = colors.outline, width = 32.dp, height = 4.dp) }
    ) {
        Column(modifier = Modifier.navigationBarsPadding().padding(bottom = 16.dp)) {
            Text(
                text = stringResource(R.string.language),
                color = colors.text,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 4.dp, bottom = 8.dp)
            )
            AppLanguage.entries.forEach { language ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clickable { onSelect(language) }
                        .padding(horizontal = 24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(language.labelRes()),
                        color = colors.text,
                        fontSize = 16.sp,
                        fontWeight = if (language == selected) FontWeight.SemiBold else FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    if (language == selected) {
                        Icon(painter = painterResource(DesignR.drawable.ic_check), contentDescription = null, tint = colors.primary, modifier = Modifier.size(22.dp))
                    }
                }
            }
        }
    }
}

/** "Chiqish": karta ko'rinishidagi 56dp tugma, qizil matn + ikonka. Chiqish ketayotganda — progress. */
@Composable
private fun LogoutButton(loading: Boolean, onClick: () -> Unit) {
    val colors = SwiftTheme.colors
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 20.dp)
            .height(56.dp)
            .shadow(elevation = if (colors.cardBorder == Color.Transparent) 2.dp else 0.dp, shape = shape)
            .clip(shape)
            .background(colors.card, shape)
            .border(1.dp, colors.cardBorder, shape)
            .clickable(enabled = !loading, onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (loading) {
            CircularProgressIndicator(color = colors.error, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
        } else {
            Icon(painter = painterResource(DesignR.drawable.ic_log_out), contentDescription = null, tint = colors.error, modifier = Modifier.size(20.dp))
        }
        Text(text = stringResource(R.string.logout), color = colors.error, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}

// ---------------- Preview'lar ----------------

private val PreviewMe = User("me", "dawran_n", "Dawran", null, 0, "+998901234567", online = true)

@Composable
private fun MyProfilePreview(darkTheme: Boolean) {
    SwiftChatTheme(darkTheme = darkTheme) {
        MyProfileContent(
            uiState = MyProfileContract.UiState(me = PreviewMe, themeMode = if (darkTheme) ThemeMode.DARK else ThemeMode.LIGHT),
            onEventDispatcher = {}
        )
    }
}

@Preview(name = "Light", showSystemUi = true)
@Composable
private fun MyProfileLightPreview() = MyProfilePreview(false)

@Preview(name = "Dark", showSystemUi = true)
@Composable
private fun MyProfileDarkPreview() = MyProfilePreview(true)
