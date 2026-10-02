package uz.relay.domain.repository

import kotlinx.coroutines.flow.Flow
import uz.relay.domain.model.AppLanguage
import uz.relay.domain.model.ThemeMode

/**
 * Qurilma sozlamalari. Hisobga emas, qurilmaga tegishli — shuning uchun logout'da o'chirilmaydi
 * (boshqa hisobga kirilganda ham tanlangan tema saqlanib qoladi).
 *
 * Interface domain'da, amalga oshirish `data` modulida (DataStore) — feature'lar saqlash usulini bilmaydi.
 */
interface SettingsRepository {

    /** Tanlangan tema (tanlanmagan bo'lsa — [ThemeMode.LIGHT]). */
    val themeMode: Flow<ThemeMode>

    suspend fun setThemeMode(mode: ThemeMode)

    /** Bildirishnomalar yoqilganmi. Push bosqichida notification ko'rsatishdan oldin shu tekshiriladi. */
    val notificationsEnabled: Flow<Boolean>

    suspend fun setNotificationsEnabled(enabled: Boolean)

    /** Hozirgi til: foydalanuvchi tanlagani, tanlanmagan bo'lsa — telefonga qarab. */
    val language: Flow<AppLanguage>

    /** Tilni almashtirish. Ekranlar yangi tilda qayta chiziladi (Activity qayta yaratiladi). */
    suspend fun setLanguage(language: AppLanguage)

    /** Emoji panelidagi so'nggi ishlatilganlar (birinchisi — eng oxirgisi). Qurilmada saqlanadi, logout'da o'chmaydi. */
    val recentEmojis: Flow<List<String>>

    suspend fun addRecentEmoji(emoji: String)
}
