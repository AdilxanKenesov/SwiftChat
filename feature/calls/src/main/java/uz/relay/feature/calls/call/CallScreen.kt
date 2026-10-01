package uz.relay.feature.calls.call

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.call.activecall.AudioCallContent
import io.getstream.video.android.compose.ui.components.call.activecall.CallContent
import io.getstream.video.android.compose.ui.components.call.ringing.RingingCallContent
import io.getstream.video.android.core.Call
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import uz.relay.core.designsystem.component.SwiftSnackbarHost
import uz.relay.feature.calls.R
import uz.relay.feature.calls.call.components.CallControls
import uz.relay.feature.calls.call.components.CallTopBar

/**
 * Qo'ng'iroq ekrani. Stream'ning tayyor Compose komponentlari ishlatiladi (`VideoTheme` ichida bo'lishi shart):
 *  - [RingingCallContent] jiringlash holatiga qarab kiruvchi / chiquvchi ko'rinishni o'zi tanlaydi;
 *  - qabul qilingach — video uchun [CallContent], audio uchun [AudioCallContent].
 * Tugmalar amallari ViewModel'ga boradi ([CallContract.Intent.OnCallAction]).
 *
 * Ochiladi: shaxsiy chat sarlavhasidagi 📞/🎥 (chiquvchi) yoki ilova darajasidagi kiruvchi qo'ng'iroq (MainViewModel).
 */
@Composable
internal fun CallScreen(callId: String, video: Boolean?, chatId: String?) {
    val viewModel = hiltViewModel<CallViewModel, CallViewModel.Factory>(
        creationCallback = { factory -> factory.create(callId, video, chatId) }
    )
    val uiState by viewModel.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    viewModel.collectSideEffect { sideEffect ->
        when (sideEffect) {
            is CallContract.SideEffect.ShowError -> snackbarHostState.showSnackbar(context.getString(R.string.call_failed))
            CallContract.SideEffect.NoAnswer -> snackbarHostState.showSnackbar(context.getString(R.string.call_no_answer))
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
            CallPermissions(isVideo = uiState.isVideo)
            VideoTheme {
                CallContentHost(call = call, isVideo = uiState.isVideo, onEventDispatcher = viewModel::onEventDispatcher)
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
private fun CallContentHost(call: Call, isVideo: Boolean, onEventDispatcher: (CallContract.Intent) -> Unit) {
    val onCallAction: (io.getstream.video.android.core.call.state.CallAction) -> Unit =
        { onEventDispatcher(CallContract.Intent.OnCallAction(it)) }
    val onBack = { onEventDispatcher(CallContract.Intent.OnBack) }

    RingingCallContent(
        call = call,
        isVideoType = isVideo,
        onBackPressed = onBack,
        onCallAction = onCallAction,
        onAcceptedContent = {
            if (isVideo) {
                // Tepada — ism va davomiylik (chiqish tugmasisiz), pastda — o'zimizning boshqaruv paneli.
                CallContent(
                    call = call,
                    onBackPressed = onBack,
                    onCallAction = onCallAction,
                    appBarContent = { CallTopBar(call = it) },
                    controlsContent = { CallControls(call = it, isVideo = true, onCallAction = onCallAction) }
                )
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
        onNoAnswerContent = { LaunchedEffect(Unit) { onEventDispatcher(CallContract.Intent.OnFinished) } }
    )
}

/**
 * Ruxsatlar ekran ochilishi bilan so'raladi — chiquvchi qo'ng'iroqda suhbatdosh qabul qilishi bilan SDK darhol
 * ulanadi, o'shanda ruxsat allaqachon bo'lishi kerak. Audio qo'ng'iroqda faqat mikrofon (kamera so'ralmaydi).
 * Rad etilsa ham qo'ng'iroq davom etadi — faqat tegishli trek o'chiq bo'ladi.
 */
@Composable
private fun CallPermissions(isVideo: Boolean) {
    val context = LocalContext.current
    val permissions = buildList {
        add(Manifest.permission.RECORD_AUDIO)
        if (isVideo) add(Manifest.permission.CAMERA)
        // Android 12+: bluetooth quloqchinga ovozni yo'naltirish uchun.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) add(Manifest.permission.BLUETOOTH_CONNECT)
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }
    LaunchedEffect(isVideo) {
        val missing = permissions.filter { ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED }
        if (missing.isNotEmpty()) launcher.launch(missing.toTypedArray())
    }
}
