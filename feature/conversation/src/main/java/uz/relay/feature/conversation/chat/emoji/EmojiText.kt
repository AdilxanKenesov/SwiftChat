package uz.relay.feature.conversation.chat.emoji

import android.icu.text.BreakIterator

/** Shundan ko'p emoji bo'lsa xabar oddiy bubble'da chiziladi (Telegram ham 1–3 tagacha kattalashtiradi). */
internal const val MAX_LARGE_EMOJI = 3

/**
 * Xabar faqat emojidan iboratmi va nechta: 1..[MAX_LARGE_EMOJI] bo'lsa son, aks holda `null`.
 *
 * "Bitta emoji" — ekrandagi bitta belgi (grapheme), kodlar soni emas: 👨‍👩‍👧 (ZWJ oila), 👍🏽 (teri rangi), 🇺🇿 (bayroq),
 * 1️⃣ (keycap) — har biri bitta. Shuning uchun ICU `BreakIterator` bilan grapheme'larga bo'linadi (Android 7+).
 * Emojilar orasidagi bo'shliqlar hisobga olinmaydi.
 */
internal fun emojiOnlyCount(text: String?): Int? {
    val value = text?.trim().orEmpty()
    if (value.isEmpty()) return null
    val iterator = BreakIterator.getCharacterInstance().apply { setText(value) }
    var count = 0
    var start = iterator.first()
    var end = iterator.next()
    while (end != BreakIterator.DONE) {
        val cluster = value.substring(start, end)
        if (cluster.isNotBlank()) {
            if (!isEmojiCluster(cluster)) return null
            count++
            if (count > MAX_LARGE_EMOJI) return null
        }
        start = end
        end = iterator.next()
    }
    return count.takeIf { it > 0 }
}

/**
 * Bitta grapheme emojimi. `UProperty.EXTENDED_PICTOGRAPHIC` faqat Android 10+ da bor (minSdk 26), shuning uchun
 * Unicode belgi turi va ma'lum emoji bloklari bo'yicha aniqlanadi:
 *  - keycap (1️⃣, #️⃣): U+20E3 bilan tugaydi;
 *  - bayroq: regional indicator juftligi;
 *  - U+2000 dan yuqori belgi + emoji ko'rinish selektori (U+FE0F): ‼️ ⁉️ ℹ️;
 *  - asosiy belgi "boshqa belgi" (So) turidagi pictogram — 😀 ❤ ✋ ☀ va hokazo (harf, raqam, tinish belgisi emas).
 */
private fun isEmojiCluster(cluster: String): Boolean {
    val first = cluster.codePointAt(0)
    if (cluster.contains(KEYCAP)) return true
    if (first in REGIONAL_INDICATORS) return true
    // ‼️ ⁉️ ℹ️ kabi tinish belgisi/harf turidagilar ham emoji ko'rinish selektori (U+FE0F) bilan emoji bo'ladi.
    if (first >= 0x2000 && cluster.contains(VARIATION_EMOJI)) return true
    if (Character.getType(first) != Character.OTHER_SYMBOL.toInt()) return false
    // So turiga © ® kabi oddiy matn belgilari ham kiradi — ular faqat emoji ko'rinishi (U+FE0F) bilan emoji.
    return first >= 0x2190 || cluster.contains(VARIATION_EMOJI)
}

private const val KEYCAP = '⃣'
private const val VARIATION_EMOJI = '️'
private val REGIONAL_INDICATORS = 0x1F1E6..0x1F1FF
