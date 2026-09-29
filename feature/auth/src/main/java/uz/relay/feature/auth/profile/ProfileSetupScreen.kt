package uz.relay.feature.auth.profile

import uz.relay.core.designsystem.component.SwiftSnackbarHost
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import uz.relay.core.designsystem.R as DesignR
import uz.relay.core.designsystem.component.SwiftPrimaryButton
import uz.relay.core.designsystem.component.SwiftTextField
import uz.relay.core.designsystem.theme.SwiftChatTheme
import uz.relay.core.designsystem.theme.SwiftTheme
import uz.relay.feature.auth.R
import uz.relay.feature.auth.profile.components.AvatarPicker
import uz.relay.feature.auth.util.messageRes

/**
 * Profil sozlash ekrani (stateful qism).
 *
 * OTP'dan keyin yangi foydalanuvchi yoki profili to'ldirilmagan sessiya bilan Splash shu ekranni ochadi;
 * "Davom etish" muvaffaqiyatli bo'lsa Chats ekraniga o'tiladi. ViewModel'ga ulanadi, SideEffect'larni
 * snackbar orqali ko'rsatadi va chizishni stateless [ProfileSetupScreenContent] ga topshiradi.
 */
@Composable
internal fun ProfileSetupScreen(viewModel: ProfileSetupViewModel = hiltViewModel()) {
    val uiState by viewModel.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    viewModel.collectSideEffect { sideEffect ->
        when (sideEffect) {
            is ProfileSetupContract.SideEffect.ShowError ->
                snackbarHostState.showSnackbar(context.getString(sideEffect.error.messageRes()))
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        ProfileSetupScreenContent(
            uiState = uiState,
            onEventDispatcher = viewModel::onEventDispatcher
        )

        SwiftSnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

/**
 * Ekranning stateless UI qismi: [uiState] ni chizadi, harakatlarni [onEventDispatcher] orqali qaytaradi.
 * Preview'larda "to'g'ri" va "band" holatlarini ViewModel'siz ko'rsatish uchun ajratilgan.
 */
@Composable
private fun ProfileSetupScreenContent(
    uiState: ProfileSetupContract.UiState,
    onEventDispatcher: (ProfileSetupContract.Intent) -> Unit
) {
    val colors = SwiftTheme.colors
    val typography = SwiftTheme.typography
    val usernameError = uiState.usernameTaken

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .padding(start = 24.dp, end = 24.dp, top = 40.dp, bottom = 24.dp)
    ) {
        // Kichik ekranda klaviatura ochilganda maydonlar skroll bo'ladi, tugma esa pastda qoladi.
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = stringResource(R.string.profile_title),
                style = typography.displayTitle,
                color = colors.text,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            AvatarPicker(
                contentDescription = stringResource(R.string.pick_photo),
                modifier = Modifier
                    .padding(top = 25.dp)
                    .align(Alignment.CenterHorizontally)
            )

            SwiftTextField(
                value = uiState.name,
                onValueChange = { onEventDispatcher(ProfileSetupContract.Intent.OnNameChange(it)) },
                label = stringResource(R.string.name),
                modifier = Modifier.padding(top = 20.dp),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next
                )
            )

            SwiftTextField(
                value = uiState.username,
                onValueChange = { onEventDispatcher(ProfileSetupContract.Intent.OnUsernameChange(it)) },
                label = stringResource(R.string.username),
                modifier = Modifier.padding(top = 13.dp),
                isError = usernameError,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Ascii,
                    autoCorrectEnabled = false,
                    imeAction = ImeAction.Done
                ),
                leading = {
                    Text(text = "@", fontSize = 17.sp, color = colors.text2, modifier = Modifier.padding(end = 2.dp))
                },
                // Band bo'lsa xato ikonkasi, qoidaga mos bo'lsa yashil belgi, aks holda hech narsa.
                trailing = {
                    when {
                        usernameError -> Icon(
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

            Text(
                text = stringResource(if (usernameError) R.string.username_taken else R.string.username_rules),
                color = if (usernameError) colors.error else colors.text2,
                fontSize = 12.sp,
                fontWeight = if (usernameError) FontWeight.SemiBold else FontWeight.Normal,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 6.dp)
            )

            // Takliflar faqat username band bo'lganda paydo bo'ladi.
            if (uiState.suggestions.isNotEmpty()) {
                Row(
                    modifier = Modifier.padding(top = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    uiState.suggestions.forEach { suggestion ->
                        SuggestionChip(
                            text = suggestion,
                            onClick = { onEventDispatcher(ProfileSetupContract.Intent.OnSuggestionClick(suggestion)) }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        SwiftPrimaryButton(
            text = stringResource(R.string.continue_button),
            onClick = { onEventDispatcher(ProfileSetupContract.Intent.OnContinue) },
            enabled = uiState.continueEnabled,
            loading = uiState.saving
        )
    }
}

/** Bosilganda username'ni taklif qilingan qiymat bilan almashtiradigan chip. */
@Composable
private fun SuggestionChip(text: String, onClick: () -> Unit) {
    val colors = SwiftTheme.colors
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = Modifier
            .height(34.dp)
            .border(1.dp, colors.outline, shape)
            .background(colors.bg, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = colors.text, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

// Preview'lar: to'g'ri va band username holatlari, yorug'/qorong'i tema.
@Composable
private fun ProfileSetupPreview(darkTheme: Boolean, state: ProfileSetupContract.UiState) {
    SwiftChatTheme(darkTheme = darkTheme) {
        ProfileSetupScreenContent(uiState = state, onEventDispatcher = {})
    }
}

@Preview(name = "Valid · Light", showSystemUi = true)
@Composable
private fun ProfileSetupValidLightPreview() =
    ProfileSetupPreview(false, ProfileSetupContract.UiState(name = "Dawran", username = "dawran_n"))

@Preview(name = "Valid · Dark", showSystemUi = true)
@Composable
private fun ProfileSetupValidDarkPreview() =
    ProfileSetupPreview(true, ProfileSetupContract.UiState(name = "Dawran", username = "dawran_n"))

@Preview(name = "Taken · Light", showSystemUi = true)
@Composable
private fun ProfileSetupTakenLightPreview() = ProfileSetupPreview(
    false,
    ProfileSetupContract.UiState(
        name = "Dawran",
        username = "dawran",
        usernameTaken = true,
        suggestions = listOf("dawran_dev", "dawran01")
    )
)

@Preview(name = "Taken · Dark", showSystemUi = true)
@Composable
private fun ProfileSetupTakenDarkPreview() = ProfileSetupPreview(
    true,
    ProfileSetupContract.UiState(
        name = "Dawran",
        username = "dawran",
        usernameTaken = true,
        suggestions = listOf("dawran_dev", "dawran01")
    )
)
