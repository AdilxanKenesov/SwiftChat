package uz.relay.feature.conversation.chat.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import uz.relay.core.designsystem.R as DesignR
import uz.relay.core.designsystem.theme.SwiftTheme
import uz.relay.domain.model.MediaKind
import uz.relay.domain.model.Message
import uz.relay.domain.model.MessageMedia
import uz.relay.domain.model.MessageStatus
import uz.relay.feature.conversation.R
import uz.relay.feature.conversation.util.fileTypeLabel
import uz.relay.feature.conversation.util.formatDuration
import uz.relay.feature.conversation.util.formatMessageTime
import uz.relay.feature.conversation.util.formatSize
import uz.relay.feature.conversation.util.formatSizeProgress
import java.io.File

/*
 * Media xabarlar ko'rinishi: rasm/video bubble'i, fayl kartasi, yuklash/yuklab olish progressi.
 * MessageBubble media turini ko'rib shu yerdagi funksiyalarga yo'naltiradi. Upload holati (`message.upload`)
 * lokal bazadan keladi — yuklash WorkManager/fon jarayonida bo'lsa ham bubble progressni jonli ko'rsatadi.
 */

/** Spec: rasm/video bubble eni ≈232, balandligi 120..300 (nisbat saqlanadi). */
private val VisualBubbleWidth = 232.dp
private val VisualInnerWidth = VisualBubbleWidth - 6.dp
private val FileBubbleWidth = 248.dp

/** Server bermagan (qabul qilingan video) poster o'rniga to'q fon — ustidagi oq "play" ikkala temada ham ko'rinadi. */
private val VideoPlaceholder = Color(0xFF2B2838)
private val PillBackground = Color.Black.copy(alpha = 0.5f)
private val UploadScrim = Color(0xFF0A0814).copy(alpha = 0.38f)

/**
 * Rasm yoki video xabar: media ustida, izoh (bo'lsa) pastda. Izoh bo'lmasa vaqt media ustidagi pill'da.
 * Balandlik media nisbatidan oldindan hisoblanadi ([visualHeight]) — rasm yuklanganda ro'yxat "sakramasin".
 */
@Composable
internal fun VisualMessageBubble(
    message: Message,
    media: MessageMedia,
    senderName: String?,
    replied: Message?,
    repliedSenderName: String?,
    onReplyClick: () -> Unit,
    onCancelUpload: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isOut = message.isMine
    val bubble = bubbleColors(isOut)
    val caption = message.text?.takeIf { it.isNotBlank() }

    Column(
        modifier = modifier
            .width(VisualBubbleWidth)
            .background(bubble.container, if (isOut) OutgoingShape else IncomingShape)
            .padding(3.dp)
    ) {
        if (senderName != null) {
            Text(
                text = senderName,
                color = senderNameColor(message.senderId),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 9.dp, end = 8.dp, top = 4.dp, bottom = 3.dp)
            )
        }
        if (replied != null) {
            ReplyQuote(
                senderName = repliedSenderName,
                snippet = snippetOf(replied),
                colors = bubble,
                onClick = onReplyClick,
                modifier = Modifier.padding(start = 6.dp, end = 6.dp, top = 3.dp, bottom = 5.dp)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(visualHeight(media))
                .clip(RoundedCornerShape(15.dp))
        ) {
            MediaPreview(media = media, modifier = Modifier.fillMaxSize())

            val upload = message.upload
            val uploading = upload != null && message.status == MessageStatus.SENDING
            when {
                uploading -> {
                    Box(modifier = Modifier.fillMaxSize().background(UploadScrim))
                    UploadRing(
                        progress = upload.fraction,
                        size = 56.dp,
                        onCancel = onCancelUpload,
                        modifier = Modifier.align(Alignment.Center)
                    )
                    Pill(
                        text = formatSizeProgress(upload.sentBytes, upload.totalBytes, LocalContext.current.resources),
                        bold = true,
                        modifier = Modifier.align(Alignment.TopStart).padding(8.dp)
                    )
                }

                media.kind == MediaKind.VIDEO -> {
                    media.durationMs?.let {
                        Pill(text = formatDuration(it), bold = true, modifier = Modifier.align(Alignment.TopStart).padding(8.dp))
                    }
                    PlayBadge(size = 48.dp, modifier = Modifier.align(Alignment.Center))
                }
            }

            if (caption == null) {
                MediaMetaPill(message = message, modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp))
            }
        }

        if (caption != null) {
            Box(modifier = Modifier.padding(start = 9.dp, end = 8.dp, top = 6.dp, bottom = 3.dp)) {
                TextWithInlineMeta(
                    text = androidx.compose.ui.text.AnnotatedString(caption),
                    textColor = bubble.content,
                    meta = { MessageMeta(message = message, color = bubble.meta) }
                )
            }
        }
    }
}

/**
 * Rasm: o'zimniki bo'lsa lokal nusxa (darhol, tarmoqsiz), aks holda server URL (Coil token bilan yuklaydi).
 * Video: faqat o'zim yuborganda poster bor — server thumbnail bermaydi, qabul qiluvchida to'q fon.
 *
 * Coil `AsyncImage` ilova darajasidagi yagona ImageLoader'dan foydalanadi (app modulida sozlangan, Authorization
 * header qo'shadi va disk keshiga ega) — shuning uchun bu yerda token yoki kesh haqida o'ylash shart emas.
 * Media ko'ruvchi ham shu funksiyani qo'shni (joriy bo'lmagan) video sahifalarida ishlatadi.
 */
@Composable
internal fun MediaPreview(media: MessageMedia, modifier: Modifier = Modifier, contentScale: ContentScale = ContentScale.Crop) {
    val model: Any? = when (media.kind) {
        MediaKind.IMAGE -> media.localPath?.let(::File)?.takeIf { it.exists() } ?: media.url
        MediaKind.VIDEO -> media.posterPath?.let(::File)?.takeIf { it.exists() }
        MediaKind.FILE -> null
    }
    Box(modifier = modifier.background(if (media.kind == MediaKind.VIDEO) VideoPlaceholder else SwiftTheme.colors.skeleton)) {
        if (model != null) {
            AsyncImage(
                model = model,
                contentDescription = stringResource(if (media.kind == MediaKind.VIDEO) R.string.video else R.string.photo),
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/** Fayl xabar (spec: 46 doira · nom · "2.4 MB · PDF"). Yuklanayotganda — halqa, chiziqli progress va hajm. */
@Composable
internal fun FileMessageBubble(
    message: Message,
    media: MessageMedia,
    senderName: String?,
    replied: Message?,
    repliedSenderName: String?,
    /** Yuklab olinmoqda (0..1). `null` — yuklab olinmayapti. */
    downloadProgress: Float?,
    onReplyClick: () -> Unit,
    onCancelUpload: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = SwiftTheme.colors
    val resources = LocalContext.current.resources
    val isOut = message.isMine
    val bubble = bubbleColors(isOut)
    // Server fayl nomini saqlamaydi — u `body`da keladi.
    val fileName = message.text?.takeIf { it.isNotBlank() } ?: stringResource(R.string.file)
    val upload = message.upload?.takeIf { message.status == MessageStatus.SENDING }
    val progressTrack = if (isOut) colors.replyOut else colors.surface2
    val progressColor = if (isOut) colors.tickRead else colors.primary

    Column(
        modifier = modifier
            .width(FileBubbleWidth)
            .background(bubble.container, if (isOut) OutgoingShape else IncomingShape)
            .padding(start = 10.dp, end = 10.dp, top = 10.dp, bottom = 6.dp)
    ) {
        if (senderName != null) {
            Text(
                text = senderName,
                color = senderNameColor(message.senderId),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
        if (replied != null) {
            ReplyQuote(
                senderName = repliedSenderName,
                snippet = snippetOf(replied),
                colors = bubble,
                onClick = onReplyClick,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(colors.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                when {
                    upload != null -> UploadRing(progress = upload.fraction, size = 46.dp, onCancel = onCancelUpload, ringColor = colors.onPrimary, scrim = Color.Transparent)
                    downloadProgress != null -> ProgressRing(progress = downloadProgress, size = 46.dp, color = colors.onPrimary, track = Color.Transparent)
                    else -> Icon(
                        painter = painterResource(if (isOut || media.localPath != null) DesignR.drawable.ic_file else DesignR.drawable.ic_download),
                        contentDescription = stringResource(R.string.download),
                        tint = colors.onPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = fileName,
                    color = bubble.content,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val progress = upload?.fraction ?: downloadProgress
                if (progress != null) {
                    LinearProgressIndicator(
                        progress = { progress },
                        color = progressColor,
                        trackColor = progressTrack,
                        strokeCap = StrokeCap.Round,
                        gapSize = 0.dp,
                        drawStopIndicator = {},
                        modifier = Modifier.fillMaxWidth().height(4.dp)
                    )
                    val done = upload?.sentBytes ?: (media.sizeBytes * progress).toLong()
                    Text(text = formatSizeProgress(done, media.sizeBytes, resources), color = bubble.meta, fontSize = 13.sp)
                } else {
                    Text(
                        text = stringResource(R.string.file_meta, formatSize(media.sizeBytes, resources), fileTypeLabel(fileName, media.mimeType)),
                        color = if (isOut) bubble.meta else colors.text2,
                        fontSize = 13.sp
                    )
                }
            }
        }
        Box(modifier = Modifier.fillMaxWidth().padding(top = 2.dp), contentAlignment = Alignment.CenterEnd) {
            MessageMeta(message = message, color = bubble.meta)
        }
    }
}

/** Nisbat saqlangan balandlik, 120..300 oralig'ida. O'lcham noma'lum bo'lsa — o'rtacha 180. */
private fun visualHeight(media: MessageMedia): Dp {
    val width = media.width ?: 0
    val height = media.height ?: 0
    if (width <= 0 || height <= 0) return 180.dp
    return (VisualInnerWidth * (height.toFloat() / width)).coerceIn(120.dp, 300.dp)
}

/** Media ustidagi yarim shaffof pill: vaqt · holat belgisi (oq). */
@Composable
private fun MediaMetaPill(message: Message, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .background(PillBackground, RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = formatMessageTime(message.createdAt), color = Color.White, fontSize = 12.sp)
        if (message.isMine) {
            val icon = when (message.status) {
                MessageStatus.SENDING -> DesignR.drawable.ic_clock
                MessageStatus.SENT -> DesignR.drawable.ic_check_single
                MessageStatus.DELIVERED, MessageStatus.READ -> DesignR.drawable.ic_check_double
                MessageStatus.FAILED -> DesignR.drawable.ic_alert_circle
            }
            Icon(painter = painterResource(icon), contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
        }
    }
}

/** Media ustidagi kichik yarim shaffof yorliq (davomiylik, yuklash hajmi). */
@Composable
private fun Pill(text: String, modifier: Modifier = Modifier, bold: Boolean = false) {
    Text(
        text = text,
        color = Color.White,
        fontSize = 12.sp,
        fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal,
        modifier = modifier
            .background(PillBackground, RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    )
}

/** Markazdagi "play" doirasi (videoda). */
@Composable
internal fun PlayBadge(size: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(size)
            .background(PillBackground, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(DesignR.drawable.ic_play),
            contentDescription = stringResource(R.string.play_video),
            tint = Color.White,
            // Uchburchak ko'z bilan markazda ko'rinishi uchun biroz o'ngga (padding — offset emas).
            modifier = Modifier.padding(start = size * 0.06f).size(size * 0.42f)
        )
    }
}

/** Yuklash halqasi + × (bosilsa bekor qilinadi). */
@Composable
private fun UploadRing(
    progress: Float,
    size: Dp,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    ringColor: Color = Color.White,
    scrim: Color = Color.Black.copy(alpha = 0.45f)
) {
    Box(
        modifier = modifier
            .size(size)
            .background(scrim, CircleShape)
            .clip(CircleShape)
            .clickable(onClick = onCancel),
        contentAlignment = Alignment.Center
    ) {
        ProgressRing(progress = progress, size = size, color = ringColor, track = ringColor.copy(alpha = 0.25f))
        Icon(
            painter = painterResource(DesignR.drawable.ic_close),
            contentDescription = stringResource(R.string.cancel_upload),
            tint = ringColor,
            modifier = Modifier.size(size * 0.34f)
        )
    }
}

/** Canvas halqa (spec: r = size/2 − 4, qalinlik 3, yuqoridan soat yo'nalishida). */
@Composable
private fun ProgressRing(progress: Float, size: Dp, color: Color, track: Color) {
    Canvas(modifier = Modifier.size(size)) {
        val stroke = 3.dp.toPx()
        val inset = 4.dp.toPx() + stroke / 2
        val arcSize = androidx.compose.ui.geometry.Size(this.size.width - inset * 2, this.size.height - inset * 2)
        val topLeft = androidx.compose.ui.geometry.Offset(inset, inset)
        drawArc(color = track, startAngle = 0f, sweepAngle = 360f, useCenter = false, topLeft = topLeft, size = arcSize, style = Stroke(stroke))
        // Juda kichik progress ham ko'rinsin — "hech narsa bo'lmayapti" degan taassurot qolmasin.
        val sweep = 360f * progress.coerceIn(0.02f, 1f)
        drawArc(color = color, startAngle = -90f, sweepAngle = sweep, useCenter = false, topLeft = topLeft, size = arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
    }
}
