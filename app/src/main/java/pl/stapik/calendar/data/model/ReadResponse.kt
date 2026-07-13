package pl.stapik.calendar.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ReadResponse(val content: String)