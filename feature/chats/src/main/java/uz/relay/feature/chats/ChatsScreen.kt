package uz.relay.feature.chats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import uz.relay.core.designsystem.component.SwiftWordmark
import uz.relay.core.designsystem.theme.SwiftChatTheme
import uz.relay.core.designsystem.theme.SwiftTheme

// Temporary landing screen after login; replaced by the real chat list in the chats stage.
@Composable
internal fun ChatsScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SwiftTheme.colors.bg),
        contentAlignment = Alignment.Center
    ) {
        SwiftWordmark()
    }
}

@Preview(name = "Light", showSystemUi = true)
@Composable
private fun ChatsLightPreview() {
    SwiftChatTheme(darkTheme = false) { ChatsScreen() }
}

@Preview(name = "Dark", showSystemUi = true)
@Composable
private fun ChatsDarkPreview() {
    SwiftChatTheme(darkTheme = true) { ChatsScreen() }
}
