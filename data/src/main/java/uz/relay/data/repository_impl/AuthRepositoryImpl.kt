package uz.relay.data.repository_impl

import android.os.Build
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import uz.relay.core.common.result.AppResult
import uz.relay.core.common.result.map
import uz.relay.core.common.result.onSuccess
import uz.relay.data.mapper.toSession
import uz.relay.data.model.request.OtpRequest
import uz.relay.data.model.request.VerifyOtpRequest
import uz.relay.data.source.local.SessionStorage
import uz.relay.data.source.network.api.AuthApi
import uz.relay.data.utils.safeApiCall
import uz.relay.domain.model.AuthState
import uz.relay.domain.repository.AuthRepository
import javax.inject.Inject

internal class AuthRepositoryImpl @Inject constructor(
    private val authApi: AuthApi,
    private val sessionStorage: SessionStorage
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
                // A new user fills in the profile first.
                sessionStorage.save(tokens.toSession(), profileSetupPending = tokens.isNewUser)
            }
            .map { it.isNewUser }

    override suspend fun completeProfileSetup() {
        sessionStorage.setProfileSetupPending(false)
    }

    /** The server keeps a device record per `deviceName`. */
    private fun deviceName(): String =
        if (Build.MODEL.startsWith(Build.MANUFACTURER, ignoreCase = true)) Build.MODEL
        else "${Build.MANUFACTURER} ${Build.MODEL}"
}
