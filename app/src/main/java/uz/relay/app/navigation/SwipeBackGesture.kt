package uz.relay.app.navigation

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitHorizontalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.navigationevent.DirectNavigationEventInput
import androidx.navigationevent.NavigationEvent
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner

/** Barmoq ekran kengligining shuncha qismidan o'tsa, qo'yib yuborilganda orqaga qaytiladi. */
private const val COMPLETE_PROGRESS = 0.35f

/** Tez "otish" (fling) — masofa kichik bo'lsa ham orqaga qaytiladi. */
private val FlingVelocity = 700.dp

/**
 * Telegram'dagidek "o'ngga surib orqaga qaytish": ekranning **istalgan joyidan** o'ngga sursangiz, ekran barmoq
 * ortidan siljiydi va ostidan oldingi ekran ko'rinadi; qo'yib yuborilganda yetarli surilgan bo'lsa orqaga qaytiladi,
 * aks holda joyiga qaytadi.
 *
 * Nega o'zimiz animatsiya chizmaymiz: gesture tizimdagi predictive back bilan **bir xil** hodisalarni
 * ([DirectNavigationEventInput]) ilovaning `NavigationEventDispatcher`iga yuboradi. Natijada:
 *  - NavDisplay o'zining `predictivePopTransitionSpec` animatsiyasini o'ynaydi (oldingi ekran ostida ko'rinadi);
 *  - ekranlardagi `BackHandler`lar (masalan, xabar menyusi, guruh yaratishning 2-qadami) avvalgidek birinchi
 *    bo'lib ishlaydi — "orqaga" qayerda qanday ishlashi bitta joyda.
 *
 * Bolalar ustunlik qiladi: gorizontal pager, xabarni chapga surish (reply) yoki gorizontal ro'yxat harakatni
 * iste'mol qilsa, bu gesture boshlanmaydi. Faqat **o'ngga** va gorizontal harakat ushlanadi.
 */
@Composable
internal fun Modifier.swipeBack(enabled: Boolean): Modifier {
    val dispatcher = LocalNavigationEventDispatcherOwner.current?.navigationEventDispatcher ?: return this
    val input = remember(dispatcher) { DirectNavigationEventInput() }
    DisposableEffect(dispatcher, input) {
        dispatcher.addInput(input)
        onDispose { dispatcher.removeInput(input) }
    }
    if (!enabled) return this
    val flingPx = with(LocalDensity.current) { FlingVelocity.toPx() }

    return pointerInput(input) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            var dragged = 0f
            val start = awaitHorizontalTouchSlopOrCancellation(down.id) { change, over ->
                // Faqat o'ngga; chapga surish (masalan, xabarga javob) bolalarga qoladi.
                if (over > 0f && !change.isConsumed) {
                    change.consume()
                    dragged = over
                }
            } ?: return@awaitEachGesture
            val width = size.width.toFloat().coerceAtLeast(1f)
            val velocity = VelocityTracker()
            velocity.addPointerInputChange(start)
            input.backStarted(event(dragged / width, start.position.x, start.position.y))
            val finished = horizontalDrag(start.id) { change ->
                dragged = (dragged + change.positionChange().x).coerceAtLeast(0f)
                change.consume()
                velocity.addPointerInputChange(change)
                input.backProgressed(event(dragged / width, change.position.x, change.position.y))
            }
            val fling = velocity.calculateVelocity().x
            val complete = finished && (dragged / width > COMPLETE_PROGRESS || fling > flingPx)
            if (complete) input.backCompleted() else input.backCancelled()
        }
    }
}

private fun event(progress: Float, x: Float, y: Float) = NavigationEvent(
    swipeEdge = NavigationEvent.EDGE_LEFT,
    progress = progress.coerceIn(0f, 1f),
    touchX = x,
    touchY = y,
    frameTimeMillis = System.currentTimeMillis()
)
