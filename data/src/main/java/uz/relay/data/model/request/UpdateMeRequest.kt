package uz.relay.data.model.request

import kotlinx.serialization.Serializable

@Serializable
data class UpdateMeRequest(
    val displayName: String? = null,
    val username: String? = null
)
