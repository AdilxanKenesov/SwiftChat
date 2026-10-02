package uz.relay.feature.profile.me

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.combine
import org.orbitmvi.orbit.viewmodel.orbitContainer
import uz.relay.domain.model.AppLanguage
import uz.relay.domain.model.ConnectionStatus
import uz.relay.domain.model.ThemeMode
import uz.relay.domain.model.User
import uz.relay.domain.usecase.auth.LogoutUseCase
import uz.relay.domain.usecase.chat.ObserveConnectionStatusUseCase
import uz.relay.domain.usecase.settings.ObserveLanguageUseCase
import uz.relay.domain.usecase.settings.ObserveNotificationsEnabledUseCase
import uz.relay.domain.usecase.settings.ObserveThemeModeUseCase
import uz.relay.domain.usecase.settings.SetLanguageUseCase
import uz.relay.domain.usecase.settings.SetNotificationsEnabledUseCase
import uz.relay.domain.usecase.settings.SetThemeModeUseCase
import uz.relay.domain.usecase.user.ObserveMeUseCase
import uz.relay.domain.usecase.user.RefreshMeUseCase
import javax.inject.Inject

/**
 * "Mening profilim" ViewModel'i. Profil (Room keshi), ulanish holati va sozlamalarni bitta state'ga yig'adi.
 *
 * Til va tema kabi o'zgarishlar to'g'ridan-to'g'ri DataStore'ga emas, use case'lar orqali yoziladi: feature
 * modul saqlash tafsilotini bilmaydi, til almashganda locale qo'llash kabi qo'shimcha ishlar esa bitta joyda
 * (domain/data'da) bajariladi. Sozlamalar alohida ombor (DataStore) da, profil ma'lumotlari bilan aralashmaydi:
 * ular qurilmaga tegishli va logout'da tozalanmasligi, serverga ham ketmasligi kerak.
 */
@HiltViewModel
class MyProfileViewModel @Inject constructor(
    private val observeMe: ObserveMeUseCase,
    private val refreshMe: RefreshMeUseCase,
    private val observeConnectionStatus: ObserveConnectionStatusUseCase,
    private val observeThemeMode: ObserveThemeModeUseCase,
    private val setThemeMode: SetThemeModeUseCase,
    private val observeNotificationsEnabled: ObserveNotificationsEnabledUseCase,
    private val setNotificationsEnabled: SetNotificationsEnabledUseCase,
    private val observeLanguage: ObserveLanguageUseCase,
    private val setLanguage: SetLanguageUseCase,
    private val logout: LogoutUseCase,
    private val directions: MyProfileContract.Directions
) : ViewModel(), MyProfileContract.ViewModel {

    override val container =
        orbitContainer<MyProfileContract.UiState, MyProfileContract.SideEffect>(MyProfileContract.UiState()) {
            observeData()
            refresh()
        }

    /** Screen'dan keladigan barcha Intent'lar uchun yagona kirish nuqtasi. */
    override fun onEventDispatcher(intent: MyProfileContract.Intent) {
        when (intent) {
            MyProfileContract.Intent.OnBack -> intent { directions.back() }
            MyProfileContract.Intent.OnEdit -> intent { directions.navigateToEditProfile() }
            is MyProfileContract.Intent.OnNotificationsChange -> intent { setNotificationsEnabled(intent.enabled) }
            is MyProfileContract.Intent.OnThemeChange -> intent { setThemeMode(intent.mode) }
            // Til almashsa Activity yangi tilda qayta yaratiladi — ekran o'zi yangilanadi.
            is MyProfileContract.Intent.OnLanguageChange -> intent { setLanguage(intent.language) }
            MyProfileContract.Intent.OnLogout -> logoutNow()
        }
    }

    /**
     * Profil keshdan, sozlamalar DataStore'dan o'qiladi — hammasi Flow: boshqa ekranda ism o'zgarsa yoki
     * switch bosilsa, bu yerda ham o'zi yangilanadi (switch holati ViewModel'da alohida saqlanmaydi).
     */
    private fun observeData() = intent {
        repeatOnSubscription {
            combine(
                observeMe(),
                observeConnectionStatus(),
                observeThemeMode(),
                observeNotificationsEnabled(),
                observeLanguage()
            ) { me, connection, themeMode, notifications, language -> ProfileData(me, connection, themeMode, notifications, language) }
                .collect { data ->
                    reduce {
                        state.copy(
                            me = data.me,
                            connectionStatus = data.connectionStatus,
                            themeMode = data.themeMode,
                            notificationsEnabled = data.notificationsEnabled,
                            language = data.language
                        )
                    }
                }
        }
    }

    /**
     * Ekran avval keshdagini ko'rsatadi, keyin serverdan yangilaydi. Xato ko'rsatilmaydi: keshdagi profil
     * yetarli, internet yo'qligi esa global banner orqali allaqachon ko'rinadi.
     */
    private fun refresh() = intent { refreshMe() }

    /**
     * Chiqish: ikki marta bosilishdan himoya. Keyingi o'tish yo'q — sessiya o'chgach MainViewModel login
     * ekraniga o'zi olib boradi (Directions'da shuning uchun "logout" yo'nalishi yo'q).
     */
    private fun logoutNow() = intent {
        if (state.loggingOut) return@intent
        reduce { state.copy(loggingOut = true) }
        logout()
    }
}

/** `combine` natijasi — `reduce` ichida joriy holatga (masalan, `loggingOut`) qo'shiladi. */
private data class ProfileData(
    val me: User?,
    val connectionStatus: ConnectionStatus,
    val themeMode: ThemeMode,
    val notificationsEnabled: Boolean,
    val language: AppLanguage
)
