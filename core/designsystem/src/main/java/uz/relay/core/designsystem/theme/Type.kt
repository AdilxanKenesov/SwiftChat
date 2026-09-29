package uz.relay.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import uz.relay.core.designsystem.R

/**
 * Figtree shrifti — dizayn spec'idagi shrift: zamonaviy, geometrik va kichik o'lchamda ham o'qilishi oson. Fayllar `res/font` da — ilovaga qo'shilgan, shuning uchun tarmoqsiz ham ishlaydi
 * (downloadable fonts'ga bog'liq emas).
 */
val FigtreeFontFamily = FontFamily(
    Font(R.font.figtree_regular, FontWeight.Normal),
    Font(R.font.figtree_medium, FontWeight.Medium),
    Font(R.font.figtree_semibold, FontWeight.SemiBold),
    Font(R.font.figtree_bold, FontWeight.Bold),
    Font(R.font.figtree_extrabold, FontWeight.ExtraBold),
)

/** Ilovaning matn uslublari (dizayn spec'idagi shkala). [LocalSwiftTypography] orqali beriladi. */
@Immutable
data class SwiftTypography(
    // Ekran sarlavhasi (h1)
    val displayTitle: TextStyle,
    // Chatlar ro'yxati app bar'idagi wordmark
    val appTitle: TextStyle,
    // Chat sarlavhasi, profil ismi (17–24)
    val title: TextStyle,
    // Xabar matni
    val body: TextStyle,
    // Qator sarlavhasi
    val bodyStrong: TextStyle,
    // Oxirgi xabar, izoh qatori (13–15)
    val supporting: TextStyle,
    // Belgilar (badge), tab'lar, chip'lar (12–15)
    val label: TextStyle,
)

/** Figtree uslubini qisqa yaratish uchun yordamchi; lineHeight berilmasa — standart. */
private fun figtree(size: Int, weight: FontWeight, lineHeight: Int? = null, letterSpacing: Double = 0.0) = TextStyle(
    fontFamily = FigtreeFontFamily,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight?.sp ?: TextStyle.Default.lineHeight,
    letterSpacing = letterSpacing.sp,
)

/** Standart matn shkalasi (hamma temada bir xil). */
val DefaultSwiftTypography = SwiftTypography(
    displayTitle = figtree(28, FontWeight.Bold, lineHeight = 34, letterSpacing = -0.4),
    appTitle = figtree(22, FontWeight.ExtraBold),
    title = figtree(17, FontWeight.Bold),
    body = figtree(15, FontWeight.Normal, lineHeight = 21),
    bodyStrong = figtree(16, FontWeight.SemiBold),
    supporting = figtree(15, FontWeight.Normal),
    label = figtree(13, FontWeight.SemiBold),
)

// Material komponentlar (masalan, Dialog, TextButton) shu uslublarga tayanadi — hammasida Figtree bo'lsin.
internal val SwiftMaterialTypography = Typography().run {
    Typography(
        displayLarge = displayLarge.copy(fontFamily = FigtreeFontFamily),
        displayMedium = displayMedium.copy(fontFamily = FigtreeFontFamily),
        displaySmall = displaySmall.copy(fontFamily = FigtreeFontFamily),
        headlineLarge = headlineLarge.copy(fontFamily = FigtreeFontFamily),
        headlineMedium = headlineMedium.copy(fontFamily = FigtreeFontFamily),
        headlineSmall = headlineSmall.copy(fontFamily = FigtreeFontFamily),
        titleLarge = titleLarge.copy(fontFamily = FigtreeFontFamily),
        titleMedium = titleMedium.copy(fontFamily = FigtreeFontFamily),
        titleSmall = titleSmall.copy(fontFamily = FigtreeFontFamily),
        bodyLarge = bodyLarge.copy(fontFamily = FigtreeFontFamily),
        bodyMedium = bodyMedium.copy(fontFamily = FigtreeFontFamily),
        bodySmall = bodySmall.copy(fontFamily = FigtreeFontFamily),
        labelLarge = labelLarge.copy(fontFamily = FigtreeFontFamily),
        labelMedium = labelMedium.copy(fontFamily = FigtreeFontFamily),
        labelSmall = labelSmall.copy(fontFamily = FigtreeFontFamily),
    )
}
