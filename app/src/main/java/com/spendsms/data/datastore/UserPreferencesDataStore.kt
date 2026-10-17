package com.spendsms.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_settings")

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

data class UserPreferences(
    val themeMode: ThemeMode,
    val isBiometricEnabled: Boolean,
    val isOnboardingCompleted: Boolean,
    val monitoredSenders: Set<String>,
    val lastSyncTimestamp: Long
)

@Singleton
class UserPreferencesDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private object PreferencesKeys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val MONITORED_SENDERS = stringSetPreferencesKey("monitored_senders")
        val LAST_SYNC_TIMESTAMP = longPreferencesKey("last_sync_timestamp")
    }

    companion object {
        val DEFAULT_SENDERS = setOf(
            "bKash",
            "16247",
            "NAGAD",
            "16167",
            "Rocket",
            "16216",
            "CityBank",
            "CITYBANK",
            "EBL",
            "BRAC BANK",
            "BRACBank",
            "SCB",
            "DBBL",
            "Upay",
            "UCB",
            "IslamiBank",
            "IBBL",
            "MTB"
        )
    }

    val userPreferencesFlow: Flow<UserPreferences> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val themeModeName = preferences[PreferencesKeys.THEME_MODE] ?: ThemeMode.SYSTEM.name
            val themeMode = runCatching { ThemeMode.valueOf(themeModeName) }.getOrDefault(ThemeMode.SYSTEM)
            val isBiometricEnabled = preferences[PreferencesKeys.BIOMETRIC_ENABLED] ?: false
            val isOnboardingCompleted = preferences[PreferencesKeys.ONBOARDING_COMPLETED] ?: false
            val monitoredSenders = preferences[PreferencesKeys.MONITORED_SENDERS] ?: DEFAULT_SENDERS
            val lastSyncTimestamp = preferences[PreferencesKeys.LAST_SYNC_TIMESTAMP] ?: 0L

            UserPreferences(
                themeMode = themeMode,
                isBiometricEnabled = isBiometricEnabled,
                isOnboardingCompleted = isOnboardingCompleted,
                monitoredSenders = monitoredSenders,
                lastSyncTimestamp = lastSyncTimestamp
            )
        }

    suspend fun setThemeMode(themeMode: ThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = themeMode.name
        }
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.BIOMETRIC_ENABLED] = enabled
        }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ONBOARDING_COMPLETED] = completed
        }
    }

    suspend fun setMonitoredSenders(senders: Set<String>) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.MONITORED_SENDERS] = senders
        }
    }

    suspend fun addMonitoredSender(sender: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[PreferencesKeys.MONITORED_SENDERS] ?: DEFAULT_SENDERS
            preferences[PreferencesKeys.MONITORED_SENDERS] = current + sender.trim()
        }
    }

    suspend fun removeMonitoredSender(sender: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[PreferencesKeys.MONITORED_SENDERS] ?: DEFAULT_SENDERS
            preferences[PreferencesKeys.MONITORED_SENDERS] = current - sender.trim()
        }
    }

    suspend fun setLastSyncTimestamp(timestamp: Long) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_SYNC_TIMESTAMP] = timestamp
        }
    }

    suspend fun clearAll() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
