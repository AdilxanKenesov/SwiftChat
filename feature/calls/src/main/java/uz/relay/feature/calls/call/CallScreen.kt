package uz.relay.feature.calls.call

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import io.getstream.video.android.compose.pip.rememberIsInPipMode
import io.getstream.video.android.compose.theme.StreamColors
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.call.activecall.AudioCallContent
import io.getstream.video.android.compose.ui.components.call.activecall.CallContent
import io.getstream.video.android.compose.ui.components.call.ringing.RingingCallContent
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.call.state.CallAction
import io.getstream.video.android.core.pip.PictureInPictureConfiguration
import kotlinx.coroutines.delay
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import uz.relay.core.designsystem.component.SwiftSnackbarHost
import uz.relay.core.designsystem.theme.Brand
import uz.relay.feature.calls.R
import uz.relay.feature.calls.call.components.CallControls
import uz.relay.feature.calls.call.components.CallExtras
import uz.relay.feature.calls.call.components.CallTopBar
import uz.relay.feature.calls.call.components.CallVideoContent
import uz.relay.feature.calls.call.components.RaisedHandsChip

/**
 * Qo'ng'iroq ekrani. Stream'ning tayyor Compose komponentlari ishlatiladi (`VideoTheme` ichida bo'lishi shart):
 *  - [RingingCallContent] jiringlash holatiga qarab kiruvchi / chiquvchi ko'rinishni o'zi tanlaydi;
 *  - qabul qilingach — video uchun [CallContent], audio uchun [AudioCallContent].
 * Tugmalar amallari ViewModel'ga boradi ([CallContract.Intent.OnCallAction]).
 *
 * Ochiladi: shaxsiy chat sarlavhasidagi 📞/🎥 (chiquvchi) yoki ilova darajasidagi kiruvchi qo'ng'iroq (MainViewModel).
 */
@Composable
internal fun CallScreen(callId: String, video: Boolean?, chatId: String?, group: Boolean) {
    val viewModel = hiltViewModel<CallViewModel, CallViewModel.Factory>(
        creationCallback = { factory -> factory.create(callId, video, chatId, group) }
    )
    val uiState by viewModel.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current

    viewModel.collectSideEffect { sideEffect ->
        when (sideEffect) {
            is CallContract.SideEffect.ShowError -> snackbarHostState.showSnackbar(resources.getString(R.string.call_failed))
            CallContract.SideEffect.NoAnswer -> snackbarHostState.showSnackbar(resources.getString(R.string.call_no_answer))
        }
    }

    BackHandler { viewModel.onEventDispatcher(CallContract.Intent.OnBack) }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        val call = viewModel.call
        if (uiState.unavailable || call == null) {
            Text(
                text = stringResource(R.string.calls_unavailable),
                color = Color.White,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center).padding(32.dp)
            )
        } else {
            var permissionsSettled by remember { mutableStateOf(false) }
            CallPermissions(isVideo = uiState.isVideo, onSettled = { permissionsSettled = true })
            // Gapirayotgan odam ramkasi Stream'da `brandPrimary` rangida — ilova brend rangiga moslanadi.
            VideoTheme(colors = StreamColors.defaultColors().copy(brandPrimary = Brand)) {
                if (uiState.isGroup) {
                    // Guruh xonasi: jiringlash bosqichi yo'q — to'liq ekranli xona. Ruxsat dialogi tugaguncha
                    // ko'rsatilmaydi: dialog activity'ni pauza qiladi, Stream esa pauzada PiP'ga o'tib ketardi.
                    if (permissionsSettled) FullScreenVideoCall(
                        call = call,
                        group = true,
                        uiState = uiState,
                        onEventDispatcher = viewModel::onEventDispatcher
                    )
                } else {
                    CallContentHost(call = call, uiState = uiState, onEventDispatcher = viewModel::onEventDispatcher)
                }
            }
        }
        SwiftSnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.TopCenter))
    }
}

/**
 * Jiringlash → faol qo'ng'iroq. Rad etilsa yoki javob berilmasa ekran yopiladi (slot ichidagi LaunchedEffect —
 * u slot ko'rinishi bilan bir marta ishlaydi).
 */
@Composable
private fun CallContentHost(call: Call, uiState: CallContract.UiState, onEventDispatcher: (CallContract.Intent) -> Unit) {
    val isVideo = uiState.isVideo
    val onCallAction: (CallAction) -> Unit =
        { onEventDispatcher(CallContract.Intent.OnCallAction(it)) }
    val onBack = { onEventDispatcher(CallContract.Intent.OnBack) }

    RingingCallContent(
        call = call,
        isVideoType = isVideo,
        onBackPressed = onBack,
        onCallAction = onCallAction,
        onAcceptedContent = {
            if (isVideo) {
                FullScreenVideoCall(call = call, group = false, uiState = uiState, onEventDispatcher = onEventDispatcher)
            } else {
                val micOn by call.microphone.isEnabled.collectAsState()
                AudioCallContent(
                    call = call,
                    isMicrophoneEnabled = micOn,
                    onCallAction = onCallAction,
                    onBackPressed = onBack,
                    controlsContent = {
                        CallControls(
                            call = call,
                            isVideo = false,
                            onCallAction = onCallAction,
                            modifier = Modifier.align(Alignment.BottomCenter)
                        )
                    }
                )
            }
        },
        onRejectedContent = { LaunchedEffect(Unit) { onEventDispatcher(CallContract.Intent.OnFinished) } },
        onNoAnswerContent = { LaunchedEffect(Unit) { onEventDispatcher(CallContract.Intent.OnFinished) } },
        // Qo'ng'iroqdan chiqilgach holat `Idle` bo'ladi — Stream bu yerda hech narsa chizmaydi (qora ekran).
        // Ekranni yopishni asosan ViewModel qiladi; bu zaxira, signal kechiksa ham foydalanuvchi qolib ketmasin.
        onIdle = { LaunchedEffect(Unit) { onEventDispatcher(CallContract.Intent.OnFinished) } }
    )
}

/**
 * Video qo'ng'iroq butun ekranda (Telegram'dagidek): suhbatdosh kamerasi status bar va navigation bar ostigacha
 * cho'ziladi, mening kameram kichik suzuvchi oynada. Stream'ning `CallContent`i tepa panel va tugmalarni videodan
 * TASHQARIGA (Scaffold top/bottom bar) qo'yadi — video o'rtada qisilib qolardi. Shuning uchun uning slot'lari bo'sh
 * qoldiriladi, ism/davomiylik va tugmalar esa video USTIGA qo'yiladi. Yorug' videoda oq matn/ikonka ko'rinsin
 * deb, ularning ortida yengil qora gradient bor.
 *
 * PiP (Picture-in-Picture): Home yoki "orqaga" bosilsa video kichik oynada davom etadi — buni `CallContent`ning
 * o'zi qiladi (manifest'da `supportsPictureInPicture`). PiP o'chiq bo'lsa Stream fonga o'tganda kamera VA
 * mikrofonni pauza qilardi — suhbatdosh meni eshitmay qolardi. Kichik oynada bizning panellar yashiriladi
 * (bosib bo'lmaydi, joy ham yo'q). Qo'ng'iroq kichik oynada turganda tugasa, oynada chat ochilib qolmasin —
 * ilova fonga o'tkaziladi (PiP oynasi yopiladi).
 */
@Composable
private fun FullScreenVideoCall(
    call: Call,
    group: Boolean,
    uiState: CallContract.UiState,
    onEventDispatcher: (CallContract.Intent) -> Unit
) {
    val onCallAction: (CallAction) -> Unit = { onEventDispatcher(CallContract.Intent.OnCallAction(it)) }
    val activity = LocalActivity.current
    val inPip = rememberIsInPipMode()
    DisposableEffect(Unit) {
        onDispose {
            if (activity != null && activity.isInPictureInPictureMode) {
                activity.moveTaskToBack(false)
            }
        }
    }
    val screenSharing by call.screenShare.isEnabled.collectAsState()
    val screenShare = rememberScreenShareLauncher(
        onPrepare = { onEventDispatcher(CallContract.Intent.OnScreenSharePrepare) },
        onResult = { onEventDispatcher(CallContract.Intent.OnStartScreenShare(it)) }
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // Ekran ulashish ruxsati oynasi ochilganda PiP vaqtincha o'chadi (izoh — rememberScreenShareLauncher'da).
        // Stream PiP sozlamasini birinchi chizilishda eslab qoladi, shuning uchun `key` bilan qayta yaratiladi.
        key(screenShare.pipPaused) {
            CallContent(
                call = call,
                modifier = Modifier.fillMaxSize(),
                onBackPressed = { onEventDispatcher(CallContract.Intent.OnBack) },
                onCallAction = onCallAction,
                appBarContent = {},
                controlsContent = {},
                // Video qismi o'zimizniki: reaksiya/suzuvchi kamera panel ostida, ekran ulashishda kameralar ko'rinadi.
                videoContent = { CallVideoContent(call = it) },
                pictureInPictureConfiguration = PictureInPictureConfiguration(enable = !screenShare.pipPaused)
            )
        }
        if (!inPip) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .background(Brush.verticalGradient(listOf(Scrim, Color.Transparent))),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CallTopBar(call = call, group = group)
                RaisedHandsChip(names = uiState.raisedHands.values)
            }
            CallControls(
                call = call,
                isVideo = true,
                onCallAction = onCallAction,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Scrim))),
                extras = CallExtras(
                    handRaised = uiState.myHandRaised,
                    screenSharing = screenSharing,
                    onReaction = { onEventDispatcher(CallContract.Intent.OnSendReaction(it)) },
                    onToggleHand = { onEventDispatcher(CallContract.Intent.OnToggleHand) },
                    onToggleScreenShare = {
                        if (screenSharing) onEventDispatcher(CallContract.Intent.OnStopScreenShare) else screenShare.launch()
                    },
                    background = uiState.background,
                    onBackground = { onEventDispatcher(CallContract.Intent.OnSelectBackground(it)) }
                )
            )
        }
    }
}

/** Ekran ulashish ruxsatini so'rash holati: [launch] — tizim oynasini ochish, [pipPaused] — shu payt PiP o'chiq. */
private class ScreenShareLauncher(val pipPaused: Boolean, val launch: () -> Unit)

/**
 * Tizimning "ekranni yozib olish" ruxsat oynasi (MediaProjection). Muammo: oyna activity'ni pauza qiladi, Stream esa
 * pauzada PiP'ga o'tadi — oyna o'rniga qo'ng'iroq kichrayib qolardi. Shuning uchun:
 *  1) avval PiP o'chiriladi ([pipPaused] = true) va `CallContent` yangi sozlama bilan qayta chiziladi;
 *  2) shundan KEYIN (LaunchedEffect — yangi sozlama ulangach) oyna ochiladi;
 *  3) natija kelgach biroz kutib PiP qayta yoqiladi — Stream o'chiq PiP'da pauzada to'xtatgan kamera/mikrofonni
 *     `onResume`da qayta yoqib ulgursin.
 */
@Composable
private fun rememberScreenShareLauncher(onPrepare: () -> Unit, onResult: (Intent?) -> Unit): ScreenShareLauncher {
    val context = LocalContext.current
    var pipPaused by remember { mutableStateOf(false) }
    var pendingLaunch by remember { mutableStateOf(false) }
    var resumePip by remember { mutableIntStateOf(0) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val data = result.data
        // Rad etilsa ham chaqiriladi (null) — kamera/mikrofon oynadan oldingi holatiga qaytsin.
        onResult(data.takeIf { result.resultCode == Activity.RESULT_OK })
        resumePip++
    }
    LaunchedEffect(pendingLaunch) {
        if (pendingLaunch) {
            pendingLaunch = false
            val manager = context.getSystemService(MediaProjectionManager::class.java)
            launcher.launch(manager.createScreenCaptureIntent())
        }
    }
    LaunchedEffect(resumePip) {
        if (resumePip > 0) {
            delay(PIP_RESUME_DELAY_MS)
            pipPaused = false
        }
    }
    return ScreenShareLauncher(pipPaused = pipPaused, launch = {
        onPrepare()
        pipPaused = true
        pendingLaunch = true
    })
}

private const val PIP_RESUME_DELAY_MS = 700L

/** Video ustidagi matn/tugmalar ortidagi soya. */
private val Scrim = Color.Black.copy(alpha = 0.55f)

/**
 * Ruxsatlar ekran ochilishi bilan so'raladi — chiquvchi qo'ng'iroqda suhbatdosh qabul qilishi bilan SDK darhol
 * ulanadi, o'shanda ruxsat allaqachon bo'lishi kerak. Audio qo'ng'iroqda faqat mikrofon (kamera so'ralmaydi).
 * Rad etilsa ham qo'ng'iroq davom etadi — faqat tegishli trek o'chiq bo'ladi. [onSettled] — ruxsatlar bor yoki
 * dialog yopildi (natijasidan qat'i nazar).
 */
@Composable
private fun CallPermissions(isVideo: Boolean, onSettled: () -> Unit) {
    val context = LocalContext.current
    val permissions = buildList {
        add(Manifest.permission.RECORD_AUDIO)
        if (isVideo) add(Manifest.permission.CAMERA)
        // Android 12+: bluetooth quloqchinga ovozni yo'naltirish uchun.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) add(Manifest.permission.BLUETOOTH_CONNECT)
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { onSettled() }
    LaunchedEffect(isVideo) {
        val missing = permissions.filter { ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED }
        if (missing.isNotEmpty()) launcher.launch(missing.toTypedArray()) else onSettled()
    }
}
