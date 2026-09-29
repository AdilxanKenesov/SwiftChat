package uz.relay.core.designsystem.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.relay.core.designsystem.theme.SwiftChatTheme
import uz.relay.core.designsystem.theme.SwiftTheme

/**
 * O'qilmaganlar soni belgisi (to'liq yumaloq). [muted] chat uchun kulrang — ovozsiz chat diqqatni
 * tortmasligi kerak. 99 dan katta son "99+" bo'ladi, aks holda belgi kengayib ketadi.
 */
@Composable
fun CountBadge(count: Int, modifier: Modifier = Modifier, muted: Boolean = false, size: Dp = 22.dp) {
    val colors = SwiftTheme.colors
    Box(
        modifier = modifier
            .heightIn(min = size)
            .widthIn(min = size)
            .background(if (muted) colors.muted else colors.primary, CircleShape)
            .padding(horizontal = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (count > 99) "99+" else count.toString(),
            color = if (muted) colors.onMuted else colors.onPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Chatlar sarlavhasi o'rnida ko'rinadigan holat: kichik aylanuvchi halqa + matn
 * ("Ulanmoqda…" yoki "Yangilanmoqda…"). Halqa Canvas'da chiziladi: surface2 rangli to'liq doira
 * va uning ustida aylanuvchi primary yoy (dizayndagidek).
 */
@Composable
fun ConnectionTitle(text: String, modifier: Modifier = Modifier) {
    val colors = SwiftTheme.colors
    val transition = rememberInfiniteTransition(label = "connectionSpinner")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 900, easing = LinearEasing)),
        label = "connectionSpinnerRotation"
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Canvas(modifier = Modifier.size(20.dp)) {
            val strokeWidth = 2.6.dp.toPx()
            val inset = strokeWidth / 2
            val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
            drawArc(
                color = colors.surface2,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(strokeWidth)
            )
            drawArc(
                color = colors.primary,
                startAngle = rotation - 90f,
                sweepAngle = 90f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(strokeWidth, cap = StrokeCap.Round)
            )
        }
        Text(
            text = text,
            color = colors.text2,
            fontSize = 19.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/**
 * Yuklanish paytidagi "soya" qator: 56dp doira + ikki yumaloq chiziq. Kengliklar har qatorda turlicha
 * beriladi — bir xil chiziqlar sun'iy ko'rinadi.
 */
@Composable
fun SkeletonChatRow(titleWidth: Dp, subtitleWidth: Dp, modifier: Modifier = Modifier) {
    val colors = SwiftTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(76.dp)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(colors.skeleton, CircleShape)
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SkeletonBar(width = titleWidth, height = 13.dp)
                SkeletonBar(width = 34.dp, height = 11.dp)
            }
            SkeletonBar(width = subtitleWidth, height = 11.dp)
        }
    }
}

@Composable
private fun SkeletonBar(width: Dp, height: Dp) {
    Box(
        modifier = Modifier
            .size(width = width, height = height)
            .background(SwiftTheme.colors.skeleton, RoundedCornerShape(height / 2))
    )
}

@Preview(name = "Light", showBackground = true)
@Composable
private fun IndicatorsLightPreview() {
    SwiftChatTheme(darkTheme = false) { IndicatorsPreviewContent() }
}

@Preview(name = "Dark", showBackground = true, backgroundColor = 0xFF131218)
@Composable
private fun IndicatorsDarkPreview() {
    SwiftChatTheme(darkTheme = true) { IndicatorsPreviewContent() }
}

@Composable
private fun IndicatorsPreviewContent() {
    Column(
        modifier = Modifier
            .background(SwiftTheme.colors.bg)
            .padding(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ConnectionTitle(text = "Yangilanmoqda…", modifier = Modifier.padding(horizontal = 20.dp))
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CountBadge(count = 2)
            CountBadge(count = 14, muted = true)
            CountBadge(count = 250)
        }
        SkeletonChatRow(titleWidth = 120.dp, subtitleWidth = 200.dp)
    }
}
