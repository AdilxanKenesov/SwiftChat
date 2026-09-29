package uz.relay.feature.chats.util

import android.content.res.Resources
import uz.relay.feature.chats.R
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

// Formatter'lar thread-safe va qimmat — bir marta yaratilib qayta ishlatiladi.
private val TIME = DateTimeFormatter.ofPattern("HH:mm")
private val DAY_MONTH = DateTimeFormatter.ofPattern("dd.MM")

/**
 * Chatlar ro'yxatidagi vaqt (spec 3.5):
 *  - bugun → "12:48";
 *  - kecha → "Kecha";
 *  - oxirgi 7 kun ichida → hafta kunining qisqa nomi ("Du", "Se", ...);
 *  - undan eski → "22.09".
 *
 * [now] va [zone] parametr qilib olingan — testda vaqtni qotirib qo'yish mumkin bo'lsin.
 */
fun formatChatTime(
    epochMillis: Long,
    resources: Resources,
    now: Long = System.currentTimeMillis(),
    zone: ZoneId = ZoneId.systemDefault()
): String {
    val dateTime = Instant.ofEpochMilli(epochMillis).atZone(zone)
    val date = dateTime.toLocalDate()
    val today = LocalDate.ofInstant(Instant.ofEpochMilli(now), zone)
    val daysAgo = ChronoUnit.DAYS.between(date, today)

    return when {
        daysAgo <= 0L -> TIME.format(dateTime)
        daysAgo == 1L -> resources.getString(R.string.yesterday)
        daysAgo < 7L -> resources.getString(date.dayOfWeek.shortNameRes())
        else -> DAY_MONTH.format(dateTime)
    }
}

/** Hafta kunining qisqa nomi — tilga qarab string resursdan olinadi. */
private fun DayOfWeek.shortNameRes(): Int = when (this) {
    DayOfWeek.MONDAY -> R.string.day_mon
    DayOfWeek.TUESDAY -> R.string.day_tue
    DayOfWeek.WEDNESDAY -> R.string.day_wed
    DayOfWeek.THURSDAY -> R.string.day_thu
    DayOfWeek.FRIDAY -> R.string.day_fri
    DayOfWeek.SATURDAY -> R.string.day_sat
    DayOfWeek.SUNDAY -> R.string.day_sun
}

private val DAY_MONTH_TIME = DateTimeFormatter.ofPattern("dd.MM HH:mm")

/** Ovozsiz qilish tugaydigan vaqt: bugun bo'lsa "18:30", aks holda "01.10 18:30". */
fun formatMuteUntil(
    epochMillis: Long,
    now: Long = System.currentTimeMillis(),
    zone: ZoneId = ZoneId.systemDefault()
): String {
    val dateTime = Instant.ofEpochMilli(epochMillis).atZone(zone)
    val today = LocalDate.ofInstant(Instant.ofEpochMilli(now), zone)
    return if (dateTime.toLocalDate() == today) TIME.format(dateTime) else DAY_MONTH_TIME.format(dateTime)
}
