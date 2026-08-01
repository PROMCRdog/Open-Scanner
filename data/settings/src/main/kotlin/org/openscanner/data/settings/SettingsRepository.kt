package org.openscanner.data.settings

import kotlinx.coroutines.flow.Flow
import org.openscanner.core.model.AppPreferences

interface SettingsRepository {
    val preferences: Flow<AppPreferences>

    suspend fun setPrivacyMode(enabled: Boolean)

    suspend fun setRedactExports(enabled: Boolean)

    suspend fun setRefreshIntervalSeconds(seconds: Int)

    suspend fun reset()
}
