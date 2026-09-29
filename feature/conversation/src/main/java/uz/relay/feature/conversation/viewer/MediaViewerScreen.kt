package uz.relay.feature.conversation.viewer

import android.os.Build
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.ContentFrame
import androidx.media3.ui.compose.state.rememberMuteButtonState
import androidx.media3.ui.compose.state.rememberPlayPauseButtonState
import androidx.media3.ui.compose.state.rememberProgressStateWithTickInterval
import coil3.compose.AsyncImage
import kotlinx.coroutines.flow.distinctUntilChanged
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import uz.relay.core.designsystem.R as DesignR
import uz.relay.domain.model.MediaKind
import uz.relay.feature.conversation.R
import uz.relay.feature.conversation.chat.components.MediaPreview
import uz.relay.feature.conversation.chat.components.PlayBadge
import uz.relay.feature.conversation.util.dayStartMillis
import uz.relay.feature.conversation.util.formatDateSeparator
import uz.relay.feature.conversation.util.formatDuration
import uz.relay.feature.conversation.util.formatMessageTime
import uz.relay.feature.conversation.util.messageRes
import java.io.File

/** Spec: ko'ruvchi temadan qat'i nazar doim qora; panellar qora 55%, seek chizig'i #A59DFF. */
private val ChromeBackground = Color.Black.copy(alpha = 0.55f)
private val SeekColor = Color(0xFFA59DFF)
private val Secondary = Color.White.copy(alpha = 0.7f)

private const val MAX_ZOOM = 5f
private const val DOUBLE_TAP_ZOOM = 2.5f

@Composable
internal fun MediaViewerScreen(chatId: String, clientMessageId: String) {
    val viewModel = hiltViewModel<MediaViewerViewModel, MediaViewerViewModel.Factory>(
        creationCallback = { factory -> factory.create(chatId, clientMessageId) }
    )
    val uiState by viewModel.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    viewModel.collectSideEffect { sideEffect ->
        when (sideEffect) {
            MediaViewerContract.SideEffect.Saved -> snackbarHostState.showSnackbar(context.getString(R.string.saved_to_gallery))
            is MediaViewerContract.SideEffect.ShowError ->
                snackbarHostState.showSnackbar(context.getString(sideEffect.error.messageRes()))
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        val initialIndex = uiState.initialIndex
        // Pager ro'yxat va boshlang'ich sahifa ma'lum bo'lgandan keyin quriladi — aks holda 0-sahifadan "sakraydi".
        if (initialIndex != null) {
            ViewerContent(
                uiState = uiState,
                initialIndex = initialIndex,
                player = viewModel.player,
                onEventDispatcher = viewModel::onEventDispatcher
            )
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 120.dp)
        )
    }
}

@Composable
private fun BoxScope.ViewerContent(
    uiState: MediaViewerContract.UiState,
    initialIndex: Int,
    player: Player,
    onEventDispatcher: (MediaViewerContract.Intent) -> Unit
) {
    val items = uiState.items
    val pagerState = rememberPagerState(initialPage = initialIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0))) { items.size }
    // Panellar tegish bilan yashiriladi/ko'rsatiladi (rasmni to'liq ko'rish uchun).
    var chromeVisible by remember { mutableStateOf(true) }
    var zoomed by remember { mutableStateOf(false) }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }
            .distinctUntilChanged()
            .collect { page ->
                zoomed = false
                onEventDispatcher(MediaViewerContract.Intent.OnPageChange(page))
            }
    }

    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
        // Kattalashtirilgan rasmni surish sahifani almashtirmasin.
        userScrollEnabled = !zoomed,
        key = { index -> items[index].key }
    ) { page ->
        val item = items[page]
        when (item.media.kind) {
            MediaKind.VIDEO -> VideoPage(
                item = item,
                player = player,
                isCurrent = page == pagerState.settledPage,
                onTap = { chromeVisible = !chromeVisible }
            )
            else -> ZoomableImage(
                item = item,
                onZoomChange = { if (page == pagerState.currentPage) zoomed = it },
                onTap = { chromeVisible = !chromeVisible }
            )
        }
    }

    val current = items.getOrNull(pagerState.currentPage) ?: return
    AnimatedVisibility(visible = chromeVisible, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.align(Alignment.TopCenter)) {
        TopBar(
            item = current,
            senderName = if (current.message.isMine) stringResource(R.string.sys_you) else uiState.userNames[current.message.senderId].orEmpty(),
            isSaving = uiState.isSaving,
            onBack = { onEventDispatcher(MediaViewerContract.Intent.OnBack) },
            onSave = { onEventDispatcher(MediaViewerContract.Intent.OnSave(current)) }
        )
    }
    AnimatedVisibility(visible = chromeVisible, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.align(Alignment.BottomCenter)) {
        BottomBar(
            item = current,
            position = pagerState.currentPage + 1,
            total = items.size,
            player = player.takeIf { current.media.kind == MediaKind.VIDEO && pagerState.currentPage == pagerState.settledPage }
        )
    }
}

/** 64dp: orqaga · ism + "Bugun, 10:20" · saqlash (Android 10+: MediaStore ruxsatsiz ishlaydi). */
@Composable
private fun TopBar(item: ViewerItem, senderName: String, isSaving: Boolean, onBack: () -> Unit, onSave: () -> Unit) {
    val resources = LocalContext.current.resources
    val createdAt = item.message.createdAt
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ChromeBackground)
            .statusBarsPadding()
            .height(64.dp)
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(painter = painterResource(DesignR.drawable.ic_arrow_left), contentDescription = stringResource(R.string.close), tint = Color.White)
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(text = senderName, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                text = stringResource(R.string.viewer_when, formatDateSeparator(dayStartMillis(createdAt), resources), formatMessageTime(createdAt)),
                color = Secondary,
                fontSize = 13.sp
            )
        }
        // Serverga hali yetmagan (o'zim yuborayotgan) media — saqlash uchun lokal nusxa bor, lekin u baribir galereyadan olingan.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && item.media.mediaId != null) {
            if (isSaving) {
                Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                }
            } else {
                IconButton(onClick = onSave) {
                    Icon(painter = painterResource(DesignR.drawable.ic_download), contentDescription = stringResource(R.string.download), tint = Color.White, modifier = Modifier.size(22.dp))
                }
            }
        }
    }
}

/** Rasm: izoh + "3 / 12". Video: seek chizig'i, play/pauza, vaqt, ovoz. */
@Composable
private fun BottomBar(item: ViewerItem, position: Int, total: Int, player: Player?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ChromeBackground)
            .navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (player != null) {
            VideoControls(player = player)
        } else {
            item.message.text?.takeIf { it.isNotBlank() }?.let { caption ->
                Text(text = caption, color = Color.White, fontSize = 15.sp, lineHeight = 21.sp, maxLines = 4, overflow = TextOverflow.Ellipsis)
            }
        }
        Text(
            text = stringResource(R.string.pager_position, position, total),
            color = Secondary,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Media3 Compose holatlari (play/pauza, progress, ovoz) pleyerga o'zi obuna bo'ladi — pleyer listener'larini
 * qo'lda yozish shart emas. Seek chizig'i surilayotganda pozitsiya lokal, qo'yib yuborilganda `seekTo`.
 */
@OptIn(UnstableApi::class)
@Composable
private fun VideoControls(player: Player) {
    val scope = rememberCoroutineScope()
    val playPause = rememberPlayPauseButtonState(player)
    val progress = rememberProgressStateWithTickInterval(player, 250L, scope)
    val mute = rememberMuteButtonState(player)
    val duration = progress.durationMs.coerceAtLeast(0)
    var dragging by remember { mutableStateOf<Float?>(null) }
    val fraction = dragging ?: if (duration > 0) progress.currentPositionMs.toFloat() / duration else 0f

    Slider(
        value = fraction.coerceIn(0f, 1f),
        onValueChange = { dragging = it },
        onValueChangeFinished = {
            dragging?.let { player.seekTo((it * duration).toLong()) }
            dragging = null
        },
        enabled = duration > 0,
        colors = SliderDefaults.colors(
            thumbColor = Color.White,
            activeTrackColor = SeekColor,
            inactiveTrackColor = Color.White.copy(alpha = 0.3f),
            disabledThumbColor = Color.White.copy(alpha = 0.5f),
            disabledInactiveTrackColor = Color.White.copy(alpha = 0.3f)
        ),
        modifier = Modifier.height(16.dp)
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = playPause::onClick, enabled = playPause.isEnabled, modifier = Modifier.size(44.dp)) {
            Icon(
                painter = painterResource(if (playPause.showPlay) DesignR.drawable.ic_play else DesignR.drawable.ic_pause),
                contentDescription = stringResource(if (playPause.showPlay) R.string.play else R.string.pause),
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
        val position = dragging?.let { (it * duration).toLong() } ?: progress.currentPositionMs
        Text(text = "${formatDuration(position)} / ${formatDuration(duration)}", color = Color.White, fontSize = 13.sp)
        Box(modifier = Modifier.weight(1f))
        IconButton(onClick = mute::onClick, enabled = mute.isEnabled, modifier = Modifier.size(44.dp)) {
            Icon(
                painter = painterResource(if (mute.showMuted) DesignR.drawable.ic_volume_x else DesignR.drawable.ic_volume),
                contentDescription = stringResource(if (mute.showMuted) R.string.unmute_sound else R.string.mute_sound),
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

/**
 * Video sahifasi. Pleyer faqat joriy sahifada ulanadi; qo'shni sahifalarda — poster (bo'lsa) va "play" belgisi.
 * ContentFrame videoning nisbatini o'zi saqlaydi va birinchi kadr kelguncha qora "shutter" ko'rsatadi.
 */
@OptIn(UnstableApi::class)
@Composable
private fun VideoPage(item: ViewerItem, player: Player, isCurrent: Boolean, onTap: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) { detectTapGestures(onTap = { onTap() }) },
        contentAlignment = Alignment.Center
    ) {
        if (isCurrent) {
            val playPause = rememberPlayPauseButtonState(player)
            ContentFrame(player = player, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
            if (playPause.showPlay) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        .clickable(enabled = playPause.isEnabled, onClick = playPause::onClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(DesignR.drawable.ic_play),
                        contentDescription = stringResource(R.string.play),
                        tint = Color.White,
                        modifier = Modifier.padding(start = 4.dp).size(30.dp)
                    )
                }
            }
        } else {
            MediaPreview(media = item.media, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
            PlayBadge(size = 72.dp)
        }
    }
}

/**
 * Pinch-zoom (1..5×), kattalashtirilganda surish, ikki marta tegish — 2.5× ↔ 1×.
 *
 * Nega o'z gesture'imiz (`transformable` emas): transformable 1× da ham har qanday surishni o'zi yutadi va
 * pager sahifa almashtira olmay qoladi. Bu yerda bir barmoq bilan surish faqat kattalashtirilgan holatda
 * ushlanadi, aks holda pager'ga o'tadi. Ko'chirish `graphicsLayer` bilan (layout'ga tegmaydi, offset emas).
 */
@Composable
private fun ZoomableImage(item: ViewerItem, onZoomChange: (Boolean) -> Unit, onTap: () -> Unit) {
    var scale by remember { mutableFloatStateOf(1f) }
    var translation by remember { mutableStateOf(Offset.Zero) }
    var size by remember { mutableStateOf(IntSize.Zero) }

    fun clamp(offset: Offset, forScale: Float): Offset {
        val maxX = (size.width * (forScale - 1)) / 2
        val maxY = (size.height * (forScale - 1)) / 2
        return Offset(offset.x.coerceIn(-maxX, maxX), offset.y.coerceIn(-maxY, maxY))
    }

    fun update(newScale: Float, newTranslation: Offset) {
        scale = newScale
        translation = if (newScale <= 1f) Offset.Zero else clamp(newTranslation, newScale)
        onZoomChange(newScale > 1f)
    }

    val model: Any? = item.media.localPath?.let(::File)?.takeIf { it.exists() } ?: item.media.url
    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { size = it }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent()
                        val multiTouch = event.changes.count { it.pressed } > 1
                        if (multiTouch || scale > 1f) {
                            val newScale = (scale * event.calculateZoom()).coerceIn(1f, MAX_ZOOM)
                            update(newScale, translation + event.calculatePan())
                            event.changes.forEach { if (it.positionChanged()) it.consume() }
                        }
                    } while (event.changes.any { it.pressed })
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onTap() },
                    onDoubleTap = { if (scale > 1f) update(1f, Offset.Zero) else update(DOUBLE_TAP_ZOOM, Offset.Zero) }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = model,
            contentDescription = stringResource(R.string.photo),
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = translation.x
                    translationY = translation.y
                }
        )
    }
}
