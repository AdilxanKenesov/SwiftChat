package uz.relay.domain.model

/**
 * Chatni qancha muddatga ovozsiz qilish. [millis] `null` — muddatsiz: server `mutedUntil = null` ni
 * "o'zim yoqmagunimcha" deb tushunadi.
 */
enum class MuteDuration(val millis: Long?) {
    HOUR(60 * 60 * 1000L),
    EIGHT_HOURS(8 * 60 * 60 * 1000L),
    DAY(24 * 60 * 60 * 1000L),
    FOREVER(null)
}
