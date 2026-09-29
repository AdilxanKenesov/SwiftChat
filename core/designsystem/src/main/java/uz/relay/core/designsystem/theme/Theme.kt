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

val LocalSwiftColors = staticCompositionLocalOf { LightSwift }
val LocalSwiftTypography = staticCompositionLocalOf { DefaultSwiftTypography }

object SwiftTheme {
    val colors: SwiftColors
        @Composable @ReadOnlyComposable get() = LocalSwiftColors.current

    val typography: SwiftTypography
        @Composable @ReadOnlyComposable get() = LocalSwiftTypography.current
}

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
