package uz.relay.core.designsystem.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.relay.core.designsystem.theme.SwiftTheme

private val PillShape = RoundedCornerShape(28.dp)

/** Pill 56dp primary button. Disabled = surface2/text2; [loading] shows a spinner inside. */
@Composable
fun SwiftPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    val colors = SwiftTheme.colors
    SwiftPillButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled && !loading,
        container = colors.primary,
        content = colors.onPrimary,
        loading = loading,
    )
}

/** Pill 56dp tonal button (primaryContainer), optional trailing icon. */
@Composable
fun SwiftTonalButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes trailingIcon: Int? = null,
) {
    val colors = SwiftTheme.colors
    SwiftPillButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        enabled = true,
        container = colors.primaryContainer,
        content = colors.onPrimaryContainer,
        trailingIcon = trailingIcon,
    )
}

@Composable
private fun SwiftPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    container: Color,
    content: Color,
    loading: Boolean = false,
    @DrawableRes trailingIcon: Int? = null,
) {
    val colors = SwiftTheme.colors
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = PillShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            contentColor = content,
            // A loading button keeps its colors; only a truly disabled one greys out.
            disabledContainerColor = if (loading) container else colors.surface2,
            disabledContentColor = if (loading) content else colors.text2,
        ),
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = content,
                strokeWidth = 2.5.dp,
            )
        } else {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                if (trailingIcon != null) {
                    Icon(
                        painter = painterResource(trailingIcon),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}
