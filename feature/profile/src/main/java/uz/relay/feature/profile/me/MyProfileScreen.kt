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
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.platform.LocalResources
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
import uz.relay.core.designsystem.component.SwiftDialog
import uz.relay.core.designsystem.component.SwiftSnackbarHost
import uz.relay.core.designsystem.component.SwiftSwitch
import uz.relay.core.designsystem.theme.SwiftChatTheme
import uz.relay.core.designsystem.theme.SwiftTheme
import uz.relay.domain.model.AppLanguage
import uz.relay.domain.model.ConnectionStatus
import uz.relay.domain.model.ThemeMode
import uz.relay.domain.model.User
import uz.relay.feature.profile.R
import uz.relay.feature.profile.components.DangerRow
import uz.relay.feature.profile.components.InfoRow
import uz.relay.feature.profile.components.ProfileCard
import uz.relay.feature.profile.components.ProfileHeader
import uz.relay.feature.profile.components.ProfileTopBar
import uz.relay.feature.profile.components.SectionLabel
import uz.relay.feature.profile.components.SettingRow
import uz.relay.feature.profile.components.TileColors
import uz.relay.feature.profile.util.formatPhone
import uz.relay.feature.profile.util.messageRes

/**
 * "Mening profilim" ekrani (Nav3 entry: MyProfileKey). Stateful qism: ViewModel'ni `hiltViewModel()` bilan
 * oladi, SideEffect'larni Snackbar'ga aylantiradi; chizish [MyProfileContent]da (Preview ViewModel'siz ishlaydi).
 */
@Composable
internal fun MyProfileScreen(viewModel: MyProfileViewModel = hiltViewModel()) {
    val uiState by viewModel.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current

    viewModel.collectSideEffect { sideEffect ->
        when (sideEffect) {
            is MyProfileContract.SideEffect.ShowError ->
                snackbarHostState.showSnackbar(resources.getString(sideEffect.error.messageRes()))
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        MyProfileContent(uiState = uiState, onEventDispatcher = viewModel::onEventDispatcher)
        SwiftSnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

/**
 * Profil UI'si: sarlavha, ism + ulanish holati, "Hisob" (telefon/username), "Sozlamalar" va alohida kartada "Chiqish".
 * Til tanlash sheet'i va chiqish dialogi — vaqtinchalik ko'rinish holati, shuning uchun ViewModel'da emas.
 */
@Composable
internal fun MyProfileContent(
    uiState: MyProfileContract.UiState,
    onEventDispatcher: (MyProfileContract.Intent) -> Unit
) {
    val colors = SwiftTheme.colors
    val me = uiState.me
    // Dialog faqat ko'rinishga tegishli; burilishda yo'qolmasin.
    var showLogoutDialog by rememberSaveable { mutableStateOf(false) }
    var showLanguageSheet by rememberSaveable { mutableStateOf(false) }
    var showThemeSheet by rememberSaveable { mutableStateOf(false) }

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

            SectionLabel(text = stringResource(R.string.section_account))
            ProfileCard {
                me?.phone?.let { phone ->
                    InfoRow(icon = DesignR.drawable.ic_phone, tileColor = TileColors.Phone, value = formatPhone(phone), caption = stringResource(R.string.phone))
                }
                me?.username?.let { username ->
                    InfoRow(icon = DesignR.drawable.ic_at_sign, tileColor = TileColors.Username, value = username, caption = stringResource(R.string.username))
                }
            }

            SectionLabel(text = stringResource(R.string.section_settings), modifier = Modifier.padding(top = 20.dp))
            ProfileCard {
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
                // Uch variant (tizim / kunduzgi / tungi) — switch yetmaydi, til kabi tanlash sheet'i.
                SettingRow(
                    icon = DesignR.drawable.ic_moon,
                    tileColor = TileColors.DarkMode,
                    label = stringResource(R.string.theme),
                    onClick = { showThemeSheet = true }
                ) {
                    Text(text = stringResource(uiState.themeMode.labelRes()), color = colors.text2, fontSize = 14.sp)
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

            // "Chiqish" — sozlamalar ostidagi alohida kartada (ekran pastiga yopishtirilmagan), qizil plitka bilan.
            ProfileCard(modifier = Modifier.padding(top = 20.dp, bottom = 24.dp)) {
                DangerRow(
                    icon = DesignR.drawable.ic_log_out,
                    label = stringResource(R.string.logout),
                    loading = uiState.loggingOut,
                    onClick = { showLogoutDialog = true }
                )
            }
        }
    }

    if (showLanguageSheet) {
        OptionSheet(
            title = stringResource(R.string.language),
            options = AppLanguage.entries,
            selected = uiState.language,
            label = { it.labelRes() },
            onSelect = { language ->
                showLanguageSheet = false
                if (language != uiState.language) onEventDispatcher(MyProfileContract.Intent.OnLanguageChange(language))
            },
            onDismiss = { showLanguageSheet = false }
        )
    }

    if (showThemeSheet) {
        OptionSheet(
            title = stringResource(R.string.theme),
            options = ThemeMode.entries,
            selected = uiState.themeMode,
            label = { it.labelRes() },
            onSelect = { mode ->
                showThemeSheet = false
                if (mode != uiState.themeMode) onEventDispatcher(MyProfileContract.Intent.OnThemeChange(mode))
            },
            onDismiss = { showThemeSheet = false }
        )
    }

    // Faqat qisqa savol — ortiqcha izohsiz (foydalanuvchi talabi).
    if (showLogoutDialog) {
        SwiftDialog(
            title = stringResource(R.string.logout_title),
            confirmText = stringResource(R.string.logout),
            dismissText = stringResource(R.string.cancel),
            icon = DesignR.drawable.ic_log_out,
            destructive = true,
            onConfirm = {
                showLogoutDialog = false
                onEventDispatcher(MyProfileContract.Intent.OnLogout)
            },
            onDismiss = { showLogoutDialog = false }
        )
    }
}

/** Til nomi o'z tilida ("Русский" — ruscha interfeysda ham, o'zbekchada ham). */
private fun AppLanguage.labelRes(): Int = when (this) {
    AppLanguage.UZ -> R.string.lang_uz
    AppLanguage.RU -> R.string.lang_ru
    AppLanguage.EN -> R.string.lang_en
}

/** Tema nomi joriy tilda. */
private fun ThemeMode.labelRes(): Int = when (this) {
    ThemeMode.SYSTEM -> R.string.theme_system
    ThemeMode.LIGHT -> R.string.theme_light
    ThemeMode.DARK -> R.string.theme_dark
}

/** Bir nechta variantdan bittasini tanlash (til, tema): tanlangani yonida ✓ (primary). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> OptionSheet(
    title: String,
    options: List<T>,
    selected: T,
    label: (T) -> Int,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit
) {
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
                text = title,
                color = colors.text,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 4.dp, bottom = 8.dp)
            )
            options.forEach { option ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clickable { onSelect(option) }
                        .padding(horizontal = 24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(label(option)),
                        color = colors.text,
                        fontSize = 16.sp,
                        fontWeight = if (option == selected) FontWeight.SemiBold else FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    if (option == selected) {
                        Icon(painter = painterResource(DesignR.drawable.ic_check), contentDescription = null, tint = colors.primary, modifier = Modifier.size(22.dp))
                    }
                }
            }
        }
    }
}

// ---------------- Preview'lar ----------------
// Yorug' va qorong'i temada to'ldirilgan profil.

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
