package uz.relay.core.common.result

/**
 * The single error type across the app. The network layer maps HttpException / IOException into it,
 * so features only look at [Api.code] and [isRetryable] (the API's `{code, message, retryable}` shape).
 */
sealed interface AppError {

    /** The server answered with an error body `{code, message, retryable}`. */
    data class Api(
        val httpStatus: Int,
        val code: String,
        val message: String,
        val retryable: Boolean,
        /** Only on 409 TELEGRAM_NOT_LINKED. */
        val botUrl: String? = null
    ) : AppError

    /** The server could not be reached (no connection, timeout). */
    data object Network : AppError

    /** Anything unexpected (e.g. a response that failed to parse). */
    data class Unknown(val throwable: Throwable) : AppError
}

val AppError.isRetryable: Boolean
    get() = when (this) {
        is AppError.Api -> retryable
        AppError.Network -> true
        is AppError.Unknown -> false
    }

/** Relay API error codes (Guide, section 9). */
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
}
