package pl.stapik.calendar.data.repository

import kotlinx.serialization.json.Json
import pl.stapik.calendar.data.config.ApiConfigStorage
import pl.stapik.calendar.data.model.CalendarEntry
import pl.stapik.calendar.data.model.CalendarSyncEnvelope
import pl.stapik.calendar.data.network.NetworkModule

class MissingConfigException : Exception("API connection not configured")

data class CalendarFetchResult(
    val entries: List<CalendarEntry>,
    val updatedAt: String // ISO-8601
)

class CalendarRepository(private val apiConfigStorage: ApiConfigStorage) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun fetchEntries(): Result<CalendarFetchResult> = runCatching {
        val config = apiConfigStorage.load() ?: throw MissingConfigException()
        val api = NetworkModule.createApi(baseUrl = config.baseUrl)
        val document = api.getDocument(slotKey = SLOT_KEY, apiKey = config.apiKey)
        val syncEnvelope = json.decodeFromString<CalendarSyncEnvelope>(document.content)

        CalendarFetchResult(entries = syncEnvelope.payload, updatedAt = document.updatedAt)
    }

    private companion object {
        const val SLOT_KEY = "calendar.json"
    }
}