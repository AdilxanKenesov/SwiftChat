package uz.relay.core.designsystem.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * Gesture chegarasidan o'tilganda bir marta yengil titrash (Telegram'da xabarni surib reply qilganda shunday).
 * Foydalanuvchi qo'yib yuborishdan oldin "amal bajariladi" ekanini his qiladi. Telefon sozlamalarida titrash
 * o'chirilgan bo'lsa tizim o'zi hech narsa qilmaydi.
 */
@Composable
fun rememberGestureThresholdHaptic(): () -> Unit {
    val haptic = LocalHapticFeedback.current
    return remember(haptic) { { haptic.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate) } }
}
