package uz.relay.feature.auth.otp

import androidx.compose.ui.res.pluralStringResource
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import uz.relay.core.designsystem.R as DesignR
import uz.relay.core.designsystem.component.BrandTile
import uz.relay.core.designsystem.component.SwiftPrimaryButton
import uz.relay.core.designsystem.theme.SwiftChatTheme
import uz.relay.core.designsystem.theme.SwiftTheme
import uz.relay.feature.auth.R
import uz.relay.feature.auth.otp.components.OtpCodeInput
import uz.relay.feature.auth.util.formatFullPhone
import uz.relay.feature.auth.util.messageRes

@Composable
internal fun OtpScreen(phone: String) {
    val viewModel = hiltViewModel<OtpViewModel, OtpViewModel.Factory>(
        creationCallback = { factory -> factory.create(phone) }
    )
    val uiState by viewModel.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val shakeEvents = remember { MutableSharedFlow<Unit>(extraBufferCapacity = 1) }
    val context = LocalContext.current

    viewModel.collectSideEffect { sideEffect ->
        when (sideEffect) {
            is OtpContract.SideEffect.ShowError ->
                snackbarHostState.showSnackbar(context.getString(sideEffect.error.messageRes()))

            OtpContract.SideEffect.Shake -> shakeEvents.tryEmit(Unit)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        OtpScreenContent(
            uiState = uiState,
            shakeEvents = shakeEvents,
            onEventDispatcher = viewModel::onEventDispatcher
        )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .imePadding()
        )
    }
}

@Composable
private fun OtpScreenContent(
    uiState: OtpContract.UiState,
    shakeEvents: Flow<Unit>,
    onEventDispatcher: (OtpContract.Intent) -> Unit
) {
    val colors = SwiftTheme.colors
    val typography = SwiftTheme.typography
    val status = uiState.status
    val renew = status == OtpContract.Status.Expired || status == OtpContract.Status.Locked

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        Box(
            modifier = Modifier
                .height(64.dp)
                .padding(horizontal = 4.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            IconButton(onClick = { onEventDispatcher(OtpContract.Intent.OnBack) }) {
                Icon(
                    painter = painterResource(DesignR.drawable.ic_arrow_left),
                    contentDescription = stringResource(R.string.back),
                    tint = colors.text
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 24.dp)
        ) {
            BrandTile(icon = DesignR.drawable.ic_shield_check, iconSize = 30.dp)

            Text(
                text = stringResource(R.string.otp_title),
                style = typography.displayTitle,
                color = colors.text,
                modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
            )

            val formattedPhone = formatFullPhone(uiState.phone)
            val subtitle = stringResource(R.string.otp_sent, formattedPhone)
            val phoneStart = subtitle.indexOf(formattedPhone)
            Text(
                text = buildAnnotatedString {
                    append(subtitle)
                    if (phoneStart >= 0) {
                        addStyle(
                            SpanStyle(color = colors.text, fontWeight = FontWeight.SemiBold),
                            phoneStart,
                            phoneStart + formattedPhone.length
                        )
                    }
                },
                style = typography.supporting.copy(lineHeight = 22.sp),
                color = colors.text2
            )

            OtpCodeInput(
                code = uiState.code,
                codeLength = uiState.codeLength,
                status = status,
                enabled = uiState.inputEnabled,
                onCodeChange = { onEventDispatcher(OtpContract.Intent.OnCodeChange(it)) },
                shakeEvents = shakeEvents,
                modifier = Modifier.padding(top = 36.dp)
            )

            when (status) {
                is OtpContract.Status.Wrong -> StatusRow(
                    icon = DesignR.drawable.ic_alert_circle,
                    text = pluralStringResource(R.plurals.otp_wrong, status.attemptsLeft, status.attemptsLeft)
                )

                OtpContract.Status.Expired -> StatusRow(
                    icon = DesignR.drawable.ic_clock,
                    text = stringResource(R.string.otp_expired)
                )

                OtpContract.Status.Locked -> LockedCard()
                OtpContract.Status.Input -> Unit
            }

            if (uiState.verifying) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .padding(top = 28.dp)
                        .size(24.dp)
                        .align(Alignment.CenterHorizontally),
                    color = colors.primary,
                    strokeWidth = 2.5.dp
                )
            } else if (!renew) {
                ResendTimer(
                    secondsLeft = uiState.secondsLeft,
                    resending = uiState.resending,
                    onResend = { onEventDispatcher(OtpContract.Intent.OnResendCode) },
                    modifier = Modifier
                        .padding(top = 28.dp)
                        .align(Alignment.CenterHorizontally)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            if (renew) {
                SwiftPrimaryButton(
                    text = stringResource(R.string.new_code),
                    onClick = { onEventDispatcher(OtpContract.Intent.OnResendCode) },
                    loading = uiState.resending
                )
            }
        }
    }
}

@Composable
private fun StatusRow(@DrawableRes icon: Int, text: String) {
    val colors = SwiftTheme.colors
    Row(
        modifier = Modifier.padding(top = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(painter = painterResource(icon), contentDescription = null, tint = colors.error, modifier = Modifier.size(18.dp))
        Text(text = text, color = colors.error, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun LockedCard() {
    val colors = SwiftTheme.colors
    Row(
        modifier = Modifier
            .padding(top = 20.dp)
            .fillMaxWidth()
            .background(colors.errorContainer, RoundedCornerShape(18.dp))
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(colors.error, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(DesignR.drawable.ic_lock),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = stringResource(R.string.otp_locked),
                color = colors.error,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Text(text = stringResource(R.string.otp_locked_sub), color = colors.text, fontSize = 14.sp)
        }
    }
}

@Composable
private fun ResendTimer(
    secondsLeft: Int,
    resending: Boolean,
    onResend: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = SwiftTheme.colors
    if (secondsLeft > 0) {
        val time = "%d:%02d".format(secondsLeft / 60, secondsLeft % 60)
        val line = stringResource(R.string.resend_in, time)
        Text(
            text = buildAnnotatedString {
                append(line.removeSuffix(time))
                withStyle(SpanStyle(color = colors.text, fontWeight = FontWeight.Bold)) { append(time) }
            },
            modifier = modifier,
            color = colors.text2,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
    } else {
        TextButton(onClick = onResend, enabled = !resending, modifier = modifier) {
            Text(
                text = stringResource(R.string.resend_code),
                color = colors.primary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun OtpPreview(darkTheme: Boolean, state: OtpContract.UiState) {
    SwiftChatTheme(darkTheme = darkTheme) {
        OtpScreenContent(uiState = state, shakeEvents = emptyFlow(), onEventDispatcher = {})
    }
}

private const val PreviewPhone = "+998901234567"

@Preview(name = "Input · Light", showSystemUi = true)
@Composable
private fun OtpInputLightPreview() =
    OtpPreview(false, OtpContract.UiState(phone = PreviewPhone, codeLength = 6, code = "111", secondsLeft = 42))

@Preview(name = "Input · Dark", showSystemUi = true)
@Composable
private fun OtpInputDarkPreview() =
    OtpPreview(true, OtpContract.UiState(phone = PreviewPhone, codeLength = 6, code = "111", secondsLeft = 42))

@Preview(name = "Wrong · Light", showSystemUi = true)
@Composable
private fun OtpWrongLightPreview() = OtpPreview(
    false,
    OtpContract.UiState(phone = PreviewPhone, codeLength = 6, code = "123456", status = OtpContract.Status.Wrong(3), secondsLeft = 18)
)

@Preview(name = "Wrong · Dark", showSystemUi = true)
@Composable
private fun OtpWrongDarkPreview() = OtpPreview(
    true,
    OtpContract.UiState(phone = PreviewPhone, codeLength = 6, code = "123456", status = OtpContract.Status.Wrong(3), secondsLeft = 18)
)

@Preview(name = "Expired · Light", showSystemUi = true)
@Composable
private fun OtpExpiredLightPreview() =
    OtpPreview(false, OtpContract.UiState(phone = PreviewPhone, codeLength = 6, code = "111111", status = OtpContract.Status.Expired))

@Preview(name = "Expired · Dark", showSystemUi = true)
@Composable
private fun OtpExpiredDarkPreview() =
    OtpPreview(true, OtpContract.UiState(phone = PreviewPhone, codeLength = 6, code = "111111", status = OtpContract.Status.Expired))

@Preview(name = "Locked · Light", showSystemUi = true)
@Composable
private fun OtpLockedLightPreview() =
    OtpPreview(false, OtpContract.UiState(phone = PreviewPhone, codeLength = 6, status = OtpContract.Status.Locked))

@Preview(name = "Locked · Dark", showSystemUi = true)
@Composable
private fun OtpLockedDarkPreview() =
    OtpPreview(true, OtpContract.UiState(phone = PreviewPhone, codeLength = 6, status = OtpContract.Status.Locked))
