package pl.stapik.calendar.data.cache

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import pl.stapik.calendar.data.model.CalendarEntry

sealed interface LegacyCacheReading {
    data class Intact(val calendar: CachedCalendar) : LegacyCacheReading
    data class Salvaged(val calendar: CachedCalendar) : LegacyCacheReading
    data object Unreadable : LegacyCacheReading
}

@Serializable
internal data class CachedCalendarPayload(
    val entries: List<CalendarEntry>,
    val updatedAt: String,
    val dirty: Boolean = false,
    val scope: String? = null,
    val modifiedAt: String? = null
)

object LegacyCacheCodec {
    private const val ENTRIES = "entries"
    private const val UPDATED_AT = "updatedAt"
    private const val DIRTY = "dirty"
    private const val SCOPE = "scope"
    private const val MODIFIED_AT = "modifiedAt"

    private val json = Json { ignoreUnknownKeys = true }

    fun encode(calendar: CachedCalendar): String =
        json.encodeToString(
            CachedCalendarPayload(
                entries = calendar.entries,
                updatedAt = calendar.updatedAt,
                dirty = calendar.dirty,
                scope = calendar.scope,
                modifiedAt = calendar.modifiedAt
            )
        )

    fun read(raw: String): LegacyCacheReading =
        decodeIntact(raw)?.let { LegacyCacheReading.Intact(it) }
            ?: salvage(raw)?.let { LegacyCacheReading.Salvaged(it) }
            ?: LegacyCacheReading.Unreadable

    private fun decodeIntact(raw: String): CachedCalendar? =
        runCatching { json.decodeFromString<CachedCalendarPayload>(raw) }
            .getOrNull()
            ?.let {
                CachedCalendar(
                    entries = it.entries,
                    updatedAt = it.updatedAt,
                    dirty = it.dirty,
                    scope = it.scope,
                    modifiedAt = it.modifiedAt
                )
            }

    private fun salvage(raw: String): CachedCalendar? {
        val root = runCatching { json.parseToJsonElement(raw) }.getOrNull() as? JsonObject ?: return null
        val stored = root[ENTRIES] as? JsonArray ?: return null
        val kept = stored.mapNotNull { runCatching { json.decodeFromJsonElement(CalendarEntry.serializer(), it) }.getOrNull() }
        val lostSome = kept.size < stored.size
        return CachedCalendar(
            entries = kept,
            updatedAt = root.text(UPDATED_AT).orEmpty(),
            dirty = !lostSome && root.flag(DIRTY),
            scope = root.text(SCOPE),
            modifiedAt = root.text(MODIFIED_AT)
        )
    }

    private fun JsonObject.text(key: String): String? =
        (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content

    private fun JsonObject.flag(key: String): Boolean =
        (this[key] as? JsonPrimitive)?.booleanOrNull == true
}
