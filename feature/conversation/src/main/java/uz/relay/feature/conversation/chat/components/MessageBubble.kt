package uz.relay.feature.conversation.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.max
import uz.relay.core.designsystem.R as DesignR
import uz.relay.core.designsystem.theme.SenderNameColors
import uz.relay.core.designsystem.theme.SwiftTheme
import uz.relay.domain.model.CallLogFormat
import uz.relay.domain.model.Message
import uz.relay.domain.model.MessageStatus
import uz.relay.domain.model.MessageType
import uz.relay.feature.conversation.R
import uz.relay.feature.conversation.chat.emoji.emojiOnlyCount
import uz.relay.feature.conversation.util.formatMessageTime

/** Bubble ranglari: kiruvchi va chiquvchi xabar uchun turlicha (spec 1.1). */
internal data class BubbleColors(
    val container: Color,
    val content: Color,
    val meta: Color,
    val replyContainer: Color,
    val replyAccent: Color,
    val replySnippet: Color
)

/**
 * Yo'nalishga qarab bubble ranglarini SwiftTheme'dan yig'adi. Ranglar bitta joyda tanlanadi, shunda matnli,
 * media va fayl bubble'lari (hamda iqtibos) bir xil palitradan foydalanadi.
 */
@Composable
internal fun bubbleColors(isOut: Boolean): BubbleColors {
    val colors = SwiftTheme.colors
    return if (isOut) {
        BubbleColors(
            container = colors.bubbleOut,
            content = colors.onBubbleOut,
            meta = colors.metaOut,
            replyContainer = colors.replyOut,
            replyAccent = colors.replyOutAccent,
            replySnippet = colors.metaOut
        )
    } else {
        BubbleColors(
            container = colors.bubbleIn,
            content = colors.text,
            meta = colors.meta,
            replyContainer = colors.replyIn,
            replyAccent = colors.primary,
            replySnippet = colors.text2
        )
    }
}

/** Radius 18, "dum" burchagi 6: kiruvchida pastki-chap, chiquvchida pastki-o'ng (spec 1.3). */
internal val IncomingShape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomEnd = 18.dp, bottomStart = 6.dp)
internal val OutgoingShape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomEnd = 6.dp, bottomStart = 18.dp)

/** Guruhdagi yuboruvchi ismining rangi — userId'dan barqaror hisoblanadi (ikkala bubble rangida ham o'qiladi). */
fun senderNameColor(userId: String): Color = SenderNameColors[Math.floorMod(userId.hashCode(), SenderNameColors.size)]

/**
 * Xabar bubble'i (maksimal eni 264dp): [ism] · [javob iqtibosi] · matn/media + ichki meta (vaqt, ✓).
 *
 * MessageRow ichida chaqiriladi. Media xabarlar (rasm/video/fayl) MessageMedia.kt'dagi alohida bubble'larga
 * yo'naltiriladi — shu sababli bu funksiya "dispatcher" vazifasini ham bajaradi va ekran bitta kirish nuqtasini biladi.
 *
 * @param senderName guruhda ketma-ketlikning birinchi xabarida ko'rsatiladi, aks holda `null`.
 * @param replied javob berilgan xabar (bazada bo'lsa); [repliedSenderName] — uning egasi.
 */
@Composable
fun MessageBubble(
    message: Message,
    senderName: String?,
    replied: Message?,
    repliedSenderName: String?,
    onReplyClick: () -> Unit,
    modifier: Modifier = Modifier,
    /** Fayl yuklab olinmoqda (0..1). */
    downloadProgress: Float? = null,
    onCancelUpload: () -> Unit = {}
) {
    // Media xabar — o'z ko'rinishi (o'chirilgan media esa oddiy "Xabar oʻchirildi" bubble'i).
    val media = message.media.firstOrNull()?.takeIf { !message.isDeleted }
    if (media != null) {
        val activeReply = replied
        when (message.type) {
            MessageType.IMAGE, MessageType.VIDEO -> {
                VisualMessageBubble(message, media, senderName, activeReply, repliedSenderName, onReplyClick, onCancelUpload, modifier)
                return
            }
            MessageType.FILE -> {
                FileMessageBubble(message, media, senderName, activeReply, repliedSenderName, downloadProgress, onReplyClick, onCancelUpload, modifier)
                return
            }
            else -> Unit
        }
    }

    // Qo'ng'iroq yozuvi (matnli xabar maxsus formatda) — o'z ko'rinishi.
    if (message.type == MessageType.TEXT && !message.isDeleted) {
        CallLogFormat.parse(message.text)?.let { log ->
            CallLogBubble(message = message, log = log, modifier = modifier)
            return
        }
    }

    // Faqat emoji (1–3 ta), javobsiz — katta va bubble'siz (Telegram'dagidek). Javob bo'lsa iqtibos uchun oddiy bubble.
    if (message.type == MessageType.TEXT && !message.isDeleted && message.replyToClientMessageId == null) {
        emojiOnlyCount(message.text)?.let { count ->
            EmojiMessage(message = message, count = count, modifier = modifier)
            return
        }
    }

    val isOut = message.isMine
    val bubble = bubbleColors(isOut)

    Column(
        modifier = modifier
            .widthIn(max = 264.dp)
            .background(bubble.container, if (isOut) OutgoingShape else IncomingShape)
            .padding(start = 12.dp, end = 10.dp, top = 7.dp, bottom = 6.dp)
    ) {
        if (senderName != null) {
            Text(
                text = senderName,
                color = senderNameColor(message.senderId),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(bottom = 1.dp)
            )
        }
        if (replied != null && !message.isDeleted) {
            ReplyQuote(
                senderName = repliedSenderName,
                snippet = snippetOf(replied),
                colors = bubble,
                onClick = onReplyClick,
                modifier = Modifier.padding(top = 2.dp, bottom = 5.dp)
            )
        }

        TextWithInlineMeta(
            text = bodyText(message, bubble),
            textColor = if (message.isDeleted) bubble.meta else bubble.content,
            meta = { MessageMeta(message = message, color = bubble.meta) }
        )
    }
}

/** Bubble matni: o'chirilgan → kursiv "Xabar oʻchirildi"; media → "Rasm/Video/Fayl" (+ izoh); aks holda matn. */
@Composable
private fun bodyText(message: Message, bubble: BubbleColors): AnnotatedString {
    val deleted = stringResource(R.string.deleted)
    val mediaLabel = when (message.type) {
        MessageType.IMAGE -> stringResource(R.string.photo)
        MessageType.VIDEO -> stringResource(R.string.video)
        MessageType.FILE -> stringResource(R.string.file)
        else -> null
    }
    return remember(message, deleted, mediaLabel, bubble) {
        buildAnnotatedString {
            when {
                message.isDeleted -> {
                    pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                    append(deleted)
                    pop()
                }
                // Media meta'si hali kelmagan (masalan, eski lokal yozuv) — turi va izohi matn sifatida.
                mediaLabel != null -> {
                    pushStyle(SpanStyle(color = bubble.replyAccent, fontWeight = FontWeight.SemiBold))
                    append(mediaLabel)
                    pop()
                    message.text?.takeIf { it.isNotBlank() }?.let { caption ->
                        append("\n")
                        append(caption)
                    }
                }
                else -> append(message.text.orEmpty())
            }
        }
    }
}

/** Iqtibos va nusxa uchun qisqa matn. */
@Composable
fun snippetOf(message: Message): String = when {
    message.isDeleted -> stringResource(R.string.deleted)
    message.type == MessageType.IMAGE -> message.text?.takeIf { it.isNotBlank() } ?: stringResource(R.string.photo)
    message.type == MessageType.VIDEO -> message.text?.takeIf { it.isNotBlank() } ?: stringResource(R.string.video)
    message.type == MessageType.FILE -> message.text?.takeIf { it.isNotBlank() } ?: stringResource(R.string.file)
    else -> CallLogFormat.parse(message.text)?.let { stringResource(callTitleRes(it, message.isMine)) }
        ?: message.text.orEmpty().replace('\n', ' ')
}

/** Javob iqtibosi: radius 10, chapda 3dp rangli chiziq, ism (13/700) + bir qatorli matn. Bosilsa asl xabarga o'tadi. */
@Composable
internal fun ReplyQuote(
    senderName: String?,
    snippet: String,
    colors: BubbleColors,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(IntrinsicSize.Min)
            .background(colors.replyContainer, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(end = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .fillMaxHeight()
                .background(colors.replyAccent, RoundedCornerShape(2.dp))
        )
        Column(modifier = Modifier.padding(vertical = 5.dp)) {
            if (senderName != null) {
                Text(
                    text = senderName,
                    color = colors.replyAccent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = snippet,
                color = colors.replySnippet,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** Meta (12sp): ["tahrirlangan"] · vaqt · [holat belgisi — faqat o'zimning xabarimda]. */
@Composable
internal fun MessageMeta(message: Message, color: Color) {
    val colors = SwiftTheme.colors
    Row(
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (message.isEdited && !message.isDeleted) {
            Text(
                text = stringResource(R.string.edited),
                color = color,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                modifier = Modifier.padding(end = 2.dp)
            )
        }
        Text(text = formatMessageTime(message.createdAt), color = color, fontSize = 12.sp, lineHeight = 16.sp)
        if (message.isMine && !message.isDeleted) {
            val (icon, description, tint, size) = when (message.status) {
                MessageStatus.SENDING -> StatusIcon(DesignR.drawable.ic_clock, R.string.status_sending, color, 14)
                MessageStatus.SENT -> StatusIcon(DesignR.drawable.ic_check_single, R.string.status_sent, color, 16)
                MessageStatus.DELIVERED -> StatusIcon(DesignR.drawable.ic_check_double, R.string.status_delivered, color, 17)
                MessageStatus.READ -> StatusIcon(DesignR.drawable.ic_check_double, R.string.status_read, colors.tickRead, 17)
                MessageStatus.FAILED -> StatusIcon(DesignR.drawable.ic_alert_circle, R.string.status_failed, colors.error, 15)
            }
            Icon(
                painter = painterResource(icon),
                contentDescription = stringResource(description),
                tint = tint,
                modifier = Modifier.size(size.dp)
            )
        }
    }
}

private data class StatusIcon(val icon: Int, val description: Int, val tint: Color, val size: Int)

/**
 * Matn + meta, Telegram uslubida: meta matnning OXIRGI qatoriga sig'sa, o'sha qatorning o'ng tomonida turadi;
 * sig'masa, pastki qatorga o'ng tomonga tushadi.
 *
 * Nega custom Layout: oxirgi qator qancha joy egallashini faqat matn o'lchangandan keyin bilamiz. Layout avval
 * matnni o'lchaydi, keyin oxirgi qator kengligiga qarab metani joylaydi. TextLayoutResult oddiy "holder"da
 * saqlanadi (Compose state emas) — u o'lchash paytida yoziladi va darhol o'qiladi, qayta chizishga sabab bo'lmaydi.
 */
@Composable
internal fun TextWithInlineMeta(
    text: AnnotatedString,
    textColor: Color,
    meta: @Composable () -> Unit
) {
    val layoutHolder = remember { arrayOfNulls<TextLayoutResult>(1) }
    Layout(
        content = {
            Text(
                text = text,
                color = textColor,
                fontSize = 15.sp,
                lineHeight = 21.sp,
                onTextLayout = { layoutHolder[0] = it }
            )
            meta()
        }
    ) { measurables, constraints ->
        val textPlaceable = measurables[0].measure(constraints.copy(minWidth = 0))
        val metaPlaceable = measurables[1].measure(constraints.copy(minWidth = 0))
        val gap = 10.dp.roundToPx()

        val lastLineWidth = layoutHolder[0]?.let { layout ->
            val last = layout.lineCount - 1
            (layout.getLineRight(last) - layout.getLineLeft(last)).toInt()
        } ?: textPlaceable.width
        val inlineWidth = lastLineWidth + gap + metaPlaceable.width

        if (inlineWidth <= constraints.maxWidth) {
            // Meta oxirgi qatorda: pastki chiziqlari tekislanadi.
            val width = max(textPlaceable.width, inlineWidth)
            val height = max(textPlaceable.height, metaPlaceable.height)
            layout(width, height) {
                textPlaceable.placeRelative(0, 0)
                metaPlaceable.placeRelative(width - metaPlaceable.width, height - metaPlaceable.height)
            }
        } else {
            // Sig'madi — meta alohida qatorda, o'ng tomonda.
            val width = max(textPlaceable.width, metaPlaceable.width)
            val height = textPlaceable.height + metaPlaceable.height
            layout(width, height) {
                textPlaceable.placeRelative(0, 0)
                metaPlaceable.placeRelative(width - metaPlaceable.width, textPlaceable.height)
            }
        }
    }
}
