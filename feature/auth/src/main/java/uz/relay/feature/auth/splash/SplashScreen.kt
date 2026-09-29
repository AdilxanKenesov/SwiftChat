package uz.relay.feature.auth.splash

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import org.orbitmvi.orbit.compose.collectAsState
import uz.relay.core.designsystem.component.SwiftLogoTile
import uz.relay.core.designsystem.component.SwiftWordmark
import uz.relay.core.designsystem.theme.SwiftChatTheme
import uz.relay.core.designsystem.theme.SwiftTheme

/**
 * Ilova ochilganda birinchi ko'rinadigan ekran: logo va yuklanish indikatori.
 *
 * O'zi hech narsa ko'rsatmaydi va Intent qabul qilmaydi - ViewModel saqlangan sessiyani o'qib,
 * Phone, ProfileSetup yoki Chats ekraniga `ResetTo` bilan o'tkazadi.
 */
@Composable
internal fun SplashScreen(viewModel: SplashViewModel = hiltViewModel()) {
    // Obuna bo'lish container'ni ishga tushiradi, uning onCreate bloki birinchi ekranni tanlaydi.
    viewModel.collectAsState()
    SplashScreenContent()
}

/** Stateless splash ko'rinishi; Preview'da ViewModel'siz ko'rsatish uchun ajratilgan. */
@Composable
private fun SplashScreenContent() {
    val colors = SwiftTheme.colors

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                // Pastdagi 80dp bo'shliq markazdagi blokni markazdan 40dp yuqoriga ko'taradi (offset ishlatmasdan).
                .padding(bottom = 80.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            SwiftLogoTile(size = 112.dp)
            SwiftWordmark(fontSize = 32.sp, letterSpacing = (-0.8).sp)
        }

        SplashProgress(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 64.dp)
        )
    }
}

/**
 * Pastdagi kichik "borib-keluvchi" progress chizig'i. Canvas'da chizilgan, chunki dizayndagi bu indikator
 * standart Material progress'ga o'xshamaydi va har kadrda faqat draw bosqichi qayta ishlaydi (layout emas).
 */
@Composable
private fun SplashProgress(modifier: Modifier = Modifier) {
    val colors = SwiftTheme.colors
    val transition = rememberInfiniteTransition(label = "splashProgress")
    val fraction by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 700, easing = LinearEasing), RepeatMode.Reverse),
        label = "splashProgressOffset"
    )

    Canvas(modifier = modifier.size(width = 40.dp, height = 4.dp)) {
        val radius = CornerRadius(size.height / 2)
        val indicatorWidth = 16.dp.toPx()

        drawRoundRect(color = colors.surface2, cornerRadius = radius)
        drawRoundRect(
            color = colors.primary,
            topLeft = Offset(x = (size.width - indicatorWidth) * fraction, y = 0f),
            size = Size(indicatorWidth, size.height),
            cornerRadius = radius
        )
    }
}

// Preview'lar: yorug' va qorong'i tema.
@Preview(name = "Light", showSystemUi = true)
@Composable
private fun SplashLightPreview() {
    SwiftChatTheme(darkTheme = false) { SplashScreenContent() }
}

@Preview(name = "Dark", showSystemUi = true)
@Composable
private fun SplashDarkPreview() {
    SwiftChatTheme(darkTheme = true) { SplashScreenContent() }
}
