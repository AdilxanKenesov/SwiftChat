package uz.relay.data.source.network.api

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import uz.relay.data.model.request.AddMembersRequest
import uz.relay.data.model.request.ChangeRoleRequest
import uz.relay.data.model.request.ChatSettingsRequest
import uz.relay.data.model.request.CreateDirectRequest
import uz.relay.data.model.request.CreateGroupRequest
import uz.relay.data.model.request.UpdateChatRequest
import uz.relay.data.model.response.ChatListPageResponse
import uz.relay.data.model.response.ChatMemberResponse
import uz.relay.data.model.response.ChatResponse
import uz.relay.data.model.response.MembersResponse

interface ChatApi {

    /**
     * Oxirgi faollik bo'yicha, eng yangisi birinchi. `cursor` — oldingi javobning `nextCursor`i;
     * `null` bo'lsa Retrofit uni so'rovga umuman qo'shmaydi (birinchi sahifa).
     */
    @GET("v1/chats")
    suspend fun getChats(
        @Query("limit") limit: Int = MAX_PAGE_SIZE,
        @Query("cursor") cursor: String? = null
    ): ChatListPageResponse

    @GET("v1/chats/{id}")
    suspend fun getChat(@Path("id") id: String): ChatResponse

    /** Get-or-create: bu odam bilan chat bo'lsa 200, yangi yaratilsa 201 — ikkalasida ham chat qaytadi. */
    @POST("v1/chats/direct")
    suspend fun createDirect(@Body request: CreateDirectRequest): ChatResponse

    /** Har doim yangi guruh; chaqiruvchi OWNER bo'ladi. */
    @POST("v1/chats/group")
    suspend fun createGroup(@Body request: CreateGroupRequest): ChatResponse

    /** Guruh nomini o'zgartirish — faqat ADMIN/OWNER. */
    @PATCH("v1/chats/{id}")
    suspend fun updateChat(@Path("id") id: String, @Body request: UpdateChatRequest): ChatResponse

    /** Faqat MENING sozlamam (ovozsiz) — boshqa a'zolar buni ko'rmaydi. */
    @PUT("v1/chats/{id}/settings")
    suspend fun updateSettings(@Path("id") id: String, @Body request: ChatSettingsRequest): ChatResponse

    /**
     * A'zo qo'shish (ADMIN/OWNER); javob — yangilangan TO'LIQ a'zolar ro'yxati.
     *
     * API'da a'zolarni o'qiydigan GET yo'q. Bo'sh `userIds` bilan chaqirilsa hech kim qo'shilmaydi, lekin
     * ro'yxat qaytadi — ADMIN/OWNER uchun a'zolar "snapshot"i shu yo'l bilan olinadi. Oddiy a'zoga 403.
     */
    @POST("v1/chats/{id}/members")
    suspend fun addMembers(@Path("id") id: String, @Body request: AddMembersRequest): MembersResponse

    /** OWNER/ADMIN oddiy a'zoni chiqaradi. Javob: 204. */
    @DELETE("v1/chats/{id}/members/{userId}")
    suspend fun removeMember(@Path("id") id: String, @Path("userId") userId: String)

    /** Faqat OWNER: ADMIN yoki MEMBER qilish. */
    @PATCH("v1/chats/{id}/members/{userId}")
    suspend fun changeRole(
        @Path("id") id: String,
        @Path("userId") userId: String,
        @Body request: ChangeRoleRequest
    ): ChatMemberResponse

    /** Guruhdan chiqish. OWNER chiqsa, egalik eng eski ADMIN'ga (bo'lmasa eng eski a'zoga) o'tadi. Javob: 204. */
    @POST("v1/chats/{id}/leave")
    suspend fun leave(@Path("id") id: String)

    companion object {
        /** Server ruxsat bergan eng katta sahifa: kamroq so'rov — 300 so'rov/daqiqa limitini tejaydi. */
        const val MAX_PAGE_SIZE = 100
    }
}
