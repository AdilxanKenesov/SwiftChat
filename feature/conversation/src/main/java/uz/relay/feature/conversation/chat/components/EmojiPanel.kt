package uz.relay.feature.conversation.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.relay.core.designsystem.theme.SwiftTheme
import uz.relay.feature.conversation.chat.emoji.EmojiCatalog
import uz.relay.feature.conversation.chat.emoji.EmojiCategory

/**
 * Klaviatura o'rnida ochiladigan emoji paneli: tepada kategoriyalar (birinchisi — "So'nggi", agar bo'lsa), ostida
 * emojilar to'ri. Emoji bosilsa matnga qo'shiladi, panel yopilmaydi (ketma-ket bir nechtasini tanlash qulay).
 * Balandligi oxirgi ochilgan klaviatura bilan bir xil ([height]) — panel va klaviatura almashganda ekran sakramaydi.
 */
@Composable
internal fun EmojiPanel(
    recent: List<String>,
    height: Dp,
    onEmoji: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = SwiftTheme.colors
    val categories = remember(recent) {
        if (recent.isEmpty()) EmojiCatalog else listOf(EmojiCategory(RECENT_ICON, recent)) + EmojiCatalog
    }
    var selected by rememberSaveable { mutableIntStateOf(0) }
    val current = categories[selected.coerceIn(categories.indices)]

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.bg)
            .navigationBarsPadding()
            .height(height)
            .testTag(EMOJI_PANEL_TAG)
    ) {
        HorizontalDivider(color = colors.line)
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp)) {
            categories.forEachIndexed { index, category ->
                val isSelected = index == selected.coerceIn(categories.indices)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) colors.surface2 else colors.bg)
                        .clickable { selected = index },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = category.icon, fontSize = 18.sp)
                }
            }
        }
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 44.dp),
            modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 6.dp)
        ) {
            items(current.emojis, key = { it }) { emoji ->
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .clickable { onEmoji(emoji) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = emoji, fontSize = 26.sp)
                }
            }
        }
    }
}

/** "So'nggi" kategoriyasining belgisi. */
private const val RECENT_ICON = "🕘"

internal const val EMOJI_PANEL_TAG = "emoji_panel"
