package uz.relay.core.designsystem.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.relay.core.designsystem.R
import uz.relay.core.designsystem.theme.Brand
import uz.relay.core.designsystem.theme.SwiftChatTheme
import uz.relay.core.designsystem.theme.SwiftTheme

/** Brand tile with the white SwiftChat mark: 112dp on splash, 34dp in the chat list app bar. */
@Composable
fun SwiftLogoTile(
    size: Dp,
    modifier: Modifier = Modifier,
    glow: Boolean = true,
) {
    val shape = RoundedCornerShape(size * 0.3f)
    Box(
        modifier = modifier
            .size(size)
            .then(
                if (glow) Modifier.shadow(elevation = size * 0.2f, shape = shape, ambientColor = Brand, spotColor = Brand)
                else Modifier
            )
            .background(Brand, shape),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_logo),
            contentDescription = null,
            modifier = Modifier.size(size * 0.643f),
        )
    }
}

/** "Swift" in text color + "Chat" in primary, weight 800. */
@Composable
fun SwiftWordmark(
    modifier: Modifier = Modifier,
    fontSize: TextUnit = SwiftTheme.typography.appTitle.fontSize,
    letterSpacing: TextUnit = 0.sp,
) {
    val colors = SwiftTheme.colors
    Text(
        text = buildAnnotatedString {
            append("Swift")
            withStyle(SpanStyle(color = colors.primary)) { append("Chat") }
        },
        modifier = modifier,
        color = colors.text,
        style = SwiftTheme.typography.appTitle.merge(TextStyle(fontSize = fontSize, letterSpacing = letterSpacing)),
    )
}

@Preview(name = "Light", showBackground = true)
@Composable
private fun SwiftLogoLightPreview() {
    SwiftChatTheme(darkTheme = false) { SwiftLogoPreviewContent() }
}

@Preview(name = "Dark", showBackground = true, backgroundColor = 0xFF131218)
@Composable
private fun SwiftLogoDarkPreview() {
    SwiftChatTheme(darkTheme = true) { SwiftLogoPreviewContent() }
}

@Composable
private fun SwiftLogoPreviewContent() {
    Column(
        modifier = Modifier
            .background(SwiftTheme.colors.bg)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        SwiftLogoTile(size = 112.dp)
        SwiftWordmark(fontSize = 32.sp, letterSpacing = (-0.8).sp)
        SwiftLogoTile(size = 34.dp)
    }
}
