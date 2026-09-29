package uz.relay.feature.auth.splash

import org.orbitmvi.orbit.OrbitContainerHost

/**
 * Splash ekranining Orbit MVI kontrakti.
 *
 * Contract interfeysi ekranning barcha qismlarini (ViewModel, UiState, SideEffect, Directions)
 * bir joyda guruhlaydi - ekran nimani ko'rsatishi va qayerga o'tishi bitta faylda ko'rinadi.
 * Splash hech qanday holat ko'rsatmaydi, shuning uchun UiState bo'sh `object`, Intent esa yo'q.
 */
interface SplashContract {

    interface ViewModel : OrbitContainerHost<UiState, UiState, SideEffect>

    sealed interface SideEffect

    // Splash'da o'zgaruvchan holat yo'q - faqat logo va animatsiya.
    object UiState

    /** Saqlangan sessiya holatiga qarab birinchi ekranni tanlash yo'llari. */
    interface Directions {
        suspend fun navigateToPhone()
        suspend fun navigateToProfileSetup()
        suspend fun navigateToChats()
    }
}
