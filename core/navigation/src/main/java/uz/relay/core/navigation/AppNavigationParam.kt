package uz.relay.core.navigation

import androidx.navigation3.runtime.NavKey

/**
 * One navigation command. ViewModels decide "where" through their Directions;
 * the app module applies the command to the back stack.
 */
sealed interface AppNavigationParam {

    /** Push [key]. With [singleTop], nothing happens if it is already on top. */
    data class To(val key: NavKey, val singleTop: Boolean = true) : AppNavigationParam

    /** Replace the current screen (back does not return to it). */
    data class Replace(val key: NavKey) : AppNavigationParam

    data object Back : AppNavigationParam

    /** Pop back to [key]; with [inclusive] it is removed too. */
    data class BackTo(val key: NavKey, val inclusive: Boolean = false) : AppNavigationParam

    /**
     * [key] stekda bo'lsa — unga qaytish (ustidagilar yopiladi), bo'lmasa — yangisini ochish.
     * Masalan: chat → profil → "Xabar" o'sha chatning ikkinchi nusxasini ochmasdan, unga qaytaradi.
     */
    data class BackToOrTo(val key: NavKey) : AppNavigationParam

    /** Clear the stack and leave only [key] (login, logout). */
    data class ResetTo(val key: NavKey) : AppNavigationParam
}
