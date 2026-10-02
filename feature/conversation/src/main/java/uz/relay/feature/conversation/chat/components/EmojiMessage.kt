package uz.relay.feature.conversation.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.relay.domain.model.Message

/**
 * Faqat 1–3 ta emojidan iborat xabar — Telegram'dagidek katta va bubble'siz. Vaqt va ✓ belgisi emoji ostidagi
 * kichik yarim shaffof "pill" ichida (har qanday chat fonida o'qiladi). O'lcham emoji soniga qarab kichrayadi.
 */
@Composable
internal fun EmojiMessage(message: Message, count: Int, modifier: Modifier = Modifier) {
    val size = when (count) {
        1 -> 56.sp
        2 -> 44.sp
        else -> 36.sp
    }
    Column(
        modifier = modifier.padding(horizontal = 2.dp, vertical = 2.dp),
        horizontalAlignment = if (message.isMine) Alignment.End else Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(text = message.text.orEmpty().trim(), fontSize = size, lineHeight = size * 1.15f)
        Box(
            modifier = Modifier
                .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            MessageMeta(message = message, color = Color.White)
        }
    }
}
