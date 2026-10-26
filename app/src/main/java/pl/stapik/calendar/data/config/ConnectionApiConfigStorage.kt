package pl.stapik.calendar.data.config

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import pl.stapik.common.cloud.CloudConfig
import pl.stapik.common.crypto.KeystoreCiphers
import pl.stapik.common.crypto.KeystoreKeys
import pl.stapik.common.settings.AesGcmLegacyDecryptor
import pl.stapik.common.settings.ConfigLoad
import pl.stapik.common.settings.ConnectionConfigStore
import pl.stapik.common.settings.LegacyFormat
import pl.stapik.common.settings.LegacyPrefsConfigReader
import pl.stapik.common.settings.LegacyPrefsSource

/** The DataStore of the pre-library versions; now only read once, to migrate the connection. */
private val Context.apiConfigDataStore by preferencesDataStore(name = "api_config")

private class DataStoreLegacySource(private val dataStore: DataStore<Preferences>) : LegacyPrefsSource {
    override fun readAll(): Map<String, String> = runBlocking {
        dataStore.data.first().asMap().entries
            .mapNotNull { (key, value) -> (value as? String)?.let { key.name to it } }
            .toMap()
    }

    override fun clear() = runBlocking { dataStore.edit { it.clear() }; Unit }
}

/**
 * Connection settings in the library's encrypted file. The first [load] without that file migrates
 * the configuration saved by earlier versions, so the user does not connect again.
 *
 * The store is not thread-safe and the app, the widget and the reminders all read it, so the app
 * keeps exactly one instance (see `AppContainer`).
 */
class ConnectionApiConfigStorage(context: Context) : ApiConfigStorage {
    /** Shared with the connect form's controller, so that both work on the same file. */
    val store = ConnectionConfigStore(
        directory = File(context.applicationContext.filesDir, "stapik").toPath(),
        cipher = KeystoreCiphers.create(KEY_ALIAS),
        schemaVersion = SCHEMA_VERSION,
        legacy = LegacyPrefsConfigReader(
            source = DataStoreLegacySource(context.applicationContext.apiConfigDataStore),
            format = LegacyFormat.CALENDAR_PLANNER,
            decryptor = AesGcmLegacyDecryptor { KeystoreKeys.find(LEGACY_KEY_ALIAS) },
        ),
    )

    private val lock = Mutex()
    private val mutableProblem = MutableStateFlow<ConnectionProblem?>(null)

    override val problem: StateFlow<ConnectionProblem?> = mutableProblem.asStateFlow()

    override suspend fun load(): CloudConfig? = lock.withLock {
        withContext(Dispatchers.IO) {
            val result = store.load()
            mutableProblem.value = problemOf(result)
            (result as? ConfigLoad.Loaded)?.config
        }
    }

    override suspend fun save(config: CloudConfig) {
        lock.withLock {
            withContext(Dispatchers.IO) { store.save(config) }
            mutableProblem.value = null
        }
    }

    override suspend fun clear() {
        lock.withLock {
            withContext(Dispatchers.IO) { store.clear() }
            mutableProblem.value = null
        }
    }

    private fun problemOf(result: ConfigLoad): ConnectionProblem? = when (result) {
        is ConfigLoad.Loaded, ConfigLoad.None -> null
        is ConfigLoad.SchemaReset -> ConnectionProblem.RESET
        is ConfigLoad.NewerSchema -> ConnectionProblem.NEWER_VERSION
        ConfigLoad.Unreadable -> ConnectionProblem.UNREADABLE
    }

    private companion object {
        const val SCHEMA_VERSION = 1
        const val KEY_ALIAS = "stapikcalendar_connection_key"
        const val LEGACY_KEY_ALIAS = "stapikcalendar_api_config_key"
    }
}
