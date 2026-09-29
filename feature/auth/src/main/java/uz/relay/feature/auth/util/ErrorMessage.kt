package uz.relay.feature.auth.util

import androidx.annotation.StringRes
import uz.relay.core.common.result.AppError
import uz.relay.core.common.result.ErrorCodes
import uz.relay.feature.auth.R

/** ViewModels keep the [AppError]; the UI turns it into text, so ViewModels stay free of resources. */
@StringRes
fun AppError.messageRes(): Int = when (this) {
    AppError.Network -> R.string.no_internet
    is AppError.Unknown -> R.string.error_unknown
    is AppError.Api -> when {
        code == ErrorCodes.RATE_LIMITED || httpStatus == 429 -> R.string.error_rate_limited
        code == ErrorCodes.OTP_DELIVERY_UNAVAILABLE -> R.string.error_otp_unavailable
        else -> R.string.error_unknown
    }
}
