package uz.relay.feature.calls.call.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.layout.ContentScale
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
import uz.relay.feature.calls.call.CallBackground
import uz.relay.feature.calls.call.imageResOrNull

/** Qo'ng'iroq ekrani har doim qorong'i (Telegram kabi) — temadan qat'i nazar oq ikonka/matn. */
private val ButtonOn = Color.White.copy(alpha = 0.18f)
private val HangUpRed = Color(0xFFE53935)
/** "Ko'proq" paneli va qo'l chip'i foni — video ustida ham o'qiladigan yarim shaffof qora. */
private val PanelColor = Color(0xCC1C1C1E)

/**
 * Faol qo'ng'iroqning pastki boshqaruv paneli. Stream'ning standart `ControlActions`i o'rniga — u ba'zi
 * qurilmalarda ko'rinmay qolgan, qizil "chiqish" tugmasi esa tepadagi panelga tushib qolgan edi. Bu yerda tugmalar
 * aniq joyda: pastda, navigation bar ustida, ostida nomi bilan.
 *
 *  - Video: Kamera · Almashtirish · Mikrofon · Ko'proq · Tugatish ("Ko'proq" — [extras] berilganda)
 *  - Audio: Mikrofon · Karnay · Tugatish
 *
 * Holat (yoqiq/o'chiq) to'g'ridan-to'g'ri `call.camera/microphone/speaker` StateFlow'laridan — tugma bosilganda
 * Stream o'zgartiradi va bu yerga qaytib keladi. Amallar ViewModel orqali ([onCallAction]) — tugatishda tarix yozilsin.
 */
/**
 * Video qo'ng'iroqdagi qo'shimcha amallar ("Ko'proq" paneli): reaksiyalar, qo'l ko'tarish, ekranni ulashish.
 * Holat ViewModel'da (qo'l) yoki Stream'da (ekran ulashish) — bu yerga faqat qiymat va callback'lar keladi.
 */
internal class CallExtras(
    val handRaised: Boolean,
    val screenSharing: Boolean,
    val onReaction: (String) -> Unit,
    val onToggleHand: () -> Unit,
    val onToggleScreenShare: () -> Unit,
    val background: CallBackground,
    val onBackground: (CallBackground) -> Unit
)

@Composable
internal fun CallControls(
    call: Call,
    isVideo: Boolean,
    onCallAction: (CallAction) -> Unit,
    modifier: Modifier = Modifier,
    extras: CallExtras? = null
) {
    val cameraOn by call.camera.isEnabled.collectAsState()
    val micOn by call.microphone.isEnabled.collectAsState()
    val speakerOn by call.speaker.isEnabled.collectAsState()
    // Panel ochiqligi — faqat UI holati (ekran burilsa yopilsa ham zarari yo'q).
    var moreOpen by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 12.dp, end = 12.dp, top = 16.dp, bottom = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (extras != null) {
            AnimatedVisibility(visible = moreOpen) {
                MorePanel(extras = extras, onDone = { moreOpen = false })
            }
        }
        // Har bir tugma teng kenglikda (weight) — 5 ta tugma tor ekranda ham sig'sin, nomlar ustma-ust tushmasin.
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            val cell = Modifier.weight(1f)
            if (isVideo) {
                ControlButton(
                    icon = if (cameraOn) DesignR.drawable.ic_video else DesignR.drawable.ic_video_off,
                    label = stringResource(R.string.call_camera),
                    on = cameraOn,
                    onClick = { onCallAction(ToggleCamera(!cameraOn)) },
                    modifier = cell
                )
                ControlButton(
                    icon = DesignR.drawable.ic_switch_camera,
                    label = stringResource(R.string.call_flip),
                    on = true,
                    enabled = cameraOn,
                    onClick = { onCallAction(FlipCamera) },
                    modifier = cell
                )
            }
            ControlButton(
                icon = if (micOn) DesignR.drawable.ic_mic else DesignR.drawable.ic_mic_off,
                label = stringResource(R.string.call_microphone),
                on = micOn,
                onClick = { onCallAction(ToggleMicrophone(!micOn)) },
                modifier = cell
            )
            if (!isVideo) {
                ControlButton(
                    icon = if (speakerOn) DesignR.drawable.ic_volume else DesignR.drawable.ic_volume_x,
                    label = stringResource(R.string.call_speaker),
                    on = speakerOn,
                    onClick = { onCallAction(ToggleSpeakerphone(!speakerOn)) },
                    modifier = cell
                )
            }
            if (extras != null) {
                ControlButton(
                    icon = DesignR.drawable.ic_more_horizontal,
                    label = stringResource(R.string.call_more),
                    // Ochiq panel — teskari rang (tugma "bosilgan" ko'rinsin).
                    on = !moreOpen,
                    onClick = { moreOpen = !moreOpen },
                    modifier = cell
                )
            }
            ControlButton(
                icon = DesignR.drawable.ic_call_end,
                label = stringResource(R.string.call_end),
                on = true,
                size = 64.dp,
                background = HangUpRed,
                onClick = { onCallAction(LeaveCall) },
                modifier = cell
            )
        }
    }
}

/** Reaksiyalar (emoji teginilsa — yuboriladi va panel yopiladi). */
private val Reactions = listOf("👍", "❤️", "😂", "😮", "👏", "🎉")

/**
 * "Ko'proq" paneli: tepada emoji qatori, ostida ikki pill tugma — qo'l ko'tarish va ekranni ulashish.
 * Yoqiq holatdagi tugma oq fonda (boshqa tugmalardagi "teskari rang" qoidasi kabi).
 */
@Composable
private fun MorePanel(extras: CallExtras, onDone: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(PanelColor)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Reactions.forEach { emoji ->
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(ButtonOn)
                        .clickable {
                            extras.onReaction(emoji)
                            onDone()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = emoji, fontSize = 22.sp)
                }
            }
        }
        BackgroundRow(selected = extras.background, onSelect = extras.onBackground)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PillButton(
                icon = DesignR.drawable.ic_hand,
                label = stringResource(if (extras.handRaised) R.string.call_lower_hand else R.string.call_raise_hand),
                active = extras.handRaised,
                onClick = {
                    extras.onToggleHand()
                    onDone()
                },
                modifier = Modifier.weight(1f)
            )
            PillButton(
                icon = DesignR.drawable.ic_screen_share,
                label = stringResource(if (extras.screenSharing) R.string.call_stop_share else R.string.call_share_screen),
                active = extras.screenSharing,
                onClick = {
                    extras.onToggleScreenShare()
                    onDone()
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * "Orqa fon" qatori: Yo'q · Xira · tayyor rasmlar. Tanlangani oq halqa bilan ajratiladi. Panel yopilmaydi —
 * natijani o'z videoingizda darhol ko'rib, boshqasini sinab ko'rish qulay.
 */
@Composable
private fun BackgroundRow(selected: CallBackground, onSelect: (CallBackground) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = stringResource(R.string.call_background), color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CallBackground.entries.forEach { background ->
                val isSelected = background == selected
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .border(width = if (isSelected) 2.dp else 0.dp, color = if (isSelected) Color.White else Color.Transparent, shape = CircleShape)
                        .padding(if (isSelected) 3.dp else 0.dp)
                        .clip(CircleShape)
                        .background(ButtonOn)
                        .clickable { onSelect(background) },
                    contentAlignment = Alignment.Center
                ) {
                    val image = background.imageResOrNull()
                    when {
                        image != null -> Image(
                            painter = painterResource(image),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        else -> Text(
                            text = stringResource(if (background == CallBackground.BLUR) R.string.call_bg_blur else R.string.call_bg_none),
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PillButton(@DrawableRes icon: Int, label: String, active: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val content = if (active) Color.Black else Color.White
    Row(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(if (active) Color.White else ButtonOn)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(painter = painterResource(icon), contentDescription = null, tint = content, modifier = Modifier.size(18.dp))
        Text(text = label, color = content, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/**
 * Qo'l ko'targanlar — tepadagi sarlavha ostida: "✋ Ali, Malika". Bo'sh bo'lsa hech narsa chizilmaydi.
 */
@Composable
internal fun RaisedHandsChip(names: Collection<String>, modifier: Modifier = Modifier) {
    if (names.isEmpty()) return
    Text(
        text = "✋ " + names.joinToString(", "),
        color = Color.White,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(PanelColor)
            .padding(horizontal = 14.dp, vertical = 7.dp)
    )
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
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
        Text(text = label, color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
