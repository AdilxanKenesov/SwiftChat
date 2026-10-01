package uz.relay.domain.model

import java.util.Locale

/** Qo'ng'iroq natijasi. */
enum class CallOutcome {
    /** Suhbatlashildi — davomiyligi bilan. */
    ANSWERED,
    /** Javob berilmadi (15 s ichida). */
    MISSED,
    /** Qo'ng'iroq qilingan odam rad etdi. */
    DECLINED,
    /** Qo'ng'iroq qilgan odam javobni kutmay bekor qildi. */
    CANCELED,
    /** Guruh video chati boshlandi (davomiylik yo'q — u tugaganda alohida yozuv keladi). */
    STARTED
}

/**
 * Chatdagi qo'ng'iroq yozuvi (Telegram'dagi "Chiquvchi qo'ng'iroq · 2:31" qatori).
 * [group] — guruh video chati: "boshlandi" ([CallOutcome.STARTED]) yoki "tugadi" ([CallOutcome.ANSWERED] + davomiylik).
 */
data class CallLog(val video: Boolean, val outcome: CallOutcome, val durationSeconds: Long, val group: Boolean = false)

/**
 * Qo'ng'iroq tarixi chatda ODDIY MATNLI xabar sifatida saqlanadi: Relay'da "qo'ng'iroq" turidagi xabar yo'q, SYSTEM
 * xabarni esa klient yubora olmaydi. Shuning uchun format:
 *  - boshqa klientlarda ham tushunarli ko'rinishi uchun inglizcha va o'qiladigan: `📞 Call · audio · 2:31`,
 *    `📞 Call · video · missed`;
 *  - bizning ilova uni [parse] bilan taniydi va tilga mos chiroyli qator qilib chizadi.
 *
 * Xabarni faqat qo'ng'iroq QILGAN tomon yuboradi — aks holda bitta qo'ng'iroq uchun ikkita yozuv bo'lardi.
 * Yo'nalish ("chiquvchi"/"kiruvchi") xabar kimniki ekanidan (isMine) aniqlanadi.
 *
 * Guruh video chati: `📞 Group call · video · started` (xonani ochgan odam yuboradi) va
 * `📞 Group call · video · 12:34` (xonadan oxirgi chiqqan odam yuboradi).
 */
object CallLogFormat {

    private const val PREFIX = "📞 Call · "
    private const val GROUP_PREFIX = "📞 Group call · "
    private val REGEX = Regex("""^📞 (Call|Group call) · (audio|video) · (started|missed|declined|canceled|(\d+):(\d{2})(?::(\d{2}))?)$""")

    fun format(log: CallLog): String {
        val kind = if (log.video) "video" else "audio"
        val tail = when (log.outcome) {
            CallOutcome.ANSWERED -> duration(log.durationSeconds)
            CallOutcome.MISSED -> "missed"
            CallOutcome.DECLINED -> "declined"
            CallOutcome.CANCELED -> "canceled"
            CallOutcome.STARTED -> "started"
        }
        return "${if (log.group) GROUP_PREFIX else PREFIX}$kind · $tail"
    }

    /** Qo'ng'iroq yozuvi bo'lmasa `null` — oddiy matnli xabar. */
    fun parse(body: String?): CallLog? {
        val match = REGEX.matchEntire(body?.trim() ?: return null) ?: return null
        val group = match.groupValues[1] == "Group call"
        val video = match.groupValues[2] == "video"
        return when (val tail = match.groupValues[3]) {
            "started" -> CallLog(video, CallOutcome.STARTED, 0, group).takeIf { group }
            "missed" -> CallLog(video, CallOutcome.MISSED, 0, group)
            "declined" -> CallLog(video, CallOutcome.DECLINED, 0, group)
            "canceled" -> CallLog(video, CallOutcome.CANCELED, 0, group)
            else -> {
                // "m:ss" yoki "h:mm:ss" — guruhlar: 4 = birinchi son, 5 = ikkinchi, 6 = (bo'lsa) uchinchi.
                val a = match.groupValues[4].toLong()
                val b = match.groupValues[5].toLong()
                val c = match.groupValues[6].takeIf { it.isNotEmpty() }?.toLong()
                val seconds = if (c != null) a * 3600 + b * 60 + c else a * 60 + b
                CallLog(video, CallOutcome.ANSWERED, seconds, group).takeIf { tail.isNotEmpty() }
            }
        }
    }

    /** "2:31", "1:02:03". */
    fun duration(seconds: Long): String {
        val s = seconds.coerceAtLeast(0)
        val h = s / 3600
        val m = (s % 3600) / 60
        val sec = s % 60
        // Locale.ROOT — har qanday tilda lotin raqamlari (format matni boshqa klientlarda ham bir xil o'qilsin).
        return if (h > 0) String.format(Locale.ROOT, "%d:%02d:%02d", h, m, sec) else String.format(Locale.ROOT, "%d:%02d", m, sec)
    }
}
