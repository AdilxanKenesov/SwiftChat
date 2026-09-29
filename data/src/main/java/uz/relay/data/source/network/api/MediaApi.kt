package uz.relay.data.source.network.api

import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.HEAD
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import uz.relay.data.model.request.StartUploadRequest
import uz.relay.data.model.response.ChunkAckResponse
import uz.relay.data.model.response.StartUploadResponse

/**
 * Rezyumlanadigan (to'xtagan joyidan davom etadigan) yuklash. Yuklab olish bu yerda emas: u Retrofit'siz,
 * to'g'ridan-to'g'ri OkHttp oqimi bilan (MediaRepositoryImpl) — javob tanasi xotiraga to'liq o'qilmasin.
 * Kim ishlatadi: MediaUploader.
 */
interface MediaApi {

    /** Yuklash sessiyasini ochadi: server `uploadId`, `mediaId` va bo'lak (chunk) hajmini beradi. */
    @POST("v1/media/uploads")
    suspend fun startUpload(@Body request: StartUploadRequest): StartUploadResponse

    /** [offset] serverdagi `confirmedOffset` bilan teng bo'lishi shart, aks holda 409 OFFSET_MISMATCH. */
    @PUT("v1/media/uploads/{uploadId}")
    suspend fun uploadChunk(
        @Path("uploadId") uploadId: String,
        @Header("Upload-Offset") offset: Long,
        @Body chunk: RequestBody
    ): ChunkAckResponse

    /** Server qancha baytni qabul qilgan — javob tanasi yo'q, qiymat `Upload-Offset` sarlavhasida. */
    @HEAD("v1/media/uploads/{uploadId}")
    suspend fun uploadOffset(@Path("uploadId") uploadId: String): Response<Void>
}
