package pl.stapik.calendar.data.repository

import kotlinx.serialization.json.Json
import pl.stapik.calendar.data.config.ApiConfigStorage
import pl.stapik.calendar.data.model.CalendarEntry
import pl.stapik.calendar.data.model.CalendarSyncEnvelope
import pl.stapik.calendar.data.model.ReadResponse
import pl.stapik.calendar.data.network.NetworkModule

class MissingConfigException : Exception("API connection not configured")

class CalendarRepository(private val apiConfigStorage: ApiConfigStorage) {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun fetchEntries(): Result<List<CalendarEntry>> = runCatching {
        val config = apiConfigStorage.load() ?: throw MissingConfigException()
        val api = NetworkModule.createApi(baseUrl = config.baseUrl)

        val rawEnvelope = api.read(filename = FILENAME, apiKey = config.apiKey).string()
        val readResponse = json.decodeFromString<ReadResponse>(rawEnvelope)
        val syncEnvelope = json.decodeFromString<CalendarSyncEnvelope>(readResponse.content)

        syncEnvelope.payload
    }

    private companion object {
        const val FILENAME = "calendar.json"
    }
}