package pl.stapik.calendar.data.cloud

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.stapik.common.model.CloudInfo
import pl.stapik.common.model.CloudScope
import pl.stapik.common.model.SlotKey
import pl.stapik.common.sync.CloudError
import pl.stapik.common.sync.CloudException
import pl.stapik.common.sync.CloudGateway
import pl.stapik.common.sync.RemoteDocument

class CalendarAccessTest {
    private class FakeGateway(var scope: CloudScope = CloudScope.READ_WRITE, var failure: CloudError? = null) : CloudGateway {
        override suspend fun info(): CloudInfo {
            failure?.let { throw CloudException(it) }
            return CloudInfo(scope, "cloud-1")
        }

        override suspend fun read(slot: SlotKey): RemoteDocument? = null

        override suspend fun write(slot: SlotKey, content: String, baseline: String): RemoteDocument =
            RemoteDocument(content, "2026-01-01T00:00:00Z")
    }

    @Test
    fun editingIsAllowedWithoutACloud() = runBlocking {
        assertTrue(CalendarAccess { null }.canEdit())
    }

    @Test
    fun editingFollowsTheScopeOfTheKey() = runBlocking {
        val gateway = FakeGateway(CloudScope.READ_ONLY)
        val access = CalendarAccess { gateway }

        assertFalse(access.canEdit())

        gateway.scope = CloudScope.READ_WRITE
        assertTrue(access.canEdit())
    }

    @Test
    fun anUnreachableCloudKeepsTheLastKnownAnswer() = runBlocking {
        val gateway = FakeGateway(CloudScope.READ_ONLY)
        val access = CalendarAccess { gateway }
        access.canEdit()

        gateway.failure = CloudError.Network()

        assertFalse(access.canEdit())
    }

    @Test
    fun anUnreachableCloudAllowsEditingUntilTheScopeIsKnown() = runBlocking {
        val access = CalendarAccess { FakeGateway(failure = CloudError.Network()) }

        assertTrue(access.canEdit())
    }
}
