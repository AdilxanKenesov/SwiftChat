package uz.relay.feature.chats.list.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.relay.core.designsystem.component.CountBadge
import uz.relay.core.designsystem.theme.SwiftTheme
import uz.relay.feature.chats.R
import uz.relay.feature.chats.list.ChatTab

/**
 * "Hammasi / Shaxsiy / Guruhlar" tablari (48dp, pastida chiziq).
 * Faol tab: primary matn + pastda 3dp yumaloq indikator (har yondan 20% ichkarida) + primary belgi.
 * Nofaol: text2 matn + kulrang belgi. Belgi — shu filtrdagi o'qilmagan chatlar soni.
 */
@Composable
internal fun ChatsTabs(
    selected: ChatTab,
    unreadChats: (ChatTab) -> Int,
    onSelect: (ChatTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = SwiftTheme.colors
    Box(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(horizontal = 8.dp)
        ) {
            ChatTab.entries.forEach { tab ->
                val isSelected = tab == selected
                BoxWithConstraints(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .selectable(selected = isSelected, role = Role.Tab, onClick = { onSelect(tab) })
                ) {
                    Row(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(tab.titleRes()),
                            color = if (isSelected) colors.primary else colors.text2,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        val count = unreadChats(tab)
                        if (count > 0) CountBadge(count = count, muted = !isSelected, size = 20.dp)
                    }
                    if (isSelected) {
                        // Indikator kengligi tabning 60% i (har yondan 20%) — padding bilan joylanadi.
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(horizontal = maxWidth * 0.2f)
                                .fillMaxWidth()
                                .height(3.dp)
                                .background(colors.primary, RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                        )
                    }
                }
            }
        }
        HorizontalDivider(
            modifier = Modifier.align(Alignment.BottomCenter),
            thickness = 1.dp,
            color = colors.line
        )
    }
}

private fun ChatTab.titleRes(): Int = when (this) {
    ChatTab.ALL -> R.string.tab_all
    ChatTab.DIRECT -> R.string.tab_direct
    ChatTab.GROUPS -> R.string.tab_groups
}
