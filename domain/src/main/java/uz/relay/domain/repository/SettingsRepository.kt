package uz.relay.domain.repository

import kotlinx.coroutines.flow.Flow
import uz.relay.domain.model.AppLanguage
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

    /** Hozirgi til: foydalanuvchi tanlagani, tanlanmagan bo'lsa — telefonga qarab. */
    val language: Flow<AppLanguage>

    /** Tilni almashtirish. Ekranlar yangi tilda qayta chiziladi (Activity qayta yaratiladi). */
    suspend fun setLanguage(language: AppLanguage)
}
