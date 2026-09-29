package uz.relay.feature.conversation.util

import android.content.res.Resources
import uz.relay.feature.conversation.R
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private val TIME = DateTimeFormatter.ofPattern("HH:mm")
private val FULL_DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy")

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

/**
 * "oxirgi marta …" uchun nisbiy vaqt: "hozirgina", "5 daqiqa oldin", "3 soat oldin", "kecha 21:14",
 * undan eskisi — to'liq sana.
 */
fun formatLastSeen(
    lastSeenAt: Long,
    resources: Resources,
    now: Long = System.currentTimeMillis(),
    zone: ZoneId = ZoneId.systemDefault()
): String {
    val minutes = (now - lastSeenAt) / 60_000
    val seen = Instant.ofEpochMilli(lastSeenAt).atZone(zone)
    val daysAgo = ChronoUnit.DAYS.between(seen.toLocalDate(), LocalDate.ofInstant(Instant.ofEpochMilli(now), zone))
    val relative = when {
        minutes < 1 -> resources.getString(R.string.just_now)
        minutes < 60 -> resources.getString(R.string.minutes_ago, minutes.toInt())
        daysAgo == 0L -> resources.getString(R.string.hours_ago, (minutes / 60).toInt())
        daysAgo == 1L -> resources.getString(R.string.yesterday_at, TIME.format(seen))
        else -> FULL_DATE.format(seen)
    }
    return resources.getString(R.string.last_seen, relative)
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
