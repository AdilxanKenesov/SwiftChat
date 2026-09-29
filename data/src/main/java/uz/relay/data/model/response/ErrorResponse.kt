package uz.relay.data.model.response

import kotlinx.serialization.Serializable

@Serializable
data class ErrorResponse(
    val code: String,
    val message: String,
    val retryable: Boolean,
    val botUrl: String? = null
)
