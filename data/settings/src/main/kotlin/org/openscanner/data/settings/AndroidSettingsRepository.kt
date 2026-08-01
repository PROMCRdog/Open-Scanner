package org.openscanner.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import org.openscanner.core.model.AppPreferences
import org.openscanner.core.model.WifiRefreshIntervalPolicy

private val Context.openScannerDataStore by preferencesDataStore(name = "open_scanner_settings")

class AndroidSettingsRepository(
    context: Context,
) : SettingsRepository {
    private val dataStore = context.applicationContext.openScannerDataStore

    override val preferences: Flow<AppPreferences> = dataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { values ->
            AppPreferences(
                privacyMode = values[Keys.privacyMode] ?: false,
                redactExports = values[Keys.redactExports] ?: true,
                refreshIntervalSeconds = WifiRefreshIntervalPolicy.sanitize(
                    values[Keys.refreshIntervalSeconds] ?: WifiRefreshIntervalPolicy.DEFAULT_SECONDS,
                ),
            )
        }

    override suspend fun setPrivacyMode(enabled: Boolean) {
        dataStore.edit { it[Keys.privacyMode] = enabled }
    }

    override suspend fun setRedactExports(enabled: Boolean) {
        dataStore.edit { it[Keys.redactExports] = enabled }
    }

    override suspend fun setRefreshIntervalSeconds(seconds: Int) {
        dataStore.edit {
            it[Keys.refreshIntervalSeconds] = WifiRefreshIntervalPolicy.sanitize(seconds)
        }
    }

    override suspend fun reset() {
        dataStore.edit { it.clear() }
    }

    private object Keys {
        val privacyMode = booleanPreferencesKey("privacy_mode")
        val redactExports = booleanPreferencesKey("redact_exports")
        val refreshIntervalSeconds = intPreferencesKey("refresh_interval_seconds")
    }
}
