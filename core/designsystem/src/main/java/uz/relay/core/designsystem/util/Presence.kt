package uz.relay.core.designsystem.util

import android.content.res.Resources
import uz.relay.core.designsystem.R
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

// Formatter'lar thread-safe va qimmat — bir marta yaratiladi.
private val TIME = DateTimeFormatter.ofPattern("HH:mm")
private val FULL_DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy")

/**
 * Odamning holat qatori: "online" yoki "oxirgi marta …" ("hozirgina", "5 daqiqa oldin", "3 soat oldin",
 * "kecha 21:14", undan eskisi — to'liq sana). Oxirgi ko'rilgan vaqt noma'lum bo'lsa — "oxirgi marta yaqinda".
 *
 * designsystem'da, chunki bir nechta feature (chatlar, suhbat, profil, guruh) bir xil matnni ko'rsatadi.
 * [now] va [zone] parametr — test'da vaqtni qat'iy berish uchun.
 */
fun formatPresence(
    online: Boolean,
    lastSeenAt: Long?,
    resources: Resources,
    now: Long = System.currentTimeMillis(),
    zone: ZoneId = ZoneId.systemDefault()
): String {
    if (online) return resources.getString(R.string.presence_online)
    if (lastSeenAt == null) return resources.getString(R.string.presence_last_seen, resources.getString(R.string.presence_recently))

    val minutes = (now - lastSeenAt) / 60_000
    val seen = Instant.ofEpochMilli(lastSeenAt).atZone(zone)
    // Kunlar farqi kalendar sanasi bo'yicha: 23:50 da ko'rilgan va hozir 00:10 bo'lsa — "kecha".
    val daysAgo = ChronoUnit.DAYS.between(seen.toLocalDate(), LocalDate.ofInstant(Instant.ofEpochMilli(now), zone))
    val relative = when {
        minutes < 1 -> resources.getString(R.string.presence_just_now)
        minutes < 60 -> resources.getQuantityString(R.plurals.presence_minutes_ago, minutes.toInt(), minutes.toInt())
        daysAgo == 0L -> resources.getQuantityString(R.plurals.presence_hours_ago, (minutes / 60).toInt(), (minutes / 60).toInt())
        daysAgo == 1L -> resources.getString(R.string.presence_yesterday_at, TIME.format(seen))
        else -> FULL_DATE.format(seen)
    }
    return resources.getString(R.string.presence_last_seen, relative)
}
