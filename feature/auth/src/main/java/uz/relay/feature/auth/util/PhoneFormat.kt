package uz.relay.feature.auth.util

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

const val UZ_PREFIX = "+998"
const val UZ_PHONE_DIGITS = 9

/** "901234567" -> "90 123 45 67" */
fun formatLocalPhone(digits: String): String = buildString {
    digits.forEachIndexed { index, c ->
        if (index == 2 || index == 5 || index == 7) append(' ')
        append(c)
    }
}

/** "+998901234567" -> "+998 90 123 45 67" */
fun formatFullPhone(phone: String): String =
    if (phone.startsWith(UZ_PREFIX)) "$UZ_PREFIX ${formatLocalPhone(phone.removePrefix(UZ_PREFIX))}" else phone

/** Shows the 9 local digits as "90 123 45 67" while the state keeps digits only. */
object LocalPhoneTransformation : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val formatted = formatLocalPhone(text.text)
        return TransformedText(AnnotatedString(formatted), object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int = offset + spacesBefore(offset)

            override fun transformedToOriginal(offset: Int): Int {
                var original = 0
                while (original < text.length && originalToTransformed(original + 1) <= offset) original++
                return original
            }
        })
    }

    // Spaces are inserted before original indices 2, 5 and 7.
    private fun spacesBefore(offset: Int): Int = listOf(2, 5, 7).count { offset > it }
}
