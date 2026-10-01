package uz.relay.data.model.response

import kotlinx.serialization.Serializable

/**
 * Stream Video token serveri javobi (`server/stream-token`). [userId] — server Relay'dan aniqlagan token egasi:
 * ilova uni o'zi kutgan foydalanuvchi bilan solishtiradi.
 */
@Serializable
data class StreamTokenResponse(
    val token: String,
    val userId: String,
    val expiresAt: Long
)
