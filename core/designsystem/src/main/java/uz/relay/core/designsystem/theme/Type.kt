package uz.relay.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import uz.relay.core.designsystem.R

val FigtreeFontFamily = FontFamily(
    Font(R.font.figtree_regular, FontWeight.Normal),
    Font(R.font.figtree_medium, FontWeight.Medium),
    Font(R.font.figtree_semibold, FontWeight.SemiBold),
    Font(R.font.figtree_bold, FontWeight.Bold),
    Font(R.font.figtree_extrabold, FontWeight.ExtraBold),
)

@Immutable
data class SwiftTypography(
    // Screen h1
    val displayTitle: TextStyle,
    // Chat list app bar wordmark
    val appTitle: TextStyle,
    // Chat header, profile name (17–24)
    val title: TextStyle,
    // Message body
    val body: TextStyle,
    // Row title
    val bodyStrong: TextStyle,
    // Last message, subtitle (13–15)
    val supporting: TextStyle,
    // Badges, tabs, chips (12–15)
    val label: TextStyle,
)

private fun figtree(size: Int, weight: FontWeight, lineHeight: Int? = null, letterSpacing: Double = 0.0) = TextStyle(
    fontFamily = FigtreeFontFamily,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight?.sp ?: TextStyle.Default.lineHeight,
    letterSpacing = letterSpacing.sp,
)

val DefaultSwiftTypography = SwiftTypography(
    displayTitle = figtree(28, FontWeight.Bold, lineHeight = 34, letterSpacing = -0.4),
    appTitle = figtree(22, FontWeight.ExtraBold),
    title = figtree(17, FontWeight.Bold),
    body = figtree(15, FontWeight.Normal, lineHeight = 21),
    bodyStrong = figtree(16, FontWeight.SemiBold),
    supporting = figtree(15, FontWeight.Normal),
    label = figtree(13, FontWeight.SemiBold),
)

// Material components fall back to these; every style uses Figtree.
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
