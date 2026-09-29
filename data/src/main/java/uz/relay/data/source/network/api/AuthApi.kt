package uz.relay.data.source.network.api

import retrofit2.http.Body
import retrofit2.http.POST
import uz.relay.data.model.request.OtpRequest
import uz.relay.data.model.request.RefreshTokenRequest
import uz.relay.data.model.request.VerifyOtpRequest
import uz.relay.data.model.response.TokenPairResponse

/** Public endpoints: called without an access token. */
interface AuthApi {

    @POST("v1/auth/otp/request")
    suspend fun requestOtp(@Body request: OtpRequest)

    @POST("v1/auth/otp/verify")
    suspend fun verifyOtp(@Body request: VerifyOtpRequest): TokenPairResponse

    @POST("v1/auth/refresh")
    suspend fun refresh(@Body request: RefreshTokenRequest): TokenPairResponse
}
