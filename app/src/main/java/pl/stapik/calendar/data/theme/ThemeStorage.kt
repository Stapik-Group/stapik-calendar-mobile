package pl.stapik.calendar.data.theme

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import pl.stapik.calendar.ui.widget.CalendarWidget
import pl.stapik.common.theme.ThemeId

private val Context.themeDataStore by preferencesDataStore(name = "app_theme")

interface ThemeStorage {
    val theme: Flow<ThemeId>
    suspend fun setTheme(theme: ThemeId)
}

class DataStoreThemeStorage(private val context: Context) : ThemeStorage {
    override val theme: Flow<ThemeId> = context.themeDataStore.data.map { prefs ->
        // Versions up to 1.2.0 stored the enum name (CLASSIC_PINK); the shared library uses ids (classic-pink).
        ThemeId.fromId(prefs[KEY_THEME]?.lowercase()?.replace('_', '-'), default = ThemeId.CLASSIC)
    }

    override suspend fun setTheme(theme: ThemeId) {
        context.themeDataStore.edit { it[KEY_THEME] = theme.id }
        CalendarWidget().updateAll(context.applicationContext)
    }

    private companion object {
        val KEY_THEME = stringPreferencesKey("selected_theme")
    }
}
