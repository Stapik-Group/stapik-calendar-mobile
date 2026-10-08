package pl.stapik.calendar.ui.calendar

data class SyncConflict(
    val localCount: Int,
    val serverCount: Int,
    val serverUpdatedAt: String,
    val canKeepLocal: Boolean
)
