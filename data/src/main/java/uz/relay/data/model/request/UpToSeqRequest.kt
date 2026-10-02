package uz.relay.data.model.request

import kotlinx.serialization.Serializable

/** `read` / `received` kvitansiyalari uchun: "shu serverSeq gacha". */
@Serializable
data class UpToSeqRequest(
    val upToSeq: Long
)
