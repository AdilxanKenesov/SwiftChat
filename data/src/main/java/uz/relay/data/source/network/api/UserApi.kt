package uz.relay.data.source.network.api

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Path
import uz.relay.data.model.request.UpdateMeRequest
import uz.relay.data.model.response.UserMeResponse
import uz.relay.data.model.response.UserResponse

interface UserApi {

    @GET("v1/users/me")
    suspend fun getMe(): UserMeResponse

    @PATCH("v1/users/me")
    suspend fun updateMe(@Body request: UpdateMeRequest): UserMeResponse

    /** Boshqa foydalanuvchining ochiq profili. */
    @GET("v1/users/{id}")
    suspend fun getUser(@Path("id") id: String): UserResponse
}
