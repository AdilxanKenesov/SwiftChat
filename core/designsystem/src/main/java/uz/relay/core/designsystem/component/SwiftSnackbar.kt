package uz.relay.core.designsystem.component

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import uz.relay.core.designsystem.theme.Brand
import uz.relay.core.designsystem.theme.SwiftTheme

/**
 * Ilova bo'yicha yagona Snackbar ko'rinishi va joylashuvi — ekranning TEPASIDA (status bar ostida).
 *
 * Nega tepada: pastda klaviatura, yozish paneli, FAB va navigatsiya paneli turadi — xabar ularning ostida
 * qolib ketardi yoki ularni yopardi. Tepada esa har bir ekranda bir xil joyda, ko'zga darhol tashlanadi.
 *
 * Nega bitta komponent: 12 ta ekranning har biri o'z padding'larini tanlab yurmasin — joylashuv va rang bir
 * joyda o'zgartiriladi. Chaqiruvchi faqat Box ichida `Modifier.align(Alignment.TopCenter)` beradi.
 *
 * Rang temaga teskari (kunduzgi rejimda to'q, tungida och) — har qanday fon ustida ajralib turadi (Material
 * "inverse surface" qoidasi). Tugma ("Qayta urinish") brand rangida, to'q fonda esa ochroq brand.
 */
@Composable
fun SwiftSnackbarHost(hostState: SnackbarHostState, modifier: Modifier = Modifier) {
    val colors = SwiftTheme.colors
    val lightTheme = colors.bg.luminance() > 0.5f
    SnackbarHost(
        hostState = hostState,
        modifier = modifier
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .widthIn(max = 560.dp)
    ) { data ->
        Snackbar(
            snackbarData = data,
            shape = RoundedCornerShape(14.dp),
            containerColor = colors.text,
            contentColor = colors.bg,
            actionColor = if (lightTheme) DarkSurfaceAction else Brand,
            dismissActionContentColor = colors.bg
        )
    }
}

/** To'q Snackbar ustidagi tugma rangi (tungi temaning primary'si) — Brand to'q fonda yaxshi o'qilmaydi. */
private val DarkSurfaceAction = Color(0xFFA59DFF)
