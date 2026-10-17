package com.spendsms.ui

import com.spendsms.data.datastore.ThemeMode
import com.spendsms.data.datastore.UserPreferences
import com.spendsms.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeSettingsRepository : SettingsRepository {

    val preferencesFlow = MutableStateFlow(
        UserPreferences(
            themeMode = ThemeMode.SYSTEM,
            isBiometricEnabled = false,
            isOnboardingCompleted = true,
            monitoredSenders = setOf("bKash", "NAGAD", "Rocket", "CityBank"),
            lastSyncTimestamp = 0L
        )
    )

    override val userPreferences: Flow<UserPreferences> = preferencesFlow.asStateFlow()

    override suspend fun setThemeMode(themeMode: ThemeMode) {
        preferencesFlow.value = preferencesFlow.value.copy(themeMode = themeMode)
    }

    override suspend fun setBiometricEnabled(enabled: Boolean) {
        preferencesFlow.value = preferencesFlow.value.copy(isBiometricEnabled = enabled)
    }

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        preferencesFlow.value = preferencesFlow.value.copy(isOnboardingCompleted = completed)
    }

    override suspend fun setMonitoredSenders(senders: Set<String>) {
        preferencesFlow.value = preferencesFlow.value.copy(monitoredSenders = senders)
    }

    override suspend fun addMonitoredSender(sender: String) {
        preferencesFlow.value = preferencesFlow.value.copy(
            monitoredSenders = preferencesFlow.value.monitoredSenders + sender
        )
    }

    override suspend fun removeMonitoredSender(sender: String) {
        preferencesFlow.value = preferencesFlow.value.copy(
            monitoredSenders = preferencesFlow.value.monitoredSenders - sender
        )
    }

    override suspend fun setLastSyncTimestamp(timestamp: Long) {
        preferencesFlow.value = preferencesFlow.value.copy(lastSyncTimestamp = timestamp)
    }

    override suspend fun clearAll() {
        preferencesFlow.value = UserPreferences(
            themeMode = ThemeMode.SYSTEM,
            isBiometricEnabled = false,
            isOnboardingCompleted = false,
            monitoredSenders = setOf("bKash", "NAGAD"),
            lastSyncTimestamp = 0L
        )
    }
}
