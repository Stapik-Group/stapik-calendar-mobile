package pl.stapik.calendar.data

import java.time.Instant
import pl.stapik.calendar.data.model.CalendarDocument
import pl.stapik.calendar.data.model.CalendarEntry
import pl.stapik.calendar.data.model.CalendarPayload
import pl.stapik.common.model.CloudInfo
import pl.stapik.common.model.CloudScope
import pl.stapik.common.model.DocumentEnvelope
import pl.stapik.common.model.SlotKey
import pl.stapik.common.sync.CloudError
import pl.stapik.common.sync.CloudException
import pl.stapik.common.sync.CloudGateway
import pl.stapik.common.sync.PartitionInfo
import pl.stapik.common.sync.RemoteDocument

internal fun envelopeOf(vararg entries: CalendarEntry, stamp: String = "2026-01-01T00:00:00Z"): DocumentEnvelope =
    CalendarDocument.codec.encode(CalendarPayload(entries.toList(), stamp), DocumentEnvelope.empty(stamp), stamp)

internal class FakeCloud(
    var identity: String = "cloud-1",
    var scope: CloudScope = CloudScope.READ_WRITE
) : CloudGateway {
    val documents = mutableMapOf<SlotKey, RemoteDocument>()
    var failure: CloudError? = null
    private var stamp = Instant.parse("2026-02-01T00:00:00Z")

    fun put(slot: SlotKey, vararg entries: CalendarEntry) {
        stamp = stamp.plusSeconds(1)
        documents[slot] = RemoteDocument(envelopeOf(*entries).toJson(), stamp.toString())
    }

    override suspend fun info(): CloudInfo {
        failIfNeeded()
        return CloudInfo(scope, identity)
    }

    override suspend fun read(slot: SlotKey): RemoteDocument? {
        failIfNeeded()
        return documents[slot]
    }

    override suspend fun write(slot: SlotKey, content: String, baseline: String): RemoteDocument {
        failIfNeeded()
        if (documents[slot]?.updatedAt.orEmpty() != baseline) throw CloudException(CloudError.Conflict)
        stamp = stamp.plusSeconds(1)
        return RemoteDocument(content, stamp.toString()).also { documents[slot] = it }
    }

    override suspend fun partitions(slot: SlotKey): List<PartitionInfo> {
        failIfNeeded()
        return documents.entries
            .filter { it.key.value == slot.value && it.key.isPartition }
            .map { PartitionInfo(checkNotNull(it.key.partition), it.value.updatedAt) }
    }

    private fun failIfNeeded() {
        failure?.let { throw CloudException(it) }
    }
}
