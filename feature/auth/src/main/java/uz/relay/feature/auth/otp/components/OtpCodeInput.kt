package uz.relay.feature.auth.otp.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.Flow
import uz.relay.core.designsystem.theme.SwiftTheme
import uz.relay.feature.auth.otp.OtpContract

/**
 * OTP kiritish maydoni: [codeLength] ta katak (eng ko'pi 54x62), ularni bitta yashirin [BasicTextField]
 * boshqaradi (NumberPassword klaviatura).
 *
 * Nega har bir katak uchun alohida TextField emas: bitta maydon bo'lsa fokusni kataklar orasida
 * ko'chirish, o'chirish (backspace) va SMS/klaviaturadan kodni to'liq joylash (paste/autofill) o'z-o'zidan
 * to'g'ri ishlaydi. Kataklar faqat [code] satrini chizadi.
 *
 * [shakeEvents] noto'g'ri kod silkinishini ishga tushiradi; u graphicsLayer'da chiziladi -
 * layout qayta hisoblanmaydi va offset modifier ishlatilmaydi.
 */
@Composable
fun OtpCodeInput(
    code: String,
    codeLength: Int,
    status: OtpContract.Status,
    enabled: Boolean,
    onCodeChange: (String) -> Unit,
    shakeEvents: Flow<Unit>,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val shake = remember { Animatable(0f) }
    val density = LocalDensity.current

    // Ekran ochilishi bilan fokus va klaviatura beriladi - foydalanuvchi darhol yozishi mumkin.
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    LaunchedEffect(shakeEvents) {
        shakeEvents.collect {
            shake.animateTo(
                targetValue = 0f,
                // Chapga-o'ngga so'nib boruvchi tebranish, oxirida 0 ga qaytadi.
                animationSpec = keyframes {
                    durationMillis = 400
                    -12f at 50
                    12f at 125
                    -8f at 200
                    8f at 275
                    -4f at 350
                }
            )
        }
    }

    BasicTextField(
        value = code,
        onValueChange = onCodeChange,
        // readOnly (disabled emas) kod tekshirilayotganda fokus va klaviaturani saqlab qoladi.
        readOnly = !enabled,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        interactionSource = interactionSource,
        modifier = modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .graphicsLayer { translationX = with(density) { shake.value.dp.toPx() } },
        decorationBox = { innerTextField ->
            Box {
                // Haqiqiy matn maydoni ko'rinmaydi; raqamlarni kataklar chizadi.
                Box(modifier = Modifier.alpha(0f)) { innerTextField() }
                // Har bir katak kenglikning o'z ulushini oladi, ko'pi bilan 54dp: 5 ta katak dizayn o'lchamida qoladi,
                // 6 ta katak 360dp ekranga sig'ishi uchun kichrayadi.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                ) {
                    repeat(codeLength) { index ->
                        CodeBox(
                            modifier = Modifier.weight(1f, fill = false),
                            digit = code.getOrNull(index)?.toString().orEmpty(),
                            active = enabled && focused && index == code.length,
                            status = status
                        )
                    }
                }
            }
        }
    )
}

/** Bitta raqam katagi; ko'rinishi holatga qarab tanlanadi (xato ustuvor, keyin faol, to'ldirilgan, bo'sh). */
@Composable
private fun CodeBox(digit: String, active: Boolean, status: OtpContract.Status, modifier: Modifier = Modifier) {
    val colors = SwiftTheme.colors
    val filled = digit.isNotEmpty()

    val style = when {
        status is OtpContract.Status.Wrong -> BoxStyle(2.dp, colors.error, colors.errorContainer, colors.error)
        status == OtpContract.Status.Expired -> BoxStyle(1.5.dp, colors.surface, colors.surface, colors.text2)
        status == OtpContract.Status.Locked -> BoxStyle(1.5.dp, colors.outline, colors.bg, colors.text, alpha = 0.4f)
        active -> BoxStyle(2.dp, colors.primary, colors.bg, colors.text)
        filled -> BoxStyle(1.5.dp, colors.surface, colors.surface, colors.text)
        else -> BoxStyle(1.5.dp, colors.outline, colors.bg, colors.text)
    }
    val shape = RoundedCornerShape(16.dp)

    Box(
        modifier = modifier
            .size(width = 54.dp, height = 62.dp)
            .alpha(style.alpha)
            .background(style.background, shape)
            .border(style.borderWidth, style.border, shape),
        contentAlignment = Alignment.Center
    ) {
        if (filled) {
            Text(text = digit, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = style.text)
        } else if (active) {
            BlinkingCaret(color = colors.primary)
        }
    }
}

/** Faol bo'sh katakdagi miltillovchi kursor (asl kursor yashirin maydonda qolgani uchun alohida chiziladi). */
@Composable
private fun BlinkingCaret(color: Color) {
    val transition = rememberInfiniteTransition(label = "caret")
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 500), RepeatMode.Reverse),
        label = "caretAlpha"
    )
    Box(
        modifier = Modifier
            .size(width = 2.dp, height = 28.dp)
            .alpha(alpha)
            .background(color, RoundedCornerShape(1.dp))
    )
}

/** Katak ko'rinishi parametrlari - `when` ichida o'qilishi oson bo'lishi uchun bitta obyektga yig'ilgan. */
private data class BoxStyle(
    val borderWidth: Dp,
    val border: Color,
    val background: Color,
    val text: Color,
    val alpha: Float = 1f
)
