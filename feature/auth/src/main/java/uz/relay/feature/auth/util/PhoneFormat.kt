package uz.relay.feature.auth.util

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

// Hozircha faqat O'zbekiston raqamlari qo'llab-quvvatlanadi: +998 va 9 ta mahalliy raqam.
const val UZ_PREFIX = "+998"
const val UZ_PHONE_DIGITS = 9

/** "901234567" -> "90 123 45 67" */
fun formatLocalPhone(digits: String): String = buildString {
    digits.forEachIndexed { index, c ->
        if (index == 2 || index == 5 || index == 7) append(' ')
        append(c)
    }
}

/** "+998901234567" -> "+998 90 123 45 67"; boshqa formatdagi raqam o'zgarishsiz qaytadi. */
fun formatFullPhone(phone: String): String =
    if (phone.startsWith(UZ_PREFIX)) "$UZ_PREFIX ${formatLocalPhone(phone.removePrefix(UZ_PREFIX))}" else phone

/**
 * 9 ta mahalliy raqamni "90 123 45 67" ko'rinishida ko'rsatadi, state esa faqat raqamlarni saqlaydi.
 *
 * VisualTransformation ishlatilgan, chunki bo'shliqlarni state'ga yozsak, validatsiya va serverga
 * yuborishda ularni har safar tozalash kerak bo'lardi. [OffsetMapping] kursor bo'shliqlar ustidan
 * to'g'ri sakrashi uchun kerak.
 */
object LocalPhoneTransformation : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val formatted = formatLocalPhone(text.text)
        return TransformedText(AnnotatedString(formatted), object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int = offset + spacesBefore(offset)

            // Teskari moslik: formatlangan pozitsiyaga to'g'ri keladigan eng katta original indeksni topadi.
            override fun transformedToOriginal(offset: Int): Int {
                var original = 0
                while (original < text.length && originalToTransformed(original + 1) <= offset) original++
                return original
            }
        })
    }

    // Bo'shliqlar original 2, 5 va 7-indekslar oldidan qo'yiladi.
    private fun spacesBefore(offset: Int): Int = listOf(2, 5, 7).count { offset > it }
}
