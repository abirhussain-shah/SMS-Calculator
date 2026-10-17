package com.spendsms.data.repository

import com.spendsms.data.datastore.ThemeMode
import com.spendsms.data.datastore.UserPreferences
import com.spendsms.data.datastore.UserPreferencesDataStore
import com.spendsms.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: UserPreferencesDataStore
) : SettingsRepository {

    override val userPreferences: Flow<UserPreferences> =
        dataStore.userPreferencesFlow

    override suspend fun setThemeMode(themeMode: ThemeMode) {
        dataStore.setThemeMode(themeMode)
    }

    override suspend fun setBiometricEnabled(enabled: Boolean) {
        dataStore.setBiometricEnabled(enabled)
    }

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.setOnboardingCompleted(completed)
    }

    override suspend fun setMonitoredSenders(senders: Set<String>) {
        dataStore.setMonitoredSenders(senders)
    }

    override suspend fun addMonitoredSender(sender: String) {
        dataStore.addMonitoredSender(sender)
    }

    override suspend fun removeMonitoredSender(sender: String) {
        dataStore.removeMonitoredSender(sender)
    }

    override suspend fun setLastSyncTimestamp(timestamp: Long) {
        dataStore.setLastSyncTimestamp(timestamp)
    }

    override suspend fun clearAll() {
        dataStore.clearAll()
    }
}
