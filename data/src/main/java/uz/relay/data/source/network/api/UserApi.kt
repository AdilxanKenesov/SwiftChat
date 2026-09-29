package uz.relay.data.source.network.api

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import uz.relay.data.model.request.UpdateMeRequest
import uz.relay.data.model.response.UserMeResponse

interface UserApi {

    @GET("v1/users/me")
    suspend fun getMe(): UserMeResponse

    @PATCH("v1/users/me")
    suspend fun updateMe(@Body request: UpdateMeRequest): UserMeResponse
}
