package pl.stapik.calendar.data.model

import kotlinx.serialization.Serializable

@Serializable
data class DocumentWriteRequest(
    val content: String,
    val clientLastKnownUpdate: String
)
