package uz.relay.data.source.network.api

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Path
import retrofit2.http.Query
import uz.relay.data.model.request.UpdateMeRequest
import uz.relay.data.model.response.UserMeResponse
import uz.relay.data.model.response.UserResponse
import uz.relay.data.model.response.UserSearchResponse

interface UserApi {

    @GET("v1/users/me")
    suspend fun getMe(): UserMeResponse

    @PATCH("v1/users/me")
    suspend fun updateMe(@Body request: UpdateMeRequest): UserMeResponse

    /** Boshqa foydalanuvchining ochiq profili. */
    @GET("v1/users/{id}")
    suspend fun getUser(@Path("id") id: String): UserResponse

    /** Username bo'yicha prefiks qidiruv (katta-kichik harf farqsiz). Ism va telefon bo'yicha qidirilmaydi. */
    @GET("v1/users/search")
    suspend fun search(@Query("q") query: String, @Query("limit") limit: Int = 20): UserSearchResponse
}
