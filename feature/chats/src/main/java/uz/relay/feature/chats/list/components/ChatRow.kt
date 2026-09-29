package uz.relay.feature.chats.list.components

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.relay.core.designsystem.R as DesignR
import uz.relay.core.designsystem.component.Avatar
import uz.relay.core.designsystem.component.CountBadge
import uz.relay.core.designsystem.theme.SwiftTheme
import uz.relay.domain.model.ChatSummary
import uz.relay.domain.model.ChatType
import uz.relay.domain.model.MessageStatus
import uz.relay.domain.model.MessageType
import uz.relay.feature.chats.R
import uz.relay.feature.chats.util.PreviewColors
import uz.relay.feature.chats.util.buildChatPreview
import uz.relay.feature.chats.util.formatChatTime

/**
 * Chatlar ro'yxatidagi bitta qator (76dp), spec 3.5:
 *  avatar 56 (+ DIRECT uchun online nuqta) · 1-qator: [guruh ikonkasi] ism · [ovozsiz] · [✓] · vaqt
 *  · 2-qator: oxirgi xabar + o'qilmaganlar belgisi. Pastki chiziq avatardan keyin boshlanadi (inset).
 */
@Composable
internal fun ChatRow(
    chat: ChatSummary,
    userNames: Map<String, String>,
    /** Hozir shu chatda yozayotganlar (o'zimsiz). Bo'sh bo'lmasa, oxirgi xabar o'rnida "yozmoqda…". */
    typingUserIds: Set<String>,
    onClick: () -> Unit,
    /** Long-press — ovozsiz qilish sheet'i. */
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = SwiftTheme.colors
    val resources = LocalContext.current.resources
    val last = chat.lastMessage
    val hasUnread = chat.unreadCount > 0
    // Vaqt primary rangda va qalin — faqat o'qilmagan xabar bo'lsa va chat ovozsiz bo'lmasa.
    val timeHighlighted = hasUnread && !chat.muted

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(76.dp)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(start = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar(
            name = chat.title,
            colorSeed = chat.peerUserId ?: chat.id,
            online = chat.type == ChatType.DIRECT && chat.peerOnline
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (chat.type == ChatType.GROUP) {
                        Icon(
                            painter = painterResource(DesignR.drawable.ic_users),
                            contentDescription = stringResource(R.string.group),
                            tint = colors.text,
                            modifier = Modifier
                                .padding(end = 5.dp)
                                .size(16.dp)
                        )
                    }
                    // Ism qolgan joyni egallaydi, "ovozsiz" belgisi esa ismning darhol yonida turadi.
                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = chat.title.orEmpty(),
                            color = colors.text,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (chat.muted) {
                            Icon(
                                painter = painterResource(DesignR.drawable.ic_bell_off),
                                contentDescription = stringResource(R.string.muted),
                                tint = colors.text2,
                                modifier = Modifier
                                    .padding(start = 5.dp)
                                    .size(15.dp)
                            )
                        }
                    }
                    if (last != null && last.isMine && !last.isDeleted && last.type != MessageType.SYSTEM) {
                        StatusTick(status = last.status, modifier = Modifier.padding(start = 5.dp, end = 5.dp))
                    }
                    Text(
                        text = formatChatTime(last?.createdAt ?: chat.lastActivityAt, resources),
                        color = if (timeHighlighted) colors.primary else colors.text2,
                        fontSize = 13.sp,
                        fontWeight = if (timeHighlighted) FontWeight.SemiBold else FontWeight.Normal
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // "Yozmoqda…" oxirgi xabardan ustun turadi (spec 3.5). Guruhda kim yozayotgani ham aytiladi.
                    val typingText = typingUserIds.firstOrNull()?.let { userId ->
                        val name = userNames[userId]
                        if (chat.type == ChatType.GROUP && name != null) stringResource(R.string.typing_named, name)
                        else stringResource(R.string.typing)
                    }
                    val preview = remember(last, chat.type, userNames, colors) {
                        last?.let {
                            buildChatPreview(
                                message = it,
                                chatType = chat.type,
                                names = userNames,
                                resources = resources,
                                colors = PreviewColors(prefix = colors.text, highlight = colors.primary)
                            )
                        }
                    }
                    Text(
                        text = typingText?.let(::AnnotatedString) ?: preview ?: AnnotatedString(""),
                        color = if (typingText != null) colors.primary else colors.text2,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (hasUnread) CountBadge(count = chat.unreadCount, muted = chat.muted)
                }
            }
            HorizontalDivider(
                modifier = Modifier.align(Alignment.BottomCenter),
                thickness = 1.dp,
                color = colors.line
            )
        }
    }
}

/** Soat — yuborilmoqda, ✓ yuborildi, ✓✓ yetkazildi (kulrang), ✓✓ o'qildi (primary), qizil belgi — yuborilmadi. */
@Composable
private fun StatusTick(status: MessageStatus, modifier: Modifier = Modifier) {
    val colors = SwiftTheme.colors
    val (icon, description) = when (status) {
        MessageStatus.SENDING -> DesignR.drawable.ic_clock to R.string.status_sending
        MessageStatus.FAILED -> DesignR.drawable.ic_alert_circle to R.string.status_failed
        MessageStatus.SENT -> DesignR.drawable.ic_check_single to R.string.status_sent
        MessageStatus.DELIVERED -> DesignR.drawable.ic_check_double to R.string.status_delivered
        MessageStatus.READ -> DesignR.drawable.ic_check_double to R.string.status_read
    }
    Icon(
        painter = painterResource(icon),
        contentDescription = stringResource(description),
        tint = when (status) {
            MessageStatus.READ -> colors.primary
            MessageStatus.FAILED -> colors.error
            else -> colors.text2
        },
        modifier = modifier.size(17.dp)
    )
}
