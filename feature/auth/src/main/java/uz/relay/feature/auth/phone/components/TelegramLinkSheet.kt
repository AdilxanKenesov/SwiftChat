package uz.relay.feature.auth.phone.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.relay.core.designsystem.R as DesignR
import uz.relay.core.designsystem.component.BrandTile
import uz.relay.core.designsystem.component.SwiftPrimaryButton
import uz.relay.core.designsystem.component.SwiftTonalButton
import uz.relay.core.designsystem.theme.Brand
import uz.relay.core.designsystem.theme.SwiftChatTheme
import uz.relay.core.designsystem.theme.SwiftTheme
import uz.relay.feature.auth.R

/** Shown on 409 TELEGRAM_NOT_LINKED. The code is re-requested only when the user taps resend. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelegramLinkSheet(
    phone: String,
    loading: Boolean,
    onOpenBot: () -> Unit,
    onResend: () -> Unit,
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
        TelegramLinkSheetContent(
            phone = phone,
            loading = loading,
            onOpenBot = onOpenBot,
            onResend = onResend,
            modifier = Modifier.navigationBarsPadding()
        )
    }
}

@Composable
private fun TelegramLinkSheetContent(
    phone: String,
    loading: Boolean,
    onOpenBot: () -> Unit,
    onResend: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = SwiftTheme.colors

    Column(modifier = modifier.padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 24.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BrandTile(icon = DesignR.drawable.ic_send, size = 52.dp, cornerRadius = 17.dp, iconSize = 24.dp)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(R.string.tg_link_title),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.text
                )
                Text(text = phone, fontSize = 14.sp, color = colors.text2)
            }
        }

        Column(
            modifier = Modifier
                .padding(top = 22.dp)
                .fillMaxWidth()
                .background(colors.surface, RoundedCornerShape(20.dp))
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            StepRow(number = 1) { Text(stringResource(R.string.tg_step1)) }
            StepRow(number = 2) {
                // Design: "Start" is bold inside step 2.
                val step = stringResource(R.string.tg_step2)
                val boldWord = step.substringBefore(' ')
                Text(buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(boldWord) }
                    append(step.removePrefix(boldWord))
                })
            }
            StepRow(number = 3) { Text(stringResource(R.string.tg_step3)) }
        }

        Column(
            modifier = Modifier.padding(top = 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SwiftTonalButton(
                text = stringResource(R.string.open_bot),
                onClick = onOpenBot,
                trailingIcon = DesignR.drawable.ic_external_link
            )
            SwiftPrimaryButton(
                text = stringResource(R.string.resend_code),
                onClick = onResend,
                loading = loading
            )
        }
    }
}

@Composable
private fun StepRow(number: Int, text: @Composable () -> Unit) {
    val colors = SwiftTheme.colors
    Row(
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(Brand, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(text = number.toString(), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
        ProvideTextStyle(
            SwiftTheme.typography.body.copy(color = colors.text)
        ) { text() }
    }
}

@Preview(name = "Light", showBackground = true)
@Composable
private fun TelegramLinkLightPreview() {
    SwiftChatTheme(darkTheme = false) {
        Box(Modifier.background(SwiftTheme.colors.bg)) {
            TelegramLinkSheetContent(phone = "+998 93 555 12 34", loading = false, onOpenBot = {}, onResend = {})
        }
    }
}

@Preview(name = "Dark", showBackground = true)
@Composable
private fun TelegramLinkDarkPreview() {
    SwiftChatTheme(darkTheme = true) {
        Box(Modifier.background(SwiftTheme.colors.bg)) {
            TelegramLinkSheetContent(phone = "+998 93 555 12 34", loading = false, onOpenBot = {}, onResend = {})
        }
    }
}
