package uz.relay.data.utils

import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import uz.relay.core.common.result.AppError
import uz.relay.core.common.result.AppResult
import uz.relay.data.model.response.ErrorResponse
import java.io.IOException

private val errorJson = Json { ignoreUnknownKeys = true }

/**
 * Wraps a Retrofit call into [AppResult]:
 * HttpException → the `{code, message, retryable}` body, IOException → [AppError.Network].
 * CancellationException is rethrown so coroutine cancellation is never swallowed as an error.
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

private fun HttpException.toAppError(): AppError {
    val status = code()
    val body = response()?.errorBody()?.string()?.let { raw ->
        runCatching { errorJson.decodeFromString(ErrorResponse.serializer(), raw) }.getOrNull()
    }
    return AppError.Api(
        httpStatus = status,
        // No JSON body (e.g. an nginx HTML page): derive a code from the status.
        code = body?.code ?: "HTTP_$status",
        message = body?.message ?: message(),
        retryable = body?.retryable ?: (status >= 500 || status == 429),
        botUrl = body?.botUrl
    )
}
