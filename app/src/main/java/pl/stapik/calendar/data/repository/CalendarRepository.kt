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
import pl.stapik.common.cloud.CloudConfig
import pl.stapik.common.cloud.HttpCloudGateway
import pl.stapik.common.cloud.HttpTransport
import pl.stapik.common.cloud.OkHttpTransport
import pl.stapik.common.model.SlotKey
import pl.stapik.common.sync.CloudError
import pl.stapik.common.sync.CloudException
import pl.stapik.common.sync.CloudGateway
import pl.stapik.common.sync.RemoteDocument

data class CalendarFetchResult(
    val entries: List<CalendarEntry>,
    val updatedAt: String,
    val scope: String
)

enum class SyncResolution { NONE, KEEP_LOCAL, USE_SERVER }

/** The server answered, but it has no calendar document in its slot. */
class CalendarNotFoundException : Exception("No calendar document on the server")

class CalendarRepository(
    private val apiConfigStorage: ApiConfigStorage,
    private val cacheStorage: CalendarCacheStorage,
    private val transport: HttpTransport = OkHttpTransport()
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val writeJson = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    // The gateway keeps the derived end-to-end key, which is slow to compute, so it is reused
    // for as long as the configuration stays the same.
    private var gateway: Pair<ApiConfig, CloudGateway>? = null

    @Synchronized
    private fun gatewayFor(config: ApiConfig): CloudGateway {
        gateway?.let { (known, ready) -> if (known == config) return ready }
        val created = HttpCloudGateway(CloudConfig(config.serverUrl, config.apiKey, config.passphrase), transport)
        gateway = config to created
        return created
    }

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
                scope = cached?.scope,
                modifiedAt = Instant.now().toString()
            )
        )
        sync(SyncResolution.NONE)
    }

    private suspend fun sync(resolution: SyncResolution): CalendarFetchOutcome {
        val cached = cacheStorage.load()
        val config = apiConfigStorage.load()
            ?: return CalendarFetchOutcome.LocalOnly(cached ?: CachedCalendar(entries = emptyList(), updatedAt = ""))

        val server = runCatching { download(config) }.getOrElse { error ->
            return if (error is SerializationException || error.isUnreadable() || cached == null) {
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
        val decided = resolution == SyncResolution.KEEP_LOCAL
        val syncedBefore = local.updatedAt.isNotEmpty()
        if (!canWrite || !(decided || syncedBefore)) {
            cacheStorage.save(localWithScope)
            return CalendarFetchOutcome.Conflict(localWithScope, server)
        }

        if (!decided && !localWins(local, server)) {
            cacheStorage.save(
                CachedCalendar(entries = server.entries, updatedAt = server.updatedAt, dirty = false, scope = server.scope)
            )
            return CalendarFetchOutcome.Fresh(server)
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
                CalendarFetchOutcome.Cached(localWithScope, error)
            }
        )
    }

    private fun localWins(local: CachedCalendar, server: CalendarFetchResult): Boolean {
        if (local.updatedAt == server.updatedAt) return true
        val modifiedAt = local.modifiedAt?.let { runCatching { Instant.parse(it) }.getOrNull() } ?: return true
        val serverUpdatedAt = runCatching { Instant.parse(server.updatedAt) }.getOrNull() ?: return true
        return modifiedAt.isAfter(serverUpdatedAt)
    }

    private fun Throwable.isUnreadable(): Boolean = (this as? CloudException)?.error is CloudError.Unreadable

    private suspend fun download(config: ApiConfig): CalendarFetchResult {
        val cloud = gatewayFor(config)
        val info = cloud.info()
        val document = cloud.read(SLOT) ?: throw CalendarNotFoundException()
        val envelope = json.decodeFromString<CalendarSyncEnvelope>(document.content)
        return CalendarFetchResult(
            entries = envelope.payload.entries,
            updatedAt = document.updatedAt,
            scope = info.scope.name
        )
    }

    private suspend fun write(config: ApiConfig, entries: List<CalendarEntry>, baseUpdatedAt: String): RemoteDocument {
        val now = Instant.now().truncatedTo(ChronoUnit.SECONDS).toString()
        val content = writeJson.encodeToString(
            CalendarSyncEnvelope(
                lastUpdate = now,
                payload = CalendarPayload(entries = entries, lastUpdate = now)
            )
        )
        return gatewayFor(config).write(SLOT, content, baseUpdatedAt)
    }

    private companion object {
        val SLOT = SlotKey("calendar.json")
        const val SCOPE_READ_WRITE = "READ_WRITE"
        val lock = Mutex()
    }
}
