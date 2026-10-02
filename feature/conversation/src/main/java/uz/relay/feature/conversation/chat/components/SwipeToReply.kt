package uz.relay.feature.conversation.chat.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitHorizontalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import uz.relay.core.designsystem.R as DesignR
import uz.relay.core.designsystem.theme.SwiftTheme
import uz.relay.core.designsystem.util.rememberGestureThresholdHaptic

/** Reply amali ishga tushadigan masofa. */
private val ReplyThreshold = 56.dp

/** Qator bundan uzoqqa siljimaydi (chegaradan keyin "rezina" kabi sekinlashadi). */
private val MaxShift = 80.dp

/**
 * Telegram'dagidek "surib javob berish": xabar qatorini **chapga** sursangiz u barmoq ortidan siljiydi, o'ng
 * chetda ↩ belgisi paydo bo'ladi. [ReplyThreshold]dan o'tilganda yengil titrash bo'ladi va qo'yib yuborilganda
 * [onReply] chaqiriladi; qator joyiga qaytadi.
 *
 * Gesture faqat chapga yo'nalishni ushlaydi:
 *  - o'ngga surish iste'mol qilinmaydi — u ota-ekranning "orqaga qaytish" gesture'iga o'tadi;
 *  - vertikal harakat ro'yxat scroll'i bilan to'qnashmaydi (gorizontal chegara o'tilmaguncha hech narsa olinmaydi).
 * Siljish `graphicsLayer` orqali (layout qayta hisoblanmaydi, `offset` ishlatilmaydi).
 */
@Composable
internal fun SwipeToReplyBox(
    enabled: Boolean,
    onReply: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val density = LocalDensity.current
    val threshold = with(density) { ReplyThreshold.toPx() }
    val maxShift = with(density) { MaxShift.toPx() }
    val shift = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val haptic = rememberGestureThresholdHaptic()

    val gesture = if (!enabled) Modifier else Modifier.pointerInput(onReply) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            var dragged = 0f
            // Faqat chapga (manfiy) chegaradan o'tilsagina gesture bizniki; o'ngga — iste'mol qilinmaydi.
            val start = awaitHorizontalTouchSlopOrCancellation(down.id) { change, over ->
                if (over < 0f) {
                    change.consume()
                    dragged = over
                }
            } ?: return@awaitEachGesture
            var armed = false
            scope.launch { shift.snapTo(resist(dragged, threshold, maxShift)) }
            horizontalDrag(start.id) { change ->
                dragged = (dragged + change.positionChange().x).coerceAtMost(0f)
                change.consume()
                val visual = resist(dragged, threshold, maxShift)
                scope.launch { shift.snapTo(visual) }
                val nowArmed = -dragged >= threshold
                if (nowArmed && !armed) haptic()
                armed = nowArmed
            }
            if (armed) onReply()
            scope.launch { shift.animateTo(0f, spring(stiffness = 600f)) }
        }
    }

    Box(modifier = modifier.fillMaxWidth().then(gesture)) {
        val progress = (-shift.value / threshold).coerceIn(0f, 1f)
        if (progress > 0f) {
            val colors = SwiftTheme.colors
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 12.dp)
                    .size(32.dp)
                    .graphicsLayer {
                        alpha = progress
                        scaleX = 0.6f + 0.4f * progress
                        scaleY = 0.6f + 0.4f * progress
                    }
                    .background(colors.chip, CircleShape)
                    .testTag(SWIPE_REPLY_ICON_TAG),
                contentAlignment = Alignment.Center
            ) {
                Icon(painter = painterResource(DesignR.drawable.ic_reply), contentDescription = null, tint = colors.onChip, modifier = Modifier.size(18.dp))
            }
        }
        Box(modifier = Modifier.fillMaxWidth().graphicsLayer { translationX = shift.value }, content = content)
    }
}

/** Chegaragacha qator barmoq bilan bir xil siljiydi, keyin qarshilik bilan [maxShift]gacha. */
private fun resist(dragged: Float, threshold: Float, maxShift: Float): Float {
    val distance = -dragged
    if (distance <= threshold) return dragged
    val extra = (distance - threshold) * 0.35f
    return -(threshold + extra).coerceAtMost(maxShift)
}

internal const val SWIPE_REPLY_ICON_TAG = "swipe_reply_icon"
