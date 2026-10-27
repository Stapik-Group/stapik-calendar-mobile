package pl.stapik.calendar.data.config

import kotlinx.coroutines.flow.StateFlow
import pl.stapik.common.cloud.CloudConfig

interface ApiConfigStorage {
    val problem: StateFlow<ConnectionProblem?>

    suspend fun load(): CloudConfig?
    suspend fun save(config: CloudConfig)
    suspend fun clear()
}