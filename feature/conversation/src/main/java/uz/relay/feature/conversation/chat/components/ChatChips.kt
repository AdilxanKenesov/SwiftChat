package uz.relay.feature.conversation.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.relay.core.designsystem.theme.SwiftTheme

/** Sana ajratgichi: markazda chip ("Bugun", "Kecha", "24-sentabr"), 13/600. */
@Composable
fun DateChip(text: String, modifier: Modifier = Modifier) {
    CenterChip(text = text, bold = true, modifier = modifier.padding(top = 8.dp, bottom = 6.dp))
}

/** SYSTEM xabar: markazda chip, 13sp ("Ali guruhni yaratdi"). */
@Composable
fun SystemChip(text: String, modifier: Modifier = Modifier) {
    CenterChip(text = text, bold = false, modifier = modifier.padding(vertical = 3.dp))
}

@Composable
private fun CenterChip(text: String, bold: Boolean, modifier: Modifier) {
    val colors = SwiftTheme.colors
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Text(
            text = text,
            color = colors.onChip,
            fontSize = 13.sp,
            fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .background(colors.chip, RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 4.dp)
        )
    }
}
