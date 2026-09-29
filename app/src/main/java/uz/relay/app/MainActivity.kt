package uz.relay.app

import android.content.Context
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import uz.relay.data.locale.AppLocaleManager
import javax.inject.Inject
import dagger.hilt.android.AndroidEntryPoint
import uz.relay.app.navigation.AppNavHost
import uz.relay.core.designsystem.theme.SwiftChatTheme
import uz.relay.domain.model.ThemeMode

/**
 * Ilovaning yagona Activity'si (single-activity): hamma ekranlar Compose'da, Navigation 3 orqali
 * [AppNavHost] ichida almashadi.
 *
 * Vazifalari: splash screen, edge-to-edge, tema (ilova sozlamasidan, tizimdan emas) va til.
 * Til per-app locale: Android 13+ da tizimning o'z mexanizmi, 8–12 da esa [AppLocaleManager.wrap] bilan
 * Context almashtiriladi va til o'zgarsa Activity qayta yaratiladi (pastdagi `observeLanguage`).
 *
 * `@AndroidEntryPoint` — Hilt bu Activity'ga va uning ViewModel'lariga inject qila olishi uchun.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    /** Tanlangan tilni kuzatish va Android 8–12 da qo'llash uchun. */
    @Inject
    lateinit var localeManager: AppLocaleManager

    /** Android 8–12: tanlangan til shu yerda qo'llanadi (13+ da tizim o'zi qiladi). */
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLocaleManager.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Tema DataStore'dan o'qilmaguncha splash ushlab turiladi — noto'g'ri tema bir lahza miltillamasligi uchun.
        // Tizim splash'i tema VA boshlang'ich ekran aniqlanguncha turadi — shu bilan yagona splash (ikkinchisi yo'q).
        installSplashScreen().setKeepOnScreenCondition { viewModel.themeMode.value == null || viewModel.startKey.value == null }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        observeLanguage()

        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            val darkTheme = themeMode == ThemeMode.DARK

            // Status/navigation bar ikonkalari tizim temasiga emas, ILOVA temasiga moslanadi: aks holda
            // telefon tungi, ilova kunduzgi rejimda bo'lsa, oq ikonkalar oq fonda ko'rinmay qoladi.
            DisposableEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme },
                    navigationBarStyle = SystemBarStyle.auto(LightScrim, DarkScrim) { darkTheme }
                )
                // Tozalash kerak emas: keyingi tema kelganda enableEdgeToEdge qayta chaqiriladi.
                onDispose {}
            }

            SwiftChatTheme(darkTheme = darkTheme) {
                val startKey by viewModel.startKey.collectAsState()
                // null bo'lsa ekranda tizim splash'i turibdi — hech narsa chizish shart emas.
                startKey?.let { AppNavHost(navigationHandler = viewModel.navigationHandler, startKey = it) }
            }
        }
    }
}

/**
 * Android 13+ da til o'zgarsa tizim Activity'ni o'zi qayta yaratadi. 8–12 da buni biz qilamiz: Activity qaysi
 * til bilan yaratilgan bo'lsa, undan farqli til kelishi bilan `recreate()` — yangi Context [AppLocaleManager.wrap]dan.
 */
private fun MainActivity.observeLanguage() {
    // Til tizim sozlamalarida (ilovadan tashqarida) o'zgargan bo'lishi mumkin.
    localeManager.refresh()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return
    val createdWith = localeManager.language.value
    lifecycleScope.launch {
        repeatOnLifecycle(Lifecycle.State.STARTED) {
            localeManager.language.collect { if (it != createdWith) recreate() }
        }
    }
}

/** `enableEdgeToEdge()` ning standart navigation bar scrim ranglari (3 tugmali navigatsiyada ko'rinadi). */
private val LightScrim = Color.argb(0xe6, 0xFF, 0xFF, 0xFF)
private val DarkScrim = Color.argb(0x80, 0x1b, 0x1b, 0x1b)
