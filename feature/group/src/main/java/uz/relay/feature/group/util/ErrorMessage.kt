package uz.relay.feature.group.util

import androidx.annotation.StringRes
import uz.relay.core.common.result.AppError
import uz.relay.core.common.result.ErrorCodes
import uz.relay.feature.group.R

/**
 * [AppError] ni foydalanuvchiga ko'rsatiladigan matn resursiga aylantiradi.
 *
 * ViewModel [AppError] ni saqlaydi, matnga UI aylantiradi — ViewModel Android resurslariga bog'lanmaydi
 * (unit-test qilish oson, til almashsa matn avtomatik to'g'ri chiqadi). Guruh ekranlari uchun muhim holatlar:
 * internet yo'qligi, rate-limit (429) va ruxsat yo'qligi (403 — masalan, oddiy a'zo admin amalini bajarmoqchi bo'lsa).
 */
@StringRes
fun AppError.messageRes(): Int = when (this) {
    AppError.Network -> R.string.no_internet
    is AppError.Unknown -> R.string.error_unknown
    is AppError.Api -> when {
        code == ErrorCodes.RATE_LIMITED || httpStatus == 429 -> R.string.error_rate_limited
        code == ErrorCodes.FORBIDDEN || httpStatus == 403 -> R.string.error_forbidden
        else -> R.string.error_unknown
    }
}
