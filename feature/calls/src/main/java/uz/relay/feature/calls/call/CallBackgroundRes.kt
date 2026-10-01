package uz.relay.feature.calls.call

import androidx.annotation.DrawableRes
import uz.relay.feature.calls.R

/**
 * Rasmli fonning resursi (`drawable-nodpi` — BitmapFactory uni zichlikka qarab kattalashtirmasin, kadr o'lchamiga
 * filtrning o'zi moslaydi). Rasmlar ilova uchun generatsiya qilingan gradient/bokeh — litsenziya talab qilmaydi.
 * Rasmsiz variantlar (yo'q, xira) uchun `null`.
 */
@DrawableRes
internal fun CallBackground.imageResOrNull(): Int? = when (this) {
    CallBackground.NONE, CallBackground.BLUR -> null
    CallBackground.BRAND -> R.drawable.call_bg_brand
    CallBackground.SUNSET -> R.drawable.call_bg_sunset
    CallBackground.NIGHT -> R.drawable.call_bg_night
    CallBackground.NATURE -> R.drawable.call_bg_nature
}

@DrawableRes
internal fun CallBackground.imageRes(): Int = requireNotNull(imageResOrNull()) { "$this has no image" }
