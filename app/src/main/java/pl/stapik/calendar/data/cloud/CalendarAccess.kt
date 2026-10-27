package pl.stapik.calendar.data.cloud

import kotlinx.coroutines.CancellationException
import pl.stapik.common.sync.CloudGateway

class CalendarAccess(private val gateway: suspend () -> CloudGateway?) {
    private var lastKnown = true

    suspend fun canEdit(): Boolean {
        val cloud = gateway()
        lastKnown = when {
            cloud == null -> true
            else -> scopeAllowsWriting(cloud) ?: lastKnown
        }
        return lastKnown
    }

    private suspend fun scopeAllowsWriting(cloud: CloudGateway): Boolean? =
        try {
            cloud.info().scope.canWrite
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
}
