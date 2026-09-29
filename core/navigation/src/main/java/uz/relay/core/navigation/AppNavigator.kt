package uz.relay.core.navigation

/**
 * ViewModel tomoni: UI'ni bilmagan holda navigatsiya so'raydi. Feature'lardagi Directions implementatsiyalari
 * shu interfeys orqali buyruq yuboradi.
 */
interface AppNavigator {

    /** Buyruqni navbatga qo'yadi; AppNavHost uni back stack'ga qo'llaydi. */
    suspend fun navigate(param: AppNavigationParam)
}
