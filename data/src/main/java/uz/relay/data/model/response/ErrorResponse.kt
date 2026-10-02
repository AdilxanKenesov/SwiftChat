package uz.relay.data.model.response

import kotlinx.serialization.Serializable

/**
 * Server xato body'si: `{code, message, retryable}`. [uz.relay.data.utils.safeApiCall] uni
 * domain `AppError.Api` ga aylantiradi. [retryable] — qayta urinishning ma'nosi bormi (UI/outbox uchun).
 * [botUrl] — Telegram OTP holatida foydalanuvchini botga yo'naltirish havolasi.
 */
@Serializable
data class ErrorResponse(
    val code: String,
    val message: String,
    val retryable: Boolean,
    val botUrl: String? = null
)
