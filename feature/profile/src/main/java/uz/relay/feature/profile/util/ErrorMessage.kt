package uz.relay.feature.profile.util

import androidx.annotation.StringRes
import uz.relay.core.common.result.AppError
import uz.relay.core.common.result.ErrorCodes
import uz.relay.feature.profile.R

/**
 * ViewModel [AppError] ni saqlaydi, matnga UI aylantiradi — ViewModel Android resurslariga bog'lanmaydi.
 * Server xato kodi ustun, HTTP status zaxira. USERNAME_TAKEN bu yerda yo'q — u Snackbar emas, maydon ostida ko'rsatiladi.
 */
@StringRes
fun AppError.messageRes(): Int = when (this) {
    AppError.Network -> R.string.no_internet
    is AppError.Unknown -> R.string.error_unknown
    is AppError.Api -> when {
        code == ErrorCodes.RATE_LIMITED || httpStatus == 429 -> R.string.error_rate_limited
        code == ErrorCodes.FORBIDDEN || httpStatus == 403 -> R.string.error_forbidden
        httpStatus == 404 -> R.string.error_not_found
        else -> R.string.error_unknown
    }
}
