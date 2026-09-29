package uz.relay.feature.auth.phone

import uz.relay.core.designsystem.component.SwiftSnackbarHost
import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import uz.relay.core.designsystem.R as DesignR
import uz.relay.core.designsystem.component.BrandTile
import uz.relay.core.designsystem.component.SwiftPrimaryButton
import uz.relay.core.designsystem.component.SwiftTextField
import uz.relay.core.designsystem.theme.SwiftChatTheme
import uz.relay.core.designsystem.theme.SwiftTheme
import uz.relay.feature.auth.R
import uz.relay.feature.auth.phone.components.TelegramLinkSheet
import uz.relay.feature.auth.util.LocalPhoneTransformation
import uz.relay.feature.auth.util.UZ_PREFIX
import uz.relay.feature.auth.util.formatFullPhone
import uz.relay.feature.auth.util.messageRes

/**
 * Telefon raqamini kiritish ekrani (stateful qism).
 *
 * Splash sessiya topmasa shu ekranni ochadi; "Kod olish" muvaffaqiyatli bo'lsa OTP ekraniga o'tiladi.
 * Bu composable faqat ViewModel'ga ulanadi: state'ni yig'adi, SideEffect'larni (snackbar, havola ochish)
 * bajaradi va chizishni stateless [PhoneScreenContent] ga topshiradi.
 */
@Composable
internal fun PhoneScreen(viewModel: PhoneViewModel = hiltViewModel()) {
    val uiState by viewModel.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // collectSideEffect lifecycle'ga bog'langan (repeatOnLifecycle STARTED) - fonda hodisa yo'qolmaydi va UI yo'qligida ishlamaydi.
    viewModel.collectSideEffect { sideEffect ->
        when (sideEffect) {
            is PhoneContract.SideEffect.ShowError ->
                snackbarHostState.showSnackbar(context.getString(sideEffect.error.messageRes()))

            is PhoneContract.SideEffect.OpenUrl -> try {
                context.startActivity(Intent(Intent.ACTION_VIEW, sideEffect.url.toUri()))
            // Qurilmada havolani ochadigan ilova bo'lmasligi mumkin.
            } catch (_: ActivityNotFoundException) {
                snackbarHostState.showSnackbar(context.getString(R.string.error_unknown))
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        PhoneScreenContent(
            uiState = uiState,
            onEventDispatcher = viewModel::onEventDispatcher
        )

        SwiftSnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }

    // Bot havolasi bor bo'lsagina Telegram'ni bog'lash sheet'i ko'rsatiladi.
    uiState.botUrl?.let {
        TelegramLinkSheet(
            phone = formatFullPhone(UZ_PREFIX + uiState.digits),
            loading = uiState.loading,
            onOpenBot = { viewModel.onEventDispatcher(PhoneContract.Intent.OnOpenBot) },
            onResend = { viewModel.onEventDispatcher(PhoneContract.Intent.OnResendCode) },
            onDismiss = { viewModel.onEventDispatcher(PhoneContract.Intent.OnDismissTelegramSheet) }
        )
    }
}

/**
 * Ekranning stateless UI qismi: faqat [uiState] ni chizadi va harakatlarni [onEventDispatcher] orqali qaytaradi.
 * ViewModel'siz bo'lgani uchun Preview'larda va UI testlarda to'g'ridan-to'g'ri ishlatiladi.
 */
@Composable
private fun PhoneScreenContent(
    uiState: PhoneContract.UiState,
    onEventDispatcher: (PhoneContract.Intent) -> Unit
) {
    val colors = SwiftTheme.colors
    val typography = SwiftTheme.typography

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .padding(start = 24.dp, end = 24.dp, top = 48.dp, bottom = 24.dp)
    ) {
        BrandTile(icon = DesignR.drawable.ic_phone)

        Text(
            text = stringResource(R.string.phone_title),
            style = typography.displayTitle,
            color = colors.text,
            modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
        )
        Text(
            text = stringResource(R.string.phone_subtitle),
            style = typography.supporting.copy(lineHeight = 22.sp),
            color = colors.text2
        )

        SwiftTextField(
            value = uiState.digits,
            onValueChange = { onEventDispatcher(PhoneContract.Intent.OnPhoneChange(it)) },
            label = stringResource(R.string.phone_label),
            modifier = Modifier.padding(top = 27.dp),
            textStyle = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.6.sp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onEventDispatcher(PhoneContract.Intent.OnGetCode) }),
            // State'da faqat raqamlar, bo'shliqlar faqat ko'rinishda qo'shiladi.
            visualTransformation = LocalPhoneTransformation,
            leading = {
                Text(text = UZ_PREFIX, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = colors.text)
                // +998 va raqam orasidagi vertikal ajratkich chiziq.
                Box(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .size(width = 1.dp, height = 24.dp)
                        .background(colors.outline)
                )
            }
        )

        Spacer(modifier = Modifier.weight(1f))

        SwiftPrimaryButton(
            text = stringResource(R.string.get_code),
            onClick = { onEventDispatcher(PhoneContract.Intent.OnGetCode) },
            enabled = uiState.continueEnabled,
            // Sheet ochiq bo'lsa yuklanish sheet ichidagi tugmada ko'rsatiladi, bu yerda emas.
            loading = uiState.loading && uiState.botUrl == null
        )
    }
}

// Preview'lar: yorug'/qorong'i tema, bo'sh va yuklanish holatlari.
@Preview(name = "Light", showSystemUi = true)
@Composable
private fun PhoneLightPreview() {
    SwiftChatTheme(darkTheme = false) {
        PhoneScreenContent(PhoneContract.UiState(digits = "901234567"), onEventDispatcher = {})
    }
}

@Preview(name = "Dark", showSystemUi = true)
@Composable
private fun PhoneDarkPreview() {
    SwiftChatTheme(darkTheme = true) {
        PhoneScreenContent(PhoneContract.UiState(digits = "901234567"), onEventDispatcher = {})
    }
}

@Preview(name = "Empty · Light", showSystemUi = true)
@Composable
private fun PhoneEmptyPreview() {
    SwiftChatTheme(darkTheme = false) {
        PhoneScreenContent(PhoneContract.UiState(), onEventDispatcher = {})
    }
}

@Preview(name = "Loading · Dark", showSystemUi = true)
@Composable
private fun PhoneLoadingPreview() {
    SwiftChatTheme(darkTheme = true) {
        PhoneScreenContent(PhoneContract.UiState(digits = "901234567", loading = true), onEventDispatcher = {})
    }
}
