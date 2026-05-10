package com.campusconnect.app.ui.theme

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

// DataStore extension on Context — one instance per process
private val Context.themeDataStore by preferencesDataStore(name = "campus_connect_theme")

/**
 * App-wide singleton that owns the **user's manual** dark-mode preference.
 *
 * Design contract
 * ───────────────
 * • [isDark] reflects ONLY what the user explicitly toggled in the app.
 *   It does NOT know about the system setting.
 * • In MainActivity, the final value passed to CampusConnectTheme is:
 *       isDark = isSystemInDarkTheme() || AppThemeState.isDark.value
 * • The toggle in ProfileScreen / AdminProfileScreen is disabled while
 *   isSystemInDarkTheme() == true, so the two sources never conflict.
 *
 * Persistence
 * ───────────
 * The preference survives process death via Jetpack DataStore.
 * Call [init] once from MainActivity.onCreate before setContent {}.
 */
object AppThemeState {

    private val DARK_KEY = booleanPreferencesKey("manual_dark_mode")

    // Backing state — initialised to false; DataStore will update it on first collect
    private val _isDark = MutableStateFlow(false)

    /** Collect this in your composables with collectAsStateWithLifecycle(). */
    val isDark: StateFlow<Boolean> = _isDark.asStateFlow()

    private var appContext: Context? = null

    /**
     * Must be called once, before the first composition.
     * Safe to call multiple times (idempotent after the first call).
     */
    fun init(context: Context, scope: CoroutineScope) {
        if (appContext != null) return          // already initialised
        appContext = context.applicationContext

        // Read persisted value and keep _isDark in sync going forward
        scope.launch {
            context.applicationContext.themeDataStore.data
                .catch { /* corrupt DataStore — fall back to default (false) */ }
                .map { prefs -> prefs[DARK_KEY] ?: false }
                .collect { persisted -> _isDark.value = persisted }
        }
    }

    /**
     * Toggle dark mode on/off (called from the in-app switch).
     * The new value is persisted immediately.
     */
    fun setDark(enabled: Boolean) {
        val ctx = appContext ?: return
        _isDark.value = enabled                 // update in-memory state instantly
        // Persist asynchronously — fire-and-forget is fine here
        kotlinx.coroutines.GlobalScope.launch {
            ctx.themeDataStore.edit { prefs ->
                prefs[DARK_KEY] = enabled
            }
        }
    }

    /** Convenience shorthand. */
    fun toggle() = setDark(!_isDark.value)
}