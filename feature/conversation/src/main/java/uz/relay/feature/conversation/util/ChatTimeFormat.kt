package uz.relay.feature.conversation.util

import android.content.res.Resources
import uz.relay.feature.conversation.R
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val TIME = DateTimeFormatter.ofPattern("HH:mm")

/** Bubble ichidagi vaqt: "10:08". */
fun formatMessageTime(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault()): String =
    TIME.format(Instant.ofEpochMilli(epochMillis).atZone(zone))

/** Kun boshining vaqti — sana ajratgichlarini guruhlash kaliti (bir kundagi xabarlar bitta guruh). */
fun dayStartMillis(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault()): Long =
    Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate().atStartOfDay(zone).toInstant().toEpochMilli()

/** Sana ajratgichi: "Bugun", "Kecha", "24-sentabr" (boshqa yil bo'lsa — "24-sentabr, 2025"). */
fun formatDateSeparator(
    dayStart: Long,
    resources: Resources,
    now: Long = System.currentTimeMillis(),
    zone: ZoneId = ZoneId.systemDefault()
): String {
    val date = Instant.ofEpochMilli(dayStart).atZone(zone).toLocalDate()
    val today = LocalDate.ofInstant(Instant.ofEpochMilli(now), zone)
    val month = resources.getString(monthRes(date.monthValue))
    return when {
        date == today -> resources.getString(R.string.today)
        date == today.minusDays(1) -> resources.getString(R.string.yesterday)
        date.year == today.year -> resources.getString(R.string.day_month, date.dayOfMonth, month)
        else -> resources.getString(R.string.day_month_year, date.dayOfMonth, month, date.year)
    }
}

private fun monthRes(month: Int): Int = when (month) {
    1 -> R.string.month_1
    2 -> R.string.month_2
    3 -> R.string.month_3
    4 -> R.string.month_4
    5 -> R.string.month_5
    6 -> R.string.month_6
    7 -> R.string.month_7
    8 -> R.string.month_8
    9 -> R.string.month_9
    10 -> R.string.month_10
    11 -> R.string.month_11
    else -> R.string.month_12
}
