package pl.stapik.calendar.data.config

class InMemoryApiConfigStorage : ApiConfigStorage {
    private var stored: ApiConfig? = null
    override suspend fun load(): ApiConfig? = stored
    override suspend fun save(config: ApiConfig) { stored = config }
}