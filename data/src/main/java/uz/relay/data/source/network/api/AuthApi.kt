package uz.relay.data.source.network.api

import retrofit2.http.Body
import retrofit2.http.POST
import uz.relay.data.model.request.OtpRequest
import uz.relay.data.model.request.RefreshTokenRequest
import uz.relay.data.model.request.VerifyOtpRequest
import uz.relay.data.model.response.TokenPairResponse

/**
 * Autentifikatsiya endpoint'lari (Retrofit interfeysi). Ular public — access token'siz chaqiriladi,
 * shuning uchun alohida "public" OkHttp klientiga ulanadi (NetworkModule): aks holda refresh so'rovining
 * o'zi TokenAuthenticator'ga tushib, cheksiz aylanishi mumkin edi.
 * Kim ishlatadi: AuthRepositoryImpl (OTP) va [uz.relay.data.source.network.interceptor.TokenRefresher] (refresh).
 */
interface AuthApi {

    /** Telefon raqamga OTP kod yuborishni so'raydi. */
    @POST("v1/auth/otp/request")
    suspend fun requestOtp(@Body request: OtpRequest)

    /** OTP'ni tekshiradi va yangi access/refresh token juftligini qaytaradi. */
    @POST("v1/auth/otp/verify")
    suspend fun verifyOtp(@Body request: VerifyOtpRequest): TokenPairResponse

    /** Refresh token bir martalik: har chaqiruvda YANGI juftlik keladi, eskisi bekor bo'ladi (rotatsiya). */
    @POST("v1/auth/refresh")
    suspend fun refresh(@Body request: RefreshTokenRequest): TokenPairResponse
}
