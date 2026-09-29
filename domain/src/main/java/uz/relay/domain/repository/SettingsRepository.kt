package uz.relay.domain.repository

import kotlinx.coroutines.flow.Flow
import uz.relay.domain.model.ThemeMode

/**
 * Qurilma sozlamalari. Hisobga emas, qurilmaga tegishli — shuning uchun logout'da o'chirilmaydi
 * (boshqa hisobga kirilganda ham tanlangan tema saqlanib qoladi).
 */
interface SettingsRepository {

    val themeMode: Flow<ThemeMode>

    suspend fun setThemeMode(mode: ThemeMode)

    /** Bildirishnomalar yoqilganmi. Push bosqichida notification ko'rsatishdan oldin shu tekshiriladi. */
    val notificationsEnabled: Flow<Boolean>

    suspend fun setNotificationsEnabled(enabled: Boolean)
}
