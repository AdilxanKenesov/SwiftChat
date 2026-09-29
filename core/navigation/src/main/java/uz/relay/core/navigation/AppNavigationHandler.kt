package uz.relay.core.navigation

import kotlinx.coroutines.flow.Flow

/**
 * UI tomoni: app modulidagi AppNavHost bu buyruqlarni yig'ib, back stack'ga qo'llaydi. Interfeys
 * alohida — UI faqat o'qiy oladi, yubora olmaydi.
 */
interface AppNavigationHandler {

    /** Navigatsiya buyruqlari oqimi (har biri bir marta keladi). */
    val params: Flow<AppNavigationParam>
}
