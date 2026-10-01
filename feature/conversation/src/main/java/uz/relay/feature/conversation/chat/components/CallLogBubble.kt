package uz.relay.feature.conversation.chat.components

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.relay.core.designsystem.R as DesignR
import uz.relay.core.designsystem.theme.SwiftTheme
import uz.relay.domain.model.CallLog
import uz.relay.domain.model.CallLogFormat
import uz.relay.domain.model.CallOutcome
import uz.relay.domain.model.Message
import uz.relay.feature.conversation.R

/**
 * Chatdagi qo'ng'iroq yozuvi (Telegram'dagi "Chiquvchi qoʻngʻiroq · 2:31"). Serverda u oddiy matnli xabar
 * (`📞 Call · audio · 2:31`, [CallLogFormat]) — bu yerda tilga mos sarlavha, ikonka va davomiylik bilan chiziladi.
 * Bosilsa o'sha turdagi qo'ng'iroq qayta boshlanadi (bosishni MessageRow ushlaydi); guruhda — video chatga
 * qo'shilish. Guruh yozuvi "boshlandi" yoki "tugadi · davomiylik".
 */
@Composable
internal fun CallLogBubble(message: Message, log: CallLog, modifier: Modifier = Modifier) {
    val colors = SwiftTheme.colors
    val isOut = message.isMine
    val bubble = bubbleColors(isOut)
    // Javobsiz / rad etilgan / bekor qilingan — qizil: Telegram'dagidek "suhbat bo'lmadi" ko'zga tashlansin.
    val failed = log.outcome != CallOutcome.ANSWERED && log.outcome != CallOutcome.STARTED
    val accent = if (failed && !isOut) colors.error else bubble.replyAccent

    Row(
        modifier = modifier
            .widthIn(min = 200.dp, max = 264.dp)
            .background(bubble.container, if (isOut) OutgoingShape else IncomingShape)
            .padding(start = 10.dp, end = 10.dp, top = 9.dp, bottom = 7.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(bubble.replyContainer, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(if (log.video) DesignR.drawable.ic_video else DesignR.drawable.ic_phone_call),
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(20.dp)
            )
        }
        Column(modifier = Modifier.weight(1f, fill = false), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = stringResource(callTitleRes(log, isOut)),
                color = bubble.content,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                if (log.outcome == CallOutcome.ANSWERED) {
                    Text(text = CallLogFormat.duration(log.durationSeconds), color = bubble.meta, fontSize = 13.sp)
                }
                MessageMeta(message = message, color = bubble.meta)
            }
        }
    }
}

/**
 * Sarlavha yo'nalishga qarab: men qo'ng'iroq qilgan bo'lsam (xabar meniki) — "chiquvchi / javobsiz / bekor qilingan",
 * menga qo'ng'iroq qilingan bo'lsa — "kiruvchi / o'tkazib yuborilgan".
 */
@StringRes
internal fun callTitleRes(log: CallLog, isMine: Boolean): Int = when (log.outcome) {
    CallOutcome.STARTED -> R.string.call_group_started
    CallOutcome.ANSWERED -> when {
        log.group -> R.string.call_group_ended
        isMine -> R.string.call_outgoing
        else -> R.string.call_incoming
    }
    CallOutcome.MISSED -> if (isMine) R.string.call_no_answer else R.string.call_missed
    CallOutcome.DECLINED -> R.string.call_declined
    CallOutcome.CANCELED -> if (isMine) R.string.call_canceled else R.string.call_missed
}
