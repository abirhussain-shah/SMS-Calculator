package com.spendsms.domain.repository

import com.spendsms.data.datastore.ThemeMode
import com.spendsms.data.datastore.UserPreferences
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {

    val userPreferences: Flow<UserPreferences>

    suspend fun setThemeMode(themeMode: ThemeMode)

    suspend fun setBiometricEnabled(enabled: Boolean)

    suspend fun setOnboardingCompleted(completed: Boolean)

    suspend fun setMonitoredSenders(senders: Set<String>)

    suspend fun addMonitoredSender(sender: String)

    suspend fun removeMonitoredSender(sender: String)

    suspend fun setLastSyncTimestamp(timestamp: Long)

    suspend fun clearAll()
}
