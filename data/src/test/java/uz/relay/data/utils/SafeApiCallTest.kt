package uz.relay.data.utils

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerializationException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import uz.relay.core.common.result.AppError
import uz.relay.core.common.result.AppResult
import java.io.IOException

/** Har qanday tarmoq xatosi exception emas, [AppResult.Error] bo'lib qaytishi va to'g'ri turga ajralishi. */
class SafeApiCallTest {

    private fun httpError(status: Int, body: String, type: String = "application/json") =
        HttpException(Response.error<Any>(status, body.toResponseBody(type.toMediaType())))

    @Test
    fun `success is wrapped`() = runTest {
        assertEquals(AppResult.Success(42), safeApiCall { 42 })
    }

    @Test
    fun `server error body becomes api error`() = runTest {
        val result = safeApiCall<Unit> {
            throw httpError(400, """{"code":"INVALID_OTP","message":"Kod noto'g'ri","retryable":false,"unknown":1}""")
        }
        val error = (result as AppResult.Error).error as AppError.Api
        assertEquals(400, error.httpStatus)
        assertEquals("INVALID_OTP", error.code)
        assertEquals("Kod noto'g'ri", error.message)
        assertEquals(false, error.retryable)
    }

    @Test
    fun `non json 5xx is retryable with code from status`() = runTest {
        val result = safeApiCall<Unit> { throw httpError(503, "<html>Bad gateway</html>", "text/html") }
        val error = (result as AppResult.Error).error as AppError.Api
        assertEquals("HTTP_503", error.code)
        assertTrue(error.retryable)
    }

    @Test
    fun `rate limit without body is retryable`() = runTest {
        val error = (safeApiCall<Unit> { throw httpError(429, "") } as AppResult.Error).error as AppError.Api
        assertTrue(error.retryable)
    }

    @Test
    fun `io failure is network error`() = runTest {
        assertEquals(AppResult.Error(AppError.Network), safeApiCall<Unit> { throw IOException("timeout") })
    }

    @Test
    fun `serialization failure is unknown error`() = runTest {
        val result = safeApiCall<Unit> { throw SerializationException("bad") }
        assertTrue((result as AppResult.Error).error is AppError.Unknown)
    }

    @Test(expected = CancellationException::class)
    fun `cancellation is never swallowed`() = runTest {
        safeApiCall<Unit> { throw CancellationException("cancelled") }
    }
}
