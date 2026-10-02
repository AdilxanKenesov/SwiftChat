package uz.relay.data.repository_impl

import kotlinx.coroutines.flow.Flow
import uz.relay.data.locale.AppLocaleManager
import uz.relay.data.source.local.AppSettingsStorage
import uz.relay.domain.model.AppLanguage
import uz.relay.domain.model.ThemeMode
import uz.relay.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * [SettingsRepository] implementatsiyasi: mavzu, bildirishnomalar va ilova tili.
 *
 * Yupqa qatlam — domain faqat interfeysni biladi, saqlash esa DataStore ([AppSettingsStorage]) va tilni
 * tizimga qo'llash [AppLocaleManager] zimmasida. Sozlamalar ekrani va ilova mavzusini kuzatuvchi qism ishlatadi.
 */
internal class SettingsRepositoryImpl @Inject constructor(
    private val storage: AppSettingsStorage,
    private val localeManager: AppLocaleManager
) : SettingsRepository {

    override val themeMode: Flow<ThemeMode> = storage.themeMode

    override suspend fun setThemeMode(mode: ThemeMode) = storage.setThemeMode(mode)

    override val notificationsEnabled: Flow<Boolean> = storage.notificationsEnabled

    override suspend fun setNotificationsEnabled(enabled: Boolean) = storage.setNotificationsEnabled(enabled)

    override val language: Flow<AppLanguage> = localeManager.language

    override suspend fun setLanguage(language: AppLanguage) = localeManager.set(language)

    override val recentEmojis: Flow<List<String>> = storage.recentEmojis

    override suspend fun addRecentEmoji(emoji: String) = storage.addRecentEmoji(emoji)
}
