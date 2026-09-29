package uz.relay.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.relay.core.designsystem.theme.AvatarColors
import uz.relay.core.designsystem.theme.SwiftChatTheme
import uz.relay.core.designsystem.theme.SwiftTheme

/**
 * Doira avatar: rangli fon ustida oq bosh harflar.
 *
 * Rang [colorSeed] (odatda userId yoki chatId) dan hisoblanadi. Nega ismdan emas: ism o'zgarsa ham
 * odamning rangi o'zgarmasligi kerak, id esa doimiy. `hashCode` Kotlin'da barqaror — har ishga
 * tushirishda bir xil rang chiqadi.
 *
 * Rasmli avatar (avatarMediaId) media bosqichida qo'shiladi: `/v1/media/{id}` token talab qiladi,
 * buning uchun token qo'shadigan ImageLoader kerak.
 *
 * @param online `true` bo'lsa, pastki o'ng burchakda yashil nuqta (fon rangida 3dp hoshiya bilan).
 */
@Composable
fun Avatar(
    name: String?,
    colorSeed: String,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    online: Boolean = false
) {
    val colors = SwiftTheme.colors
    Box(modifier = modifier.size(size)) {
        Box(
            modifier = Modifier
                .size(size)
                .background(avatarColor(colorSeed), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initialsOf(name),
                color = Color.White,
                // Dizayn: 56dp avatarda 19sp, 34dp da 14sp — o'lchamga mutanosib.
                fontSize = (size.value * 0.34f).sp,
                fontWeight = FontWeight.Bold
            )
        }
        if (online) {
            // Nuqta avatar o'lchamiga mutanosib: 56dp da 17dp (dizayn).
            val dotSize = size * 0.3f
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(dotSize)
                    .background(colors.online, CircleShape)
                    .border(3.dp, colors.bg, CircleShape)
            )
        }
    }
}

/**
 * [seed] bo'yicha [AvatarColors] palitrasidan rang tanlaydi. `floorMod` — manfiy hashCode'da ham indeks
 * 0..size oralig'ida qolishi uchun (oddiy `%` manfiy son qaytarishi mumkin).
 */
fun avatarColor(seed: String): Color = AvatarColors[Math.floorMod(seed.hashCode(), AvatarColors.size)]

/** "Jasur Aliyev" → "JA", "Dilnoza" → "D". Ism yo'q bo'lsa — bo'sh (faqat rangli doira). */
fun initialsOf(name: String?): String = name.orEmpty()
    .trim()
    .split(Regex("\\s+"))
    .filter { it.isNotEmpty() }
    .take(2)
    .joinToString("") { it.first().uppercase() }

// Preview'lar: komponentni ikkala temada Android Studio'da tekshirish uchun.
@Preview(name = "Light", showBackground = true)
@Composable
private fun AvatarLightPreview() {
    SwiftChatTheme(darkTheme = false) { AvatarPreviewContent() }
}

@Preview(name = "Dark", showBackground = true, backgroundColor = 0xFF131218)
@Composable
private fun AvatarDarkPreview() {
    SwiftChatTheme(darkTheme = true) { AvatarPreviewContent() }
}

@Composable
private fun AvatarPreviewContent() {
    Row(
        modifier = Modifier
            .background(SwiftTheme.colors.bg)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar(name = "Jasur Aliyev", colorSeed = "u1", online = true)
        Avatar(name = "Dilnoza", colorSeed = "u2")
        Avatar(name = "Loyiha jamoasi", colorSeed = "c3", size = 34.dp)
    }
}
