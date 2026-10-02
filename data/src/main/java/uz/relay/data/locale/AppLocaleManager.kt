package uz.relay.data.locale

import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.LocaleList
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import uz.relay.domain.model.AppLanguage

/**
 * Ilova ichidagi til tanlovi ("per-app language"), qo'shimcha kutubxonasiz.
 *
 *  - Android 13+: tizimning [LocaleManager]'i. Tanlov tizimda saqlanadi, Activity'ni tizim o'zi qayta yaratadi,
 *    telefon Sozlamalari → Ilova tili bo'limida ham SwiftChat chiqadi (manifest'dagi `localeConfig`).
 *  - Android 8–12: tanlov SharedPreferences'da, Activity esa [wrap] orqali tanlangan tildagi Context bilan
 *    yaratiladi. SharedPreferences (DataStore emas) — chunki `attachBaseContext` sinxron, Hilt'dan ham oldin.
 *
 * Tanlanmagan bo'lsa Android resurslarni telefon tiliga qarab o'zi tanlaydi (values-ru, values-en, aks holda
 * standart `values` — o'zbekcha). [language] ham shu qoidaga mos keladi.
 *
 * Ishlatuvchilar: SettingsRepositoryImpl (til tanlash ekrani) va MainActivity ([wrap], [refresh]).
 */
@Singleton
class AppLocaleManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _language = MutableStateFlow(readCurrent())
    /** Hozir amalda bo'lgan til — UI'da tanlangan variantni belgilash uchun. */
    val language: StateFlow<AppLanguage> = _language.asStateFlow()

    /** Ilova tilini o'zgartiradi: 13+ da tizim orqali, eski versiyalarda SharedPreferences'ga yozib. */
    fun set(language: AppLanguage) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.getSystemService(LocaleManager::class.java).applicationLocales = LocaleList.forLanguageTags(language.tag)
        } else {
            // commit (apply emas): Activity darhol qayta yaratiladi va wrap() yangi qiymatni o'qishi kerak.
            prefs(context).edit(commit = true) { putString(KEY_LANGUAGE, language.tag) }
        }
        _language.value = language
    }

    /** Til ilovadan tashqarida (Android 13+ tizim sozlamalarida) o'zgargan bo'lishi mumkin — Activity yaratilganda. */
    fun refresh() {
        _language.value = readCurrent()
    }

    /** Tanlangan til, u bo'lmasa telefon tili asosida joriy [AppLanguage]. */
    private fun readCurrent(): AppLanguage = AppLanguage.fromTag(explicitTag() ?: systemTag())

    /** Foydalanuvchi ilova ichida aniq tanlagan til; tanlamagan bo'lsa — `null`. */
    private fun explicitTag(): String? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.getSystemService(LocaleManager::class.java).applicationLocales.get(0)?.language
        } else {
            prefs(context).getString(KEY_LANGUAGE, null)
        }

    /** Telefon tili (ilova tanlovidan qat'i nazar). */
    private fun systemTag(): String? = Resources.getSystem().configuration.locales.get(0)?.language

    companion object {
        private const val PREFS = "app_locale"
        private const val KEY_LANGUAGE = "language"

        private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

        /**
         * Android 8–12: Activity'ning `attachBaseContext`'ida chaqiriladi — tanlangan til bo'lsa, shu tildagi
         * Context qaytadi. 13+ da hech narsa qilmaydi (tizim o'zi qiladi).
         */
        fun wrap(base: Context): Context {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return base
            val tag = prefs(base).getString(KEY_LANGUAGE, null) ?: return base
            val locale = Locale.forLanguageTag(tag)
            // Default locale ham o'zgaradi — sana/son formatlash (String.format, DateFormat) shu tilda bo'lsin.
            Locale.setDefault(locale)
            val config = Configuration(base.resources.configuration).apply { setLocales(LocaleList(locale)) }
            return base.createConfigurationContext(config)
        }
    }
}
