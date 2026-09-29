package uz.relay.core.navigation

import androidx.navigation3.runtime.NavKey

/**
 * Bitta navigatsiya buyrug'i. ViewModel'lar "qayerga"ni o'z Directions'i orqali hal qiladi,
 * app moduli esa buyruqni back stack'ga qo'llaydi. Sealed — AppNavHost'dagi `when` har bir turni
 * majburan ko'rib chiqadi.
 */
sealed interface AppNavigationParam {

    /** [key] ni stekka qo'shish. [singleTop] bo'lsa va u allaqachon tepada bo'lsa, hech narsa qilinmaydi (ikki marta bosishdan himoya). */
    data class To(val key: NavKey, val singleTop: Boolean = true) : AppNavigationParam

    /** Joriy ekranni almashtirish (orqaga bosilganda unga qaytilmaydi). */
    data class Replace(val key: NavKey) : AppNavigationParam

    /** Bitta ekran orqaga. */
    data object Back : AppNavigationParam

    /** [key] gacha orqaga qaytish; [inclusive] bo'lsa uning o'zi ham olib tashlanadi. */
    data class BackTo(val key: NavKey, val inclusive: Boolean = false) : AppNavigationParam

    /**
     * [key] stekda bo'lsa — unga qaytish (ustidagilar yopiladi), bo'lmasa — yangisini ochish.
     * Masalan: chat → profil → "Xabar" o'sha chatning ikkinchi nusxasini ochmasdan, unga qaytaradi.
     */
    data class BackToOrTo(val key: NavKey) : AppNavigationParam

    /** Stekni tozalab, faqat [key] ni qoldirish (login, logout). */
    data class ResetTo(val key: NavKey) : AppNavigationParam
}
