package pl.stapik.calendar.data.config

interface ApiConfigStorage {
    suspend fun load(): ApiConfig?
    suspend fun save(config: ApiConfig)
}