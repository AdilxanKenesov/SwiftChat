package uz.relay.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/** Brend rangi — ikkala temada bir xil: logo plitkasi, brend ikonka plitkalari va FAB soyasi uchun. */
val Brand = Color(0xFF5B4FE9)

/**
 * Ilovaning rang tokenlari (dizayn spec'idagi nomlar bilan: bg, surface, bubbleIn, tickRead...).
 *
 * Nega MaterialTheme.colorScheme yetarli emas: messenger'ga Material'da yo'q ranglar kerak (xabar pufakchalari,
 * ✓✓ belgisi, reply, skeleton, chat foni). Shuning uchun o'z tokenlarimiz [LocalSwiftColors] orqali beriladi.
 * `@Immutable` — Compose bu obyekt o'zgarmasligini bilsin va keraksiz recomposition bo'lmasin.
 */
@Immutable
data class SwiftColors(
    val bg: Color, val surface: Color, val surface2: Color,
    val page: Color, val card: Color, val cardBorder: Color,
    val text: Color, val text2: Color, val line: Color, val outline: Color,
    val primary: Color, val onPrimary: Color, val primaryContainer: Color, val onPrimaryContainer: Color,
    val wall: Color, val bubbleIn: Color, val bubbleOut: Color, val onBubbleOut: Color,
    val meta: Color, val metaOut: Color, val tickRead: Color,
    val replyIn: Color, val replyOut: Color, val replyOutAccent: Color,
    val chip: Color, val onChip: Color,
    val online: Color, val error: Color, val errorContainer: Color,
    val scrim: Color, val skeleton: Color, val muted: Color, val onMuted: Color, val menu: Color,
)

/** Kunduzgi tema ranglari. */
val LightSwift = SwiftColors(
    bg = Color(0xFFFFFFFF), surface = Color(0xFFF4F3F8), surface2 = Color(0xFFEAE8F1),
    page = Color(0xFFF4F3F8), card = Color(0xFFFFFFFF), cardBorder = Color.Transparent,
    text = Color(0xFF17161C), text2 = Color(0xFF67647A), line = Color(0xFFE8E6EF), outline = Color(0xFFC7C4D4),
    primary = Color(0xFF5B4FE9), onPrimary = Color(0xFFFFFFFF), primaryContainer = Color(0xFFE8E5FF), onPrimaryContainer = Color(0xFF2A1F9E),
    wall = Color(0xFFECEAF3), bubbleIn = Color(0xFFFFFFFF), bubbleOut = Color(0xFFDCD7FF), onBubbleOut = Color(0xFF17161C),
    meta = Color(0xFF827F93), metaOut = Color(0xFF6F6A98), tickRead = Color(0xFF4B3FE0),
    replyIn = Color(0xFFF2F0F9), replyOut = Color(0x1A4B3FE0), replyOutAccent = Color(0xFF4B3FE0),
    chip = Color(0x1A262240), onChip = Color(0xFF46435A),
    online = Color(0xFF1E9E63), error = Color(0xFFC8322F), errorContainer = Color(0xFFFDECEB),
    scrim = Color(0x66141220), skeleton = Color(0xFFEDEBF3), muted = Color(0xFFE3E1EA), onMuted = Color(0xFF5A576B), menu = Color(0xFFFFFFFF),
)

/** Tungi tema ranglari. */
val DarkSwift = SwiftColors(
    bg = Color(0xFF131218), surface = Color(0xFF1C1B23), surface2 = Color(0xFF282731),
    page = Color(0xFF0E0D12), card = Color(0xFF1C1B23), cardBorder = Color(0xFF26252F),
    text = Color(0xFFF3F2F7), text2 = Color(0xFFA19EB0), line = Color(0xFF2A2932), outline = Color(0xFF4A4856),
    primary = Color(0xFFA59DFF), onPrimary = Color(0xFF1B1454), primaryContainer = Color(0xFF352F7E), onPrimaryContainer = Color(0xFFE4E0FF),
    wall = Color(0xFF0C0B10), bubbleIn = Color(0xFF1F1E27), bubbleOut = Color(0xFF4338CA), onBubbleOut = Color(0xFFFFFFFF),
    meta = Color(0xFF8F8C9E), metaOut = Color(0x9EFFFFFF), tickRead = Color(0xFF8FF0CB),
    replyIn = Color(0xFF2A2933), replyOut = Color(0x1FFFFFFF), replyOutAccent = Color(0xFFE0DCFF),
    chip = Color(0x14FFFFFF), onChip = Color(0xFFB9B6C6),
    online = Color(0xFF3CCB86), error = Color(0xFFFF6B6B), errorContainer = Color(0xFF3A1D21),
    scrim = Color(0x99000000), skeleton = Color(0xFF23222B), muted = Color(0xFF33313C), onMuted = Color(0xFFB9B6C6), menu = Color(0xFF2A2933),
)

/** Avatar palitrasi (ustida oq bosh harflar). userId/chatId'ning barqaror hash'i bo'yicha tanlanadi. */
val AvatarColors = listOf(
    Color(0xFF6655E0), Color(0xFF0E8577), Color(0xFFC8553D), Color(0xFFA86A12),
    Color(0xFFB8386A), Color(0xFF2F6BD6), Color(0xFF3D8A4F),
)

/** Guruhda yuboruvchi ismi ranglari (ikkala pufakcha rangida ham o'qiladigan). */
val SenderNameColors = listOf(Color(0xFF1A9A8A), Color(0xFFD0573A), Color(0xFF3D7BE0), Color(0xFFC07A12))

/** Sozlamalar qatorlaridagi ikonka plitkalari ranglari (temaga bog'liq emas). */
object SettingsTileColors {
    val Phone = Color(0xFF2F6BD6)
    val Username = Color(0xFF3D8A4F)
    val Notifications = Color(0xFFE0603A)
    val DarkMode = Color(0xFF6655E0)
    val Language = Color(0xFF0E8577)
}
