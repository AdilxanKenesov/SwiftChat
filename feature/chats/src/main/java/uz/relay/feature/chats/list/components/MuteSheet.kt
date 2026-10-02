package uz.relay.feature.chats.list.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.relay.core.designsystem.R as DesignR
import uz.relay.core.designsystem.component.Avatar
import uz.relay.core.designsystem.theme.SwiftTheme
import uz.relay.domain.model.ChatSummary
import uz.relay.domain.model.MuteDuration
import uz.relay.feature.chats.R
import uz.relay.feature.chats.util.formatMuteUntil

/**
 * Chat qatoriga long-press bosilganda ochiladi (spec: "long-press mute"). Uslubi guruh ma'lumotidagi
 * a'zo sheet'i bilan bir xil: sarlavha (avatar + nom + holat) va 56dp qatorlar.
 *  - Ovozsiz emas → muddatlar: 1 soat, 8 soat, 1 kun, butunlay.
 *  - Ovozsiz → bitta "Ovozni yoqish" (muddatni almashtirish uchun avval yoqib, keyin qayta tanlanadi).
 *
 * ModalBottomSheet — ro'yxat ustida qisqa tanlov uchun qulay: tashqariga bosish yoki pastga surish bilan yopiladi.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MuteSheet(
    chat: ChatSummary,
    onDismiss: () -> Unit,
    onMute: (MuteDuration) -> Unit,
    onUnmute: () -> Unit
) {
    val colors = SwiftTheme.colors
    val mutedUntil = chat.mutedUntil
    val status = when {
        !chat.muted -> null
        mutedUntil != null -> stringResource(R.string.muted_until, formatMuteUntil(mutedUntil))
        else -> stringResource(R.string.muted_forever)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        // Sheet qisqa — yarim ochiq holat keraksiz, darhol to'liq ochiladi.
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = colors.bg,
        scrimColor = colors.scrim,
        dragHandle = { BottomSheetDefaults.DragHandle(color = colors.outline, width = 32.dp, height = 4.dp) }
    ) {
        Column(modifier = Modifier.navigationBarsPadding().padding(bottom = 16.dp)) {
            Row(
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Avatar(name = chat.title, colorSeed = chat.peerUserId ?: chat.id, size = 48.dp)
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = chat.title.orEmpty(),
                        color = colors.text,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(text = status ?: stringResource(R.string.mute_long), color = colors.text2, fontSize = 14.sp)
                }
            }

            if (chat.muted) {
                SheetItem(icon = DesignR.drawable.ic_bell, label = stringResource(R.string.unmute), color = colors.primary, onClick = onUnmute)
            } else {
                MuteOptions.forEach { (duration, label) ->
                    SheetItem(
                        icon = DesignR.drawable.ic_bell_off,
                        label = stringResource(label),
                        color = colors.text,
                        onClick = { onMute(duration) }
                    )
                }
            }
        }
    }
}

/** Ovozsiz qilish muddatlari va ularning yorliqlari (sheet'dagi tartibda). */
private val MuteOptions = listOf(
    MuteDuration.HOUR to R.string.mute_1h,
    MuteDuration.EIGHT_HOURS to R.string.mute_8h,
    MuteDuration.DAY to R.string.mute_1d,
    MuteDuration.FOREVER to R.string.mute_forever
)

/** Sheet'dagi bitta 56dp qator: ikonka + yorliq. */
@Composable
private fun SheetItem(icon: Int, label: String, color: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(painter = painterResource(icon), contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
        Text(text = label, color = color, fontSize = 16.sp, fontWeight = FontWeight.Medium)
    }
}
