package pl.stapik.calendar.data.theme

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import pl.stapik.calendar.ui.theme.AppTheme

private val Context.themeDataStore by preferencesDataStore(name = "app_theme")

interface ThemeStorage {
    val theme: Flow<AppTheme>
    suspend fun setTheme(theme: AppTheme)
}

class DataStoreThemeStorage(private val context: Context) : ThemeStorage {
    override val theme: Flow<AppTheme> = context.themeDataStore.data.map { prefs ->
        prefs[KEY_THEME]?.let { runCatching { AppTheme.valueOf(it) }.getOrNull() } ?: AppTheme.CLASSIC
    }

    override suspend fun setTheme(theme: AppTheme) {
        context.themeDataStore.edit { it[KEY_THEME] = theme.name }
    }

    private companion object {
        val KEY_THEME = stringPreferencesKey("selected_theme")
    }
}