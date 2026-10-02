package uz.relay.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Joriy tema ranglari va shriftlari CompositionLocal orqali daraxt bo'ylab uzatiladi — har bir composable'ga
 * parametr sifatida berish shart emas. `static...` — tema kam o'zgaradi, o'zgarganda butun daraxt qayta
 * chiziladi; bu har bir o'qishni kuzatishdan arzonroq.
 */
val LocalSwiftColors = staticCompositionLocalOf { LightSwift }
val LocalSwiftTypography = staticCompositionLocalOf { DefaultSwiftTypography }

/**
 * Ranglar va shriftlarga qisqa kirish: `SwiftTheme.colors.primary`, `SwiftTheme.typography.title`.
 * MaterialTheme'ga o'xshash API, lekin ilovaning o'z tokenlari bilan.
 */
object SwiftTheme {
    val colors: SwiftColors
        @Composable @ReadOnlyComposable get() = LocalSwiftColors.current

    val typography: SwiftTypography
        @Composable @ReadOnlyComposable get() = LocalSwiftTypography.current
}

/**
 * Ilovaning ildiz temasi: o'z tokenlarimizni ([LocalSwiftColors], [LocalSwiftTypography]) va ularga moslangan
 * MaterialTheme'ni birga beradi. MaterialTheme ham kerak, chunki Material komponentlar (TextField, Dialog,
 * Button...) o'z ranglarini colorScheme'dan oladi — ular ham dizayn ranglarida ko'rinsin.
 *
 * [darkTheme] MainActivity'dan ilova sozlamasiga qarab beriladi (tizim temasiga emas); standart qiymat
 * faqat preview'lar uchun.
 */
@Composable
fun SwiftChatTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkSwift else LightSwift

    CompositionLocalProvider(
        LocalSwiftColors provides colors,
        LocalSwiftTypography provides DefaultSwiftTypography,
    ) {
        MaterialTheme(
            colorScheme = colors.toColorScheme(darkTheme),
            typography = SwiftMaterialTypography,
            content = content,
        )
    }
}

/** Bizning tokenlarni Material colorScheme'ga moslaydi; qolgan ranglar standart light/dark sxemadan olinadi. */
private fun SwiftColors.toColorScheme(darkTheme: Boolean): ColorScheme {
    val base = if (darkTheme) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        background = bg,
        onBackground = text,
        surface = bg,
        onSurface = text,
        surfaceVariant = surface,
        onSurfaceVariant = text2,
        error = error,
        errorContainer = errorContainer,
        outline = outline,
        outlineVariant = line,
        scrim = scrim,
    )
}
