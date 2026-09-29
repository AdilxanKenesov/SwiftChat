package uz.relay.feature.profile.edit

import uz.relay.core.designsystem.component.SwiftSnackbarHost
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import uz.relay.core.designsystem.R as DesignR
import uz.relay.core.designsystem.component.Avatar
import uz.relay.core.designsystem.component.SwiftPrimaryButton
import uz.relay.core.designsystem.component.SwiftTextField
import uz.relay.core.designsystem.theme.SwiftChatTheme
import uz.relay.core.designsystem.theme.SwiftTheme
import uz.relay.feature.profile.R
import uz.relay.feature.profile.util.messageRes

/**
 * Profilni tahrirlash ekrani (Nav3 entry: EditProfileKey). Runtime argument yo'q, shuning uchun ViewModel oddiy
 * `hiltViewModel()` bilan olinadi. SideEffect'lar Snackbar'ga aylanadi; chizish [EditProfileContent]da.
 */
@Composable
internal fun EditProfileScreen(viewModel: EditProfileViewModel = hiltViewModel()) {
    val uiState by viewModel.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    viewModel.collectSideEffect { sideEffect ->
        when (sideEffect) {
            is EditProfileContract.SideEffect.ShowError ->
                snackbarHostState.showSnackbar(context.getString(sideEffect.error.messageRes()))
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        EditProfileContent(uiState = uiState, onEventDispatcher = viewModel::onEventDispatcher)
        SwiftSnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

/** ProfileSetup bilan bir xil maydonlar va qoidalar, lekin orqaga tugmasi va "Saqlash" bilan. */
@Composable
private fun EditProfileContent(
    uiState: EditProfileContract.UiState,
    onEventDispatcher: (EditProfileContract.Intent) -> Unit
) {
    val colors = SwiftTheme.colors
    val usernameError = uiState.usernameTaken
    // Qoida ("3–32 belgi…") faqat foydalanuvchi qoidaga mos KELMAYDIGAN username yozganda ko'rinadi —
    // bo'sh maydonda va to'g'ri yozilganda ortiqcha matn ko'rsatilmaydi.
    val rulesBroken = uiState.username.isNotEmpty() && !uiState.usernameValid

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { onEventDispatcher(EditProfileContract.Intent.OnBack) }) {
                Icon(painter = painterResource(DesignR.drawable.ic_arrow_left), contentDescription = stringResource(R.string.back), tint = colors.text)
            }
            Text(
                text = stringResource(R.string.edit_profile),
                color = colors.text,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 4.dp)
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            // Avatar yozilayotgan ism bilan birga o'zgaradi (bosh harflar) — natija oldindan ko'rinadi.
            Avatar(
                name = uiState.name,
                colorSeed = uiState.userId,
                size = 88.dp,
                modifier = Modifier
                    .padding(top = 12.dp)
                    .align(Alignment.CenterHorizontally)
            )

            SwiftTextField(
                value = uiState.name,
                onValueChange = { onEventDispatcher(EditProfileContract.Intent.OnNameChange(it)) },
                label = stringResource(R.string.name),
                modifier = Modifier.padding(top = 24.dp),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next)
            )

            SwiftTextField(
                value = uiState.username,
                onValueChange = { onEventDispatcher(EditProfileContract.Intent.OnUsernameChange(it)) },
                label = stringResource(R.string.username),
                modifier = Modifier.padding(top = 13.dp),
                isError = usernameError || rulesBroken,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, autoCorrectEnabled = false, imeAction = ImeAction.Done),
                leading = {
                    Text(text = "@", fontSize = 17.sp, color = colors.text2, modifier = Modifier.padding(end = 2.dp))
                },
                trailing = {
                    when {
                        usernameError || rulesBroken -> Icon(
                            painter = painterResource(DesignR.drawable.ic_alert_circle),
                            contentDescription = null,
                            tint = colors.error,
                            modifier = Modifier.size(22.dp)
                        )

                        uiState.usernameValid -> Icon(
                            painter = painterResource(DesignR.drawable.ic_check),
                            contentDescription = null,
                            tint = colors.online,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            )

            if (usernameError || rulesBroken) {
                Text(
                    text = stringResource(if (usernameError) R.string.username_taken else R.string.username_rules),
                    color = colors.error,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 6.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        SwiftPrimaryButton(
            text = stringResource(R.string.save),
            onClick = { onEventDispatcher(EditProfileContract.Intent.OnSave) },
            enabled = uiState.saveEnabled,
            loading = uiState.saving,
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 24.dp)
        )
    }
}

// ---------------- Preview'lar ----------------
// O'zgartirilgan forma (yorug'/qorong'i) va "username band" xatosi.

private val PreviewState = EditProfileContract.UiState(
    loaded = true,
    userId = "me",
    name = "Dawran N",
    username = "dawran_n",
    initialName = "Dawran",
    initialUsername = "dawran_n"
)

@Composable
private fun EditProfilePreview(darkTheme: Boolean, state: EditProfileContract.UiState) {
    SwiftChatTheme(darkTheme = darkTheme) { EditProfileContent(uiState = state, onEventDispatcher = {}) }
}

@Preview(name = "Changed · Light", showSystemUi = true)
@Composable
private fun EditProfileLightPreview() = EditProfilePreview(false, PreviewState)

@Preview(name = "Changed · Dark", showSystemUi = true)
@Composable
private fun EditProfileDarkPreview() = EditProfilePreview(true, PreviewState)

@Preview(name = "Taken · Light", showSystemUi = true)
@Composable
private fun EditProfileTakenPreview() = EditProfilePreview(false, PreviewState.copy(username = "dawran", usernameTaken = true))
