package uz.relay.feature.calls.call.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import io.getstream.video.android.compose.ui.components.call.renderer.FloatingParticipantVideo
import io.getstream.video.android.compose.ui.components.call.renderer.ParticipantVideo
import io.getstream.video.android.compose.ui.components.call.renderer.ParticipantsLayout
import io.getstream.video.android.compose.ui.components.call.renderer.RegularVideoRendererStyle
import io.getstream.video.android.compose.ui.components.call.renderer.VideoRendererStyle
import io.getstream.video.android.compose.ui.components.call.renderer.internal.ScreenShareVideoRenderer
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.model.ScreenSharingSession

/**
 * Video to'liq ekranda, ustida esa bizning tepa panel (ism, vaqt, qo'l chip'i) turadi. Stream reaksiya emojisini va
 * mening suzuvchi kameramni plitkaning TEPA-O'NG burchagiga qo'yadi — ular status bar va panel ostida qolib
 * ko'rinmasdi. Shuning uchun shu yerdan panel ostidagi joy hisoblanadi.
 */
private val TopBarSpace = 104.dp
private val EdgeSpace = 12.dp

/**
 * "Tepa-o'ng, lekin panel ostida" joylashuv. `Modifier.offset` emas, layout darajasidagi `Alignment`: element
 * plitka ichida bo'ladi va kichik plitkada pastga chiqib ketmaydi ([top] plitka balandligi bilan cheklanadi).
 */
private class BelowTopBarAlignment(private val top: Int, private val end: Int) : Alignment {
    override fun align(size: IntSize, space: IntSize, layoutDirection: LayoutDirection): IntOffset {
        val x = if (layoutDirection == LayoutDirection.Ltr) space.width - size.width - end else end
        val y = top.coerceAtMost(space.height - size.height)
        return IntOffset(x.coerceAtLeast(0), y.coerceAtLeast(0))
    }
}

@Composable
internal fun rememberBelowTopBarAlignment(): Alignment {
    val density = LocalDensity.current
    val statusBar = WindowInsets.statusBars.getTop(density)
    return remember(density, statusBar) {
        with(density) { BelowTopBarAlignment(top = statusBar + TopBarSpace.roundToPx(), end = EdgeSpace.roundToPx()) }
    }
}

/**
 * `CallContent`ning video qismi:
 *  - oddiy holatda — Stream'ning `ParticipantsLayout`i (reaksiya va suzuvchi kamera panel ostida);
 *  - kimdir ekran ulashsa — [ScreenShareWithCameras].
 */
@Composable
internal fun RowScope.CallVideoContent(call: Call) {
    val belowTopBar = rememberBelowTopBarAlignment()
    val style = remember(belowTopBar) { RegularVideoRendererStyle(reactionPosition = belowTopBar) }
    val session by call.state.screenSharingSession.collectAsState()
    val sharing = session
    if (sharing != null) {
        ScreenShareWithCameras(call = call, session = sharing, modifier = Modifier.weight(1f))
    } else {
        ParticipantsLayout(
            call = call,
            modifier = Modifier.fillMaxSize().weight(1f),
            style = style,
            floatingVideoRenderer = { floatingCall, parentSize -> MyFloatingVideo(floatingCall, parentSize, belowTopBar) }
        )
    }
}

/** Mening suzuvchi kameram — panel ostida; reaksiya kichik oynaning o'rtasida (u yerda joy kam). */
@Composable
private fun BoxScope.MyFloatingVideo(call: Call, parentSize: IntSize, alignment: Alignment) {
    val me by call.state.me.collectAsState()
    val participant = me ?: return
    FloatingParticipantVideo(
        call = call,
        participant = participant,
        parentBounds = parentSize,
        alignment = alignment,
        style = RegularVideoRendererStyle(isShowingConnectionQualityIndicator = false, reactionPosition = Alignment.Center)
    )
}

/**
 * Ekran ulashilganda (Telegram'dagidek): ulashilgan ekran butun fonda (proporsiyasi saqlanadi, kattalashtirsa
 * bo'ladi), kameralar esa o'ngda, panel ostida kichik oynachalarda — birinchi bo'lib ulashayotgan odamning kamerasi.
 * Stream'ning standart layout'i ekranni yuqori 45%ga, kameralarni uning ostiga qo'yardi — ular pastki tugmalar
 * ostida qolib ko'rinmasdi.
 */
@Composable
private fun ScreenShareWithCameras(call: Call, session: ScreenSharingSession, modifier: Modifier = Modifier) {
    val participants by call.state.participants.collectAsState()
    val sharerId = session.participant.sessionId
    val cameras = remember(participants, sharerId) {
        participants.sortedBy { it.sessionId != sharerId }.take(MAX_SIDE_CAMERAS)
    }
    val tileStyle: VideoRendererStyle = remember {
        RegularVideoRendererStyle(isShowingConnectionQualityIndicator = false, reactionPosition = Alignment.Center)
    }
    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        ScreenShareVideoRenderer(call = call, session = session, modifier = Modifier.fillMaxSize(), isShowConnectionQualityIndicator = false)
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = TopBarSpace, end = EdgeSpace),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            cameras.forEach { participant ->
                ParticipantVideo(
                    call = call,
                    participant = participant,
                    modifier = Modifier.size(width = 104.dp, height = 140.dp).clip(RoundedCornerShape(14.dp)),
                    style = tileStyle
                )
            }
        }
    }
}

/** O'ngdagi kamera oynachalari soni — pastki tugmalarga yetib bormasin. */
private const val MAX_SIDE_CAMERAS = 3
