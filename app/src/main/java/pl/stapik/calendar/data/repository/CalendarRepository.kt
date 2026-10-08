package pl.stapik.calendar.data.repository

import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import pl.stapik.calendar.data.cache.CachedCalendar
import pl.stapik.calendar.data.cache.CalendarCacheStorage
import pl.stapik.calendar.data.config.ApiConfig
import pl.stapik.calendar.data.config.ApiConfigStorage
import pl.stapik.calendar.data.model.CalendarEntry
import pl.stapik.calendar.data.model.CalendarPayload
import pl.stapik.calendar.data.model.CalendarSyncEnvelope
import pl.stapik.calendar.data.model.DocumentResponse
import pl.stapik.calendar.data.model.DocumentWriteRequest
import pl.stapik.calendar.data.network.NetworkModule
import retrofit2.HttpException

data class CalendarFetchResult(
    val entries: List<CalendarEntry>,
    val updatedAt: String,
    val scope: String
)

enum class SyncResolution { NONE, KEEP_LOCAL, USE_SERVER }

class CalendarRepository(
    private val apiConfigStorage: ApiConfigStorage,
    private val cacheStorage: CalendarCacheStorage
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val writeJson = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val lock = Mutex()

    suspend fun fetchEntries(resolution: SyncResolution = SyncResolution.NONE): CalendarFetchOutcome =
        lock.withLock { sync(resolution) }

    suspend fun disconnect() = lock.withLock {
        apiConfigStorage.clear()
        cacheStorage.load()?.let { cacheStorage.save(it.copy(updatedAt = "", scope = null)) }
    }

    suspend fun saveEntries(entries: List<CalendarEntry>): CalendarFetchOutcome = lock.withLock {
        val cached = cacheStorage.load()
        cacheStorage.save(
            CachedCalendar(
                entries = entries,
                updatedAt = cached?.updatedAt.orEmpty(),
                dirty = true,
                scope = cached?.scope
            )
        )
        sync(SyncResolution.NONE)
    }

    private suspend fun sync(resolution: SyncResolution): CalendarFetchOutcome {
        val cached = cacheStorage.load()
        val config = apiConfigStorage.load()
            ?: return CalendarFetchOutcome.LocalOnly(cached ?: CachedCalendar(entries = emptyList(), updatedAt = ""))

        val server = runCatching { download(config) }.getOrElse { error ->
            return if (error is SerializationException || cached == null) {
                CalendarFetchOutcome.Failure(error)
            } else {
                CalendarFetchOutcome.Cached(cached, error)
            }
        }

        val local = cached?.takeIf { it.dirty }
        if (local == null || resolution == SyncResolution.USE_SERVER) {
            cacheStorage.save(
                CachedCalendar(entries = server.entries, updatedAt = server.updatedAt, dirty = false, scope = server.scope)
            )
            return CalendarFetchOutcome.Fresh(server)
        }

        val localWithScope = local.copy(scope = server.scope)
        val canWrite = server.scope == SCOPE_READ_WRITE
        val baseMatches = local.updatedAt.isNotEmpty() && local.updatedAt == server.updatedAt
        if (!canWrite || !(baseMatches || resolution == SyncResolution.KEEP_LOCAL)) {
            cacheStorage.save(localWithScope)
            return CalendarFetchOutcome.Conflict(localWithScope, server)
        }

        return runCatching { write(config, local.entries, server.updatedAt) }.fold(
            onSuccess = { document ->
                cacheStorage.save(
                    CachedCalendar(entries = local.entries, updatedAt = document.updatedAt, dirty = false, scope = server.scope)
                )
                CalendarFetchOutcome.Fresh(
                    CalendarFetchResult(entries = local.entries, updatedAt = document.updatedAt, scope = server.scope)
                )
            },
            onFailure = { error ->
                cacheStorage.save(localWithScope)
                if (error is HttpException && error.code() == HTTP_CONFLICT) {
                    CalendarFetchOutcome.Conflict(localWithScope, server)
                } else {
                    CalendarFetchOutcome.Cached(localWithScope, error)
                }
            }
        )
    }

    private suspend fun download(config: ApiConfig): CalendarFetchResult {
        val api = NetworkModule.createApi(baseUrl = config.baseUrl)
        val me = api.getMe(apiKey = config.apiKey)
        val document = api.getDocument(slotKey = SLOT_KEY, apiKey = config.apiKey)
        val envelope = json.decodeFromString<CalendarSyncEnvelope>(document.content)
        return CalendarFetchResult(
            entries = envelope.payload.entries,
            updatedAt = document.updatedAt,
            scope = me.scope
        )
    }

    private suspend fun write(config: ApiConfig, entries: List<CalendarEntry>, baseUpdatedAt: String): DocumentResponse {
        val now = Instant.now().truncatedTo(ChronoUnit.SECONDS).toString()
        val content = writeJson.encodeToString(
            CalendarSyncEnvelope(
                lastUpdate = now,
                payload = CalendarPayload(entries = entries, lastUpdate = now)
            )
        )
        return NetworkModule.createApi(baseUrl = config.baseUrl).writeDocument(
            slotKey = SLOT_KEY,
            apiKey = config.apiKey,
            body = DocumentWriteRequest(content = content, clientLastKnownUpdate = baseUpdatedAt)
        )
    }

    private companion object {
        const val SLOT_KEY = "calendar.json"
        const val SCOPE_READ_WRITE = "READ_WRITE"
        const val HTTP_CONFLICT = 409
    }
}
