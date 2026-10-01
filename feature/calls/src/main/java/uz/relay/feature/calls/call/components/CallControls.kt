package uz.relay.feature.calls.call.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.call.state.CallAction
import io.getstream.video.android.core.call.state.FlipCamera
import io.getstream.video.android.core.call.state.LeaveCall
import io.getstream.video.android.core.call.state.ToggleCamera
import io.getstream.video.android.core.call.state.ToggleMicrophone
import io.getstream.video.android.core.call.state.ToggleSpeakerphone
import kotlinx.coroutines.delay
import uz.relay.core.designsystem.R as DesignR
import uz.relay.domain.model.CallLogFormat
import uz.relay.feature.calls.R

/** Qo'ng'iroq ekrani har doim qorong'i (Telegram kabi) — temadan qat'i nazar oq ikonka/matn. */
private val ButtonOn = Color.White.copy(alpha = 0.18f)
private val HangUpRed = Color(0xFFE53935)

/**
 * Faol qo'ng'iroqning pastki boshqaruv paneli. Stream'ning standart `ControlActions`i o'rniga — u ba'zi
 * qurilmalarda ko'rinmay qolgan, qizil "chiqish" tugmasi esa tepadagi panelga tushib qolgan edi. Bu yerda tugmalar
 * aniq joyda: pastda, navigation bar ustida, ostida nomi bilan.
 *
 *  - Video: Kamera · Almashtirish · Mikrofon · Tugatish
 *  - Audio: Mikrofon · Karnay · Tugatish
 *
 * Holat (yoqiq/o'chiq) to'g'ridan-to'g'ri `call.camera/microphone/speaker` StateFlow'laridan — tugma bosilganda
 * Stream o'zgartiradi va bu yerga qaytib keladi. Amallar ViewModel orqali ([onCallAction]) — tugatishda tarix yozilsin.
 */
@Composable
internal fun CallControls(call: Call, isVideo: Boolean, onCallAction: (CallAction) -> Unit, modifier: Modifier = Modifier) {
    val cameraOn by call.camera.isEnabled.collectAsState()
    val micOn by call.microphone.isEnabled.collectAsState()
    val speakerOn by call.speaker.isEnabled.collectAsState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 12.dp, end = 12.dp, top = 16.dp, bottom = 28.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Top
    ) {
        if (isVideo) {
            ControlButton(
                icon = if (cameraOn) DesignR.drawable.ic_video else DesignR.drawable.ic_video_off,
                label = stringResource(R.string.call_camera),
                on = cameraOn,
                onClick = { onCallAction(ToggleCamera(!cameraOn)) }
            )
            ControlButton(
                icon = DesignR.drawable.ic_switch_camera,
                label = stringResource(R.string.call_flip),
                on = true,
                enabled = cameraOn,
                onClick = { onCallAction(FlipCamera) }
            )
        }
        ControlButton(
            icon = if (micOn) DesignR.drawable.ic_mic else DesignR.drawable.ic_mic_off,
            label = stringResource(R.string.call_microphone),
            on = micOn,
            onClick = { onCallAction(ToggleMicrophone(!micOn)) }
        )
        if (!isVideo) {
            ControlButton(
                icon = if (speakerOn) DesignR.drawable.ic_volume else DesignR.drawable.ic_volume_x,
                label = stringResource(R.string.call_speaker),
                on = speakerOn,
                onClick = { onCallAction(ToggleSpeakerphone(!speakerOn)) }
            )
        }
        ControlButton(
            icon = DesignR.drawable.ic_call_end,
            label = stringResource(R.string.call_end),
            on = true,
            size = 64.dp,
            background = HangUpRed,
            onClick = { onCallAction(LeaveCall) }
        )
    }
}

/**
 * Dumaloq tugma + ostida nom. O'chiq holat (mikrofon/kamera o'chirilgan) — Telegram'dagidek teskari: oq fon,
 * qora ikonka — bir qarashda "o'chiq" ekani ko'rinsin.
 */
@Composable
private fun ControlButton(
    @DrawableRes icon: Int,
    label: String,
    on: Boolean,
    onClick: () -> Unit,
    size: Dp = 56.dp,
    background: Color? = null,
    enabled: Boolean = true
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(background ?: if (on) ButtonOn else Color.White)
                .clickable(enabled = enabled, onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = label,
                tint = when {
                    background != null -> Color.White
                    on -> Color.White.copy(alpha = if (enabled) 1f else 0.4f)
                    else -> Color.Black
                },
                modifier = Modifier.size(26.dp)
            )
        }
        Text(text = label, color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
    }
}

/**
 * Tepada: suhbatdosh ismi va qo'ng'iroq davomiyligi ("0:42"). Davomiylik suhbatdosh ulangan paytdan sanaladi
 * (jiringlash kirmaydi); ulanguncha "Ulanmoqda…". Bu yerda "chiqish" tugmasi YO'Q — u faqat pastdagi panelda.
 *
 * Guruh video chatida ([group]): ism o'rniga "Video chat", ostida ishtirokchilar soni va xonada o'tirgan vaqtim
 * (xonaga kirishim bilan sanaladi — kimdir ulanishini kutish yo'q).
 */
@Composable
internal fun CallTopBar(call: Call, modifier: Modifier = Modifier, group: Boolean = false) {
    val members by call.state.members.collectAsState()
    val remotes by call.state.remoteParticipants.collectAsState()
    // 1:1 qo'ng'iroq: a'zolardan men bo'lmagani — suhbatdosh (a'zolar ro'yxati join'dan oldin ham bor).
    val myId = remember { StreamVideo.instanceOrNull()?.userId }
    val name = if (group) stringResource(R.string.call_group_title) else members.firstOrNull { it.user.id != myId }?.user?.name.orEmpty()

    var startedAt by remember { mutableStateOf(if (group) System.currentTimeMillis() else null) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(remotes.isNotEmpty()) {
        if (remotes.isNotEmpty() && startedAt == null) startedAt = System.currentTimeMillis()
    }
    // Soniyada bir marta — faqat davomiylik matni qayta chiziladi.
    LaunchedEffect(startedAt) {
        while (startedAt != null) {
            now = System.currentTimeMillis()
            delay(1_000)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = 12.dp, bottom = 8.dp, start = 24.dp, end = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(text = name, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        val started = startedAt
        val duration = if (started != null) CallLogFormat.duration((now - started) / 1000) else stringResource(R.string.call_connecting)
        // Guruhda: "3 ishtirokchi · 0:42" (men ham hisobga kiraman).
        val people = remotes.size + 1
        Text(
            text = if (group) pluralStringResource(R.plurals.call_participants, people, people) + " · " + duration else duration,
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 14.sp
        )
    }
}
