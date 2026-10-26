package pl.stapik.calendar.data.config

import pl.stapik.common.cloud.CloudConfig

interface ApiConfigStorage {
    suspend fun load(): CloudConfig?
    suspend fun save(config: CloudConfig)
    suspend fun clear()
}