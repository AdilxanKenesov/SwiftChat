package uz.relay.core.common.result

/**
 * Butun ilova uchun yagona xato turi. Tarmoq qatlami HttpException / IOException'ni shunga aylantiradi,
 * shuning uchun feature'lar faqat [Api.code] va [isRetryable] ga qaraydi (API'ning `{code, message, retryable}`
 * shakli).
 *
 * Nega sealed interface: `when` barcha holatlarni (server xatosi, tarmoq yo'q, kutilmagan) majburan ko'rib
 * chiqtiradi, va Retrofit/OkHttp exception turlari data modulidan tashqariga chiqmaydi.
 */
sealed interface AppError {

    /** Server xato tanasi bilan javob berdi: `{code, message, retryable}`. */
    data class Api(
        val httpStatus: Int,
        val code: String,
        val message: String,
        val retryable: Boolean,
        /** Faqat 409 TELEGRAM_NOT_LINKED da: foydalanuvchi Telegram botni ochishi uchun havola. */
        val botUrl: String? = null
    ) : AppError

    /** Serverga yetib bo'lmadi (internet yo'q, timeout). */
    data object Network : AppError

    /** Kutilmagan har qanday xato (masalan, javobni parse qilib bo'lmadi). */
    data class Unknown(val throwable: Throwable) : AppError
}

/**
 * Qayta urinish ma'nolimi: server o'zi aytgan `retryable`, tarmoq xatosi — ha, kutilmagan xato — yo'q.
 * Outbox va UI'dagi "Qayta urinish" tugmasi shunga tayanadi.
 */
val AppError.isRetryable: Boolean
    get() = when (this) {
        is AppError.Api -> retryable
        AppError.Network -> true
        is AppError.Unknown -> false
    }

/** Relay API xato kodlari (qo'llanma, 9-bo'lim). String konstantalar — xato kodini yozishda adashmaslik uchun. */
object ErrorCodes {
    const val VALIDATION_ERROR = "VALIDATION_ERROR"
    const val UNAUTHORIZED = "UNAUTHORIZED"
    const val TOKEN_EXPIRED = "TOKEN_EXPIRED"
    const val TOKEN_REUSED = "TOKEN_REUSED"
    const val INVALID_OTP = "INVALID_OTP"
    const val OTP_EXPIRED = "OTP_EXPIRED"
    const val OTP_LOCKED = "OTP_LOCKED"
    const val TELEGRAM_NOT_LINKED = "TELEGRAM_NOT_LINKED"
    const val OTP_DELIVERY_UNAVAILABLE = "OTP_DELIVERY_UNAVAILABLE"
    const val FORBIDDEN = "FORBIDDEN"
    const val NOT_FOUND = "NOT_FOUND"
    const val USERNAME_TAKEN = "USERNAME_TAKEN"
    const val EDIT_WINDOW_EXPIRED = "EDIT_WINDOW_EXPIRED"
    const val RATE_LIMITED = "RATE_LIMITED"
    const val SERVER_ERROR = "SERVER_ERROR"

    // Media
    const val PAYLOAD_TOO_LARGE = "PAYLOAD_TOO_LARGE"
    const val OFFSET_MISMATCH = "OFFSET_MISMATCH"
    const val UPLOAD_EXPIRED = "UPLOAD_EXPIRED"
    const val SHA256_MISMATCH = "SHA256_MISMATCH"
    const val MEDIA_NOT_READY = "MEDIA_NOT_READY"

    // Qo'ng'iroqlar (klient kodlari — Stream Video xatolari shularga aylantiriladi)
    /** Qo'ng'iroq xizmatiga ulanmagan (API key yo'q yoki hali ulanmoqda). */
    const val CALLS_UNAVAILABLE = "CALLS_UNAVAILABLE"
    /** Qo'ng'iroqni yaratib/jiringlatib bo'lmadi. */
    const val CALL_FAILED = "CALL_FAILED"

    /** Klient kodi: yuboriladigan fayl qurilmadan yo'qolgan (o'chirilgan) — qayta urinish befoyda. */
    const val MEDIA_FILE_MISSING = "MEDIA_FILE_MISSING"
}
