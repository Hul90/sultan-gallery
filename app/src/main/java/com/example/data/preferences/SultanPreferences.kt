package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.AppBackgroundStyle
import com.example.data.model.AppThemeMode
import com.example.data.model.GridMode
import com.example.data.model.FormatFilter
import com.example.data.model.SortOrder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "sultan_gallery_prefs")

class SultanPreferences(private val context: Context) {

    companion object {
        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        private val KEY_BACKGROUND_STYLE = stringPreferencesKey("background_style")
        private val KEY_DARK_THEME = booleanPreferencesKey("dark_theme")
        private val KEY_AMOLED_MODE = booleanPreferencesKey("amoled_mode")
        private val KEY_GRID_MODE = stringPreferencesKey("grid_mode")
        private val KEY_SORT_ORDER = stringPreferencesKey("sort_order")
        private val KEY_SHOW_AUDIO = booleanPreferencesKey("show_audio")
        private val KEY_FORMAT_FILTER = stringPreferencesKey("format_filter")
        private val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        private val KEY_VAULT_PIN = stringPreferencesKey("vault_pin")
        private val KEY_PLAYBACK_SPEED = floatPreferencesKey("playback_speed")
    }

    val themeMode: Flow<AppThemeMode> = context.dataStore.data.map { prefs ->
        val name = prefs[KEY_THEME_MODE] ?: AppThemeMode.SULTAN_GOLD.name
        runCatching { AppThemeMode.valueOf(name) }.getOrDefault(AppThemeMode.SULTAN_GOLD)
    }

    val backgroundStyle: Flow<AppBackgroundStyle> = context.dataStore.data.map { prefs ->
        val name = prefs[KEY_BACKGROUND_STYLE] ?: AppBackgroundStyle.AMBIENT_GLOW.name
        runCatching { AppBackgroundStyle.valueOf(name) }.getOrDefault(AppBackgroundStyle.AMBIENT_GLOW)
    }

    val isDarkTheme: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_DARK_THEME] ?: true
    }

    val isAmoledMode: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_AMOLED_MODE] ?: false
    }

    val gridMode: Flow<GridMode> = context.dataStore.data.map { prefs ->
        val name = prefs[KEY_GRID_MODE] ?: GridMode.NORMAL.name
        try {
            GridMode.valueOf(name)
        } catch (_: Exception) {
            GridMode.NORMAL
        }
    }

    val sortOrder: Flow<SortOrder> = context.dataStore.data.map { prefs ->
        val name = prefs[KEY_SORT_ORDER] ?: SortOrder.DATE_DESC.name
        try {
            SortOrder.valueOf(name)
        } catch (_: Exception) {
            SortOrder.DATE_DESC
        }
    }

    val showAudio: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_SHOW_AUDIO] ?: true
    }

    val formatFilter: Flow<FormatFilter> = context.dataStore.data.map { prefs ->
        runCatching { FormatFilter.valueOf(prefs[KEY_FORMAT_FILTER] ?: FormatFilter.ALL.name) }
            .getOrDefault(FormatFilter.ALL)
    }

    val isOnboardingCompleted: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_ONBOARDING_COMPLETED] ?: false
    }

    val vaultPin: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_VAULT_PIN] ?: ""
    }

    val playbackSpeed: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[KEY_PLAYBACK_SPEED] ?: 1.0f
    }

    suspend fun setThemeMode(mode: AppThemeMode) {
        context.dataStore.edit { it[KEY_THEME_MODE] = mode.name }
    }

    suspend fun setBackgroundStyle(style: AppBackgroundStyle) {
        context.dataStore.edit { it[KEY_BACKGROUND_STYLE] = style.name }
    }

    suspend fun setDarkTheme(enabled: Boolean) {
        context.dataStore.edit { it[KEY_DARK_THEME] = enabled }
    }

    suspend fun setAmoledMode(enabled: Boolean) {
        context.dataStore.edit { it[KEY_AMOLED_MODE] = enabled }
    }

    suspend fun setGridMode(mode: GridMode) {
        context.dataStore.edit { it[KEY_GRID_MODE] = mode.name }
    }

    suspend fun setSortOrder(order: SortOrder) {
        context.dataStore.edit { it[KEY_SORT_ORDER] = order.name }
    }

    suspend fun setShowAudio(enabled: Boolean) {
        context.dataStore.edit { it[KEY_SHOW_AUDIO] = enabled }
    }

    suspend fun setFormatFilter(filter: FormatFilter) {
        context.dataStore.edit { it[KEY_FORMAT_FILTER] = filter.name }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { it[KEY_ONBOARDING_COMPLETED] = completed }
    }

    suspend fun setVaultPin(rawPin: String) {
        val hashed = com.example.data.vault.SultanVaultCryptoEngine.hashPin(rawPin)
        context.dataStore.edit { it[KEY_VAULT_PIN] = hashed }
    }

    suspend fun setPlaybackSpeed(speed: Float) {
        context.dataStore.edit { it[KEY_PLAYBACK_SPEED] = speed }
    }
}
