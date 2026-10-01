package uz.relay.feature.conversation.util

import androidx.annotation.StringRes
import uz.relay.core.common.result.AppError
import uz.relay.core.common.result.ErrorCodes
import uz.relay.feature.conversation.R

/**
 * ViewModel [AppError] ni saqlaydi, matnga UI aylantiradi — ViewModel Android resurslariga bog'lanmaydi.
 * Server xato kodi (ErrorCodes) ustun, HTTP status esa zaxira: ba'zi javoblarda body bo'lmasligi mumkin.
 */
@StringRes
fun AppError.messageRes(): Int = when (this) {
    AppError.Network -> R.string.no_internet
    is AppError.Unknown -> R.string.error_unknown
    is AppError.Api -> when {
        code == ErrorCodes.PAYLOAD_TOO_LARGE || httpStatus == 413 -> R.string.error_too_large
        code == ErrorCodes.CALLS_UNAVAILABLE -> R.string.error_calls_unavailable
        code == ErrorCodes.CALL_FAILED -> R.string.error_call_failed
        code == ErrorCodes.RATE_LIMITED || httpStatus == 429 -> R.string.error_rate_limited
        code == ErrorCodes.EDIT_WINDOW_EXPIRED -> R.string.error_edit_window
        code == ErrorCodes.FORBIDDEN -> R.string.error_forbidden
        else -> R.string.error_unknown
    }
}
