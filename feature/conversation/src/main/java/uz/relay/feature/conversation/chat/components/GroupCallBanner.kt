package uz.relay.feature.conversation.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.relay.core.designsystem.R as DesignR
import uz.relay.core.designsystem.theme.SwiftChatTheme
import uz.relay.core.designsystem.theme.SwiftTheme
import uz.relay.feature.conversation.R

/**
 * Guruhda video chat ketayotganda sarlavha ostidagi panel (Telegram'dagidek): "Video chat · 3 ishtirokchi" va
 * "Qo'shilish" tugmasi. Push yo'qligi sababli a'zolar video chat borligini aynan shu panel va chatdagi yozuvdan
 * bilib qoladi. Butun panel bosiladi — kichik tugmani nishonga olish shart emas.
 */
@Composable
internal fun GroupCallBanner(participants: Int, onJoin: () -> Unit, modifier: Modifier = Modifier) {
    val colors = SwiftTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(colors.bg)
            .clickable(onClick = onJoin)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(36.dp).background(colors.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(painter = painterResource(DesignR.drawable.ic_video), contentDescription = null, tint = colors.primary, modifier = Modifier.size(18.dp))
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(text = stringResource(R.string.group_call_banner_title), color = colors.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Text(
                text = pluralStringResource(R.plurals.group_call_participants, participants, participants),
                color = colors.text2,
                fontSize = 13.sp,
                maxLines = 1
            )
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(colors.primary)
                .padding(horizontal = 16.dp, vertical = 7.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(text = stringResource(R.string.group_call_join), color = colors.onPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Preview(name = "Group call banner · Light")
@Composable
private fun GroupCallBannerPreview() = SwiftChatTheme(darkTheme = false) { GroupCallBanner(participants = 3, onJoin = {}) }

@Preview(name = "Group call banner · Dark")
@Composable
private fun GroupCallBannerDarkPreview() = SwiftChatTheme(darkTheme = true) { GroupCallBanner(participants = 1, onJoin = {}) }
