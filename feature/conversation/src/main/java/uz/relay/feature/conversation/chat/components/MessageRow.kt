package uz.relay.feature.conversation.chat.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import uz.relay.core.designsystem.R as DesignR
import uz.relay.core.designsystem.component.Avatar
import uz.relay.core.designsystem.theme.SwiftTheme
import uz.relay.domain.model.CallLogFormat
import uz.relay.domain.model.MessageStatus
import uz.relay.domain.model.MessageType
import uz.relay.feature.conversation.R
import uz.relay.feature.conversation.chat.ChatItem

/**
 * Ro'yxatdagi bitta xabar qatori: bubble'ni chapga (kiruvchi) yoki o'ngga (chiquvchi) tekislaydi.
 *  - kiruvchi: guruhda chapda 32dp avatar joyi (avatar faqat ketma-ketlikning oxirida), o'ngda 44dp bo'sh joy;
 *  - chiquvchi: chapda 44dp bo'sh joy, xato bo'lsa bubble oldida qizil "qayta yuborish" tugmasi.
 *
 * Uzoq bosilganda bubble'ning ekrandagi joyi ([Rect]) ham beriladi — menyu o'sha joyda "ko'tarilgan" bubble'ni
 * chizishi uchun.
 *
 * Qaysi qatorda ism/avatar ko'rinishi bu yerda hisoblanmaydi — [ChatItem.Bubble] bayroqlari ViewModel'da tayyorlanadi.
 * `combinedClickable` (ExperimentalFoundationApi) bitta modifier'da oddiy bosish (media ochish) va uzoq bosishni (menyu) beradi.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageRow(
    item: ChatItem.Bubble,
    isGroup: Boolean,
    names: Map<String, String>,
    onLongPress: (Rect) -> Unit,
    onReplyClick: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    /** Rasm/video — ko'ruvchi, fayl — yuklab olib ochish. */
    onMediaClick: () -> Unit = {},
    onCancelUpload: () -> Unit = {},
    downloadProgress: Float? = null,
    /** Qo'ng'iroq yozuvi bosildi — shu turdagi (video/audio) qo'ng'iroqni qayta boshlash. */
    onCallLogClick: (video: Boolean) -> Unit = {}
) {
    val message = item.message
    val isOut = message.isMine
    val hasMedia = message.media.isNotEmpty() && !message.isDeleted
    val callLog = remember(message.text, message.type, message.isDeleted) {
        if (message.type == MessageType.TEXT && !message.isDeleted) CallLogFormat.parse(message.text) else null
    }
    // Bubble'ning oxirgi o'lchangan joyi. State emas: u faqat uzoq bosilgan paytda o'qiladi, qayta chizish kerak emas.
    val bounds = remember { arrayOf(Rect.Zero) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                top = 2.dp,
                bottom = 2.dp,
                start = if (isOut) 44.dp else 0.dp,
                end = if (isOut) 0.dp else 44.dp
            ),
        horizontalArrangement = if (isOut) Arrangement.spacedBy(8.dp, Alignment.End) else Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        if (!isOut && isGroup) {
            if (item.showAvatar) {
                Avatar(name = names[message.senderId], colorSeed = message.senderId, size = 32.dp)
            } else {
                // Avatar yo'q qatorlarda ham bubble'lar bir chiziqda tursin.
                Box(modifier = Modifier.size(32.dp))
            }
        }
        if (isOut && message.status == MessageStatus.FAILED) {
            RetryButton(onClick = onRetry, modifier = Modifier.padding(bottom = 2.dp))
        }

        MessageBubble(
            message = message,
            senderName = if (item.showSenderName) names[message.senderId] else null,
            replied = item.replied,
            repliedSenderName = item.replied?.let { names[it.senderId] },
            onReplyClick = onReplyClick,
            downloadProgress = downloadProgress,
            onCancelUpload = onCancelUpload,
            modifier = Modifier
                .onGloballyPositioned { bounds[0] = it.boundsInRoot() }
                .clip(RoundedCornerShape(18.dp))
                .combinedClickable(
                    onClick = when {
                        hasMedia -> onMediaClick
                        callLog != null -> ({ onCallLogClick(callLog.video) })
                        else -> ({})
                    },
                    // O'chirilgan xabar uchun menyu yo'q (spec: "no reply/menu").
                    onLongClick = if (message.isDeleted) null else ({ onLongPress(bounds[0]) })
                )
        )
    }
}

/** 30dp qizil doira ichida "qayta yuborish" belgisi. */
@Composable
private fun RetryButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = SwiftTheme.colors
    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(30.dp)
            .background(colors.error, CircleShape),
        colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White)
    ) {
        Icon(
            painter = painterResource(DesignR.drawable.ic_retry),
            contentDescription = stringResource(R.string.resend),
            modifier = Modifier.size(16.dp)
        )
    }
}
