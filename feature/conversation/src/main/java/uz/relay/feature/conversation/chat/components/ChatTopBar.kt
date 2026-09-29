package uz.relay.feature.conversation.chat.components

import androidx.compose.ui.res.pluralStringResource
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import uz.relay.domain.model.ChatType
import uz.relay.feature.conversation.R
import uz.relay.core.designsystem.util.formatPresence

/**
 * 64dp sarlavha: orqaga · avatar 40 · ism (17/700) + holat (13) · "ko'proq".
 *
 * Holat ustuvorligi (spec 3.8): yozmoqda (primary) > online (primary) > "oxirgi marta …" (text2).
 * Guruhda: yozmoqda > "12 aʼzo". Sarlavha bosilsa guruh ma'lumoti ochiladi.
 */
@Composable
fun ChatTopBar(
    chat: ChatSummary?,
    typingUserIds: Set<String>,
    names: Map<String, String>,
    memberCount: Int,
    onBack: () -> Unit,
    onTitleClick: () -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = SwiftTheme.colors
    val resources = LocalContext.current.resources
    val isGroup = chat?.type == ChatType.GROUP

    val typingText = typingUserIds.firstOrNull()?.let { userId ->
        val name = names[userId]
        if (isGroup && name != null) stringResource(R.string.typing_named, name) else stringResource(R.string.typing)
    }
    val (subtitle, highlighted) = when {
        typingText != null -> typingText to true
        chat == null -> null to false
        isGroup -> (if (memberCount > 0) pluralStringResource(R.plurals.members_n, memberCount, memberCount) else null) to false
        else -> formatPresence(chat.peerOnline, chat.peerLastSeenAt, resources) to chat.peerOnline
    }

    Column(modifier = modifier.background(colors.bg)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    painter = painterResource(DesignR.drawable.ic_arrow_left),
                    contentDescription = stringResource(R.string.back),
                    tint = colors.text
                )
            }
            if (chat != null) {
                Avatar(name = chat.title, colorSeed = chat.peerUserId ?: chat.id, size = 40.dp)
            } else {
                Box(modifier = Modifier.size(40.dp))
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onTitleClick)
                    .padding(start = 4.dp),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                Text(
                    text = chat?.title.orEmpty(),
                    color = colors.text,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        color = if (highlighted) colors.primary else colors.text2,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            IconButton(onClick = onMoreClick) {
                Icon(
                    painter = painterResource(DesignR.drawable.ic_more_vertical),
                    contentDescription = stringResource(R.string.more),
                    tint = colors.text,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        HorizontalDivider(thickness = 1.dp, color = colors.line)
    }
}
