package pl.stapik.calendar.data.sync

import android.content.Context
import androidx.work.WorkerParameters
import pl.stapik.calendar.appContainer
import pl.stapik.common.sync.SyncOutcome
import pl.stapik.common.work.SyncWorker

class CalendarSyncWorker(context: Context, params: WorkerParameters) : SyncWorker(context, params) {
    override suspend fun runSync(): SyncOutcome = applicationContext.appContainer.calendarEngine.sync()
}
