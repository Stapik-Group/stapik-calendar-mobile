package pl.stapik.calendar.data.notifications

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.notifiedEntriesDataStore by preferencesDataStore(name = "notified_entries")

interface NotifiedEntriesStorage {
    suspend fun load(): Set<String>
    suspend fun save(keys: Set<String>)
}

class DataStoreNotifiedEntriesStorage(private val context: Context) : NotifiedEntriesStorage {
    override suspend fun load(): Set<String> =
        context.notifiedEntriesDataStore.data.first()[KEY_NOTIFIED].orEmpty()

    override suspend fun save(keys: Set<String>) {
        context.notifiedEntriesDataStore.edit { it[KEY_NOTIFIED] = keys }
    }

    private companion object {
        val KEY_NOTIFIED = stringSetPreferencesKey("notified_entry_keys")
    }
}
