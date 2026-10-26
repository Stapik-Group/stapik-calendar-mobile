package pl.stapik.calendar.data.cloud

import pl.stapik.calendar.data.config.ApiConfigStorage
import pl.stapik.common.cloud.CloudConfig
import pl.stapik.common.cloud.HttpCloudGateway
import pl.stapik.common.cloud.HttpTransport
import pl.stapik.common.sync.CloudGateway

class CloudGatewayProvider(
    private val configStorage: ApiConfigStorage,
    private val transport: HttpTransport
) {
    private var cached: Pair<CloudConfig, CloudGateway>? = null

    suspend fun current(): CloudGateway? = configStorage.load()?.let { gatewayFor(it) }

    @Synchronized
    private fun gatewayFor(config: CloudConfig): CloudGateway {
        cached?.let { (known, ready) -> if (known == config) return ready }
        return HttpCloudGateway(config, transport).also { cached = config to it }
    }
}
