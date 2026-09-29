package uz.relay.data.repository_impl

import android.os.Build
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import uz.relay.core.common.dispatcher.AppDispatchers
import uz.relay.core.common.result.AppResult
import uz.relay.core.common.result.map
import uz.relay.core.common.result.onSuccess
import uz.relay.data.mapper.toSession
import uz.relay.data.model.request.OtpRequest
import uz.relay.data.model.request.VerifyOtpRequest
import uz.relay.data.source.local.SessionStorage
import uz.relay.data.outbox.OutboxScheduler
import uz.relay.data.source.local.database.RelayDatabase
import uz.relay.data.source.network.api.AuthApi
import uz.relay.data.source.network.api.SessionApi
import uz.relay.data.source.network.realtime.RealtimeClient
import uz.relay.data.utils.safeApiCall
import uz.relay.domain.model.AuthState
import uz.relay.domain.repository.AuthRepository
import javax.inject.Inject

internal class AuthRepositoryImpl @Inject constructor(
    private val authApi: AuthApi,
    private val sessionApi: SessionApi,
    private val realtimeClient: RealtimeClient,
    private val outboxScheduler: OutboxScheduler,
    private val sessionStorage: SessionStorage,
    private val database: RelayDatabase,
    private val dispatchers: AppDispatchers
) : AuthRepository {

    override val authState: Flow<AuthState> = combine(
        sessionStorage.session,
        sessionStorage.profileSetupPending
    ) { session, profilePending ->
        when {
            session == null -> AuthState.LOGGED_OUT
            profilePending -> AuthState.NEEDS_PROFILE
            else -> AuthState.LOGGED_IN
        }
    }.distinctUntilChanged()

    override suspend fun requestOtp(phone: String): AppResult<Unit> =
        safeApiCall { authApi.requestOtp(OtpRequest(phone)) }

    override suspend fun verifyOtp(phone: String, code: String): AppResult<Boolean> =
        safeApiCall { authApi.verifyOtp(VerifyOtpRequest(phone, code, deviceName())) }
            .onSuccess { tokens ->
                // Oldingi hisobning lokal ma'lumoti (chatlar, profillar, sync kursori) yangi hisobga
                // aralashmasligi kerak: sessiya TOKEN_REUSED bilan tugagan bo'lsa, u hech kim tomonidan
                // tozalanmagan bo'lishi mumkin.
                clearLocalData()
                // Yangi foydalanuvchi avval profilini to'ldiradi.
                sessionStorage.save(tokens.toSession(), profileSetupPending = tokens.isNewUser)
            }
            .map { it.isNewUser }

    override suspend fun completeProfileSetup() {
        sessionStorage.setProfileSetupPending(false)
    }

    /**
     * Tartib muhim:
     * 1. Socket va outbox avval to'xtatiladi — server socket'ni 4003 bilan yopganda RealtimeClient qayta ulanish
     *    / token yangilashga urinmasin, navbatdagi xabar esa chiqib ketayotgan hisob nomidan yuborilmasin.
     * 2. Server so'rovi sessiya HALI saqlangan paytda yuboriladi (unga access token kerak). Natijasi
     *    kutilmaydi: internet yo'q bo'lsa ham foydalanuvchi chiqib keta olishi kerak, shuning uchun 5 s limit.
     *    Bunda serverdagi refresh token o'z muddatida o'ladi, lekin qurilmada u endi saqlanmaydi.
     * 3. Baza tozalanadi — keyingi hisob oldingisining chatlarini ko'rmasin.
     * 4. Eng oxirida sessiya o'chiriladi → authState LOGGED_OUT → MainViewModel login ekraniga o'tkazadi.
     *    Oxirida, chunki ekran yopilishi bilan uni chaqirgan ViewModel'ning coroutine'i bekor bo'ladi.
     *
     * 3–4 NonCancellable: foydalanuvchi shu lahzada ekrandan chiqsa ham chiqish yarim yo'lda qolmasin.
     */
    override suspend fun logout() {
        realtimeClient.stop()
        outboxScheduler.cancel()
        withTimeoutOrNull(LOGOUT_TIMEOUT_MS) { safeApiCall { sessionApi.logout() } }
        withContext(NonCancellable) {
            clearLocalData()
            sessionStorage.clear()
        }
    }

    /** `clearAllTables()` bloklovchi chaqiruv — main thread'da chaqirib bo'lmaydi. */
    private suspend fun clearLocalData() = withContext(dispatchers.io) {
        database.clearAllTables()
    }

    /** Server har bir `deviceName` uchun alohida qurilma yozuvini saqlaydi. */
    private fun deviceName(): String =
        if (Build.MODEL.startsWith(Build.MANUFACTURER, ignoreCase = true)) Build.MODEL
        else "${Build.MANUFACTURER} ${Build.MODEL}"

    private companion object {
        const val LOGOUT_TIMEOUT_MS = 5_000L
    }
}
