package uz.relay.data.source.local

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import uz.relay.domain.model.ThemeMode
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Qurilma sozlamalari (tema, bildirishnomalar). [SessionStorage] dan alohida fayl, chunki:
 * - bu yerda maxfiy narsa yo'q — shifrlash shart emas;
 * - sessiya logout'da to'liq tozalanadi, sozlamalar esa saqlanib qolishi kerak.
 *
 * Nega DataStore (SharedPreferences emas): asinxron, Flow beradi — tema o'zgarsa UI o'zi qayta chiziladi.
 * SettingsRepositoryImpl orqali ishlatiladi.
 */
@Singleton
class AppSettingsStorage @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val dataStore = PreferenceDataStoreFactory.create {
        context.preferencesDataStoreFile(DATASTORE_NAME)
    }

    /** Tanlanmagan yoki noma'lum qiymat (masalan, eski "SYSTEM") — kunduzgi rejim; ilova yiqilmaydi. */
    val themeMode: Flow<ThemeMode> = dataStore.data
        .map { prefs -> prefs[KEY_THEME_MODE]?.let { name -> ThemeMode.entries.firstOrNull { it.name == name } } ?: ThemeMode.LIGHT }
        .distinctUntilChanged()

    /** Standart holatda bildirishnomalar yoqilgan. */
    val notificationsEnabled: Flow<Boolean> = dataStore.data
        .map { it[KEY_NOTIFICATIONS_ENABLED] ?: true }
        .distinctUntilChanged()

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[KEY_THEME_MODE] = mode.name }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_NOTIFICATIONS_ENABLED] = enabled }
    }

    private companion object {
        const val DATASTORE_NAME = "app_settings"
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        val KEY_NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
    }
}
