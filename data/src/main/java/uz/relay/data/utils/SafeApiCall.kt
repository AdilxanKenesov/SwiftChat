package uz.relay.data.utils

import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import uz.relay.core.common.result.AppError
import uz.relay.core.common.result.AppResult
import uz.relay.data.model.response.ErrorResponse
import java.io.IOException

/** Xato body'larini parse qilish uchun yengil Json: server yangi maydon qo'shsa ham yiqilmaydi. */
private val errorJson = Json { ignoreUnknownKeys = true }

/**
 * Retrofit chaqiruvini [AppResult] ga o'raydi — repository'lar exception o'rniga natija qaytaradi.
 *
 * - HttpException -> server'ning `{code, message, retryable}` body'si [AppError.Api] ga aylanadi;
 * - IOException -> [AppError.Network] (internet yo'q, timeout va h.k.);
 * - SerializationException -> [AppError.Unknown].
 * CancellationException qayta tashlanadi — coroutine bekor qilinishi hech qachon "xato" sifatida yutilib ketmasligi kerak.
 */
internal suspend fun <T> safeApiCall(block: suspend () -> T): AppResult<T> = try {
    AppResult.Success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: HttpException) {
    AppResult.Error(e.toAppError())
} catch (e: IOException) {
    AppResult.Error(AppError.Network)
} catch (e: SerializationException) {
    AppResult.Error(AppError.Unknown(e))
}

/** HTTP xatoni domain [AppError.Api] ga o'giradi; body bo'lmasa status koddan kelib chiqadi. */
private fun HttpException.toAppError(): AppError {
    val status = code()
    val body = response()?.errorBody()?.string()?.let { raw ->
        runCatching { errorJson.decodeFromString(ErrorResponse.serializer(), raw) }.getOrNull()
    }
    return AppError.Api(
        httpStatus = status,
        // JSON body yo'q (masalan, nginx'ning HTML sahifasi): kodni status'dan yasaymiz.
        code = body?.code ?: "HTTP_$status",
        message = body?.message ?: message(),
        // Server aytmagan bo'lsa: 5xx va 429 (rate limit) vaqtinchalik — qayta urinish mumkin.
        retryable = body?.retryable ?: (status >= 500 || status == 429),
        botUrl = body?.botUrl
    )
}
