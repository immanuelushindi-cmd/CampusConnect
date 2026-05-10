package com.campusconnect.app.ui.theme

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// One DataStore instance per app — the delegate ensures this
private val Context.themeDataStore: DataStore<Preferences>
        by preferencesDataStore(name = "theme_prefs")

/**
 * Thin wrapper around DataStore for persisting the dark mode preference.
 * Called from AppThemeState which is initialised in MainActivity.
 */
object ThemePreferences {
    private val DARK_MODE = booleanPreferencesKey("dark_mode")

    /** Emits the saved dark mode value (false by default on first install). */
    fun darkModeFlow(context: Context): Flow<Boolean> =
        context.themeDataStore.data.map { prefs -> prefs[DARK_MODE] ?: false }

    /** Persists a new dark mode value. Must be called from a coroutine. */
    suspend fun setDarkMode(context: Context, enabled: Boolean) {
        context.themeDataStore.edit { prefs -> prefs[DARK_MODE] = enabled }
    }
}
