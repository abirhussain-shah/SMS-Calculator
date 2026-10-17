package com.spendsms.ui

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spendsms.R
import com.spendsms.data.datastore.ThemeMode
import com.spendsms.ui.navigation.MainAppNavHost
import com.spendsms.ui.navigation.Screen
import com.spendsms.ui.security.BiometricAuthManager
import com.spendsms.ui.security.BiometricLockScreen
import com.spendsms.ui.theme.SpendSMSTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            val uiState by mainViewModel.uiState.collectAsStateWithLifecycle()

            if (!uiState.isLoading) {
                val darkTheme = when (uiState.themeMode) {
                    ThemeMode.SYSTEM -> isSystemInDarkTheme()
                    ThemeMode.LIGHT -> false
                    ThemeMode.DARK -> true
                }

                SpendSMSTheme(darkTheme = darkTheme) {
                    var isUnlocked by rememberSaveable {
                        mutableStateOf(!uiState.isBiometricEnabled)
                    }

                    if (uiState.isBiometricEnabled && !isUnlocked) {
                        BiometricLockScreen(
                            onAuthenticateClick = {
                                BiometricAuthManager.showBiometricPrompt(
                                    activity = this@MainActivity,
                                    title = getString(R.string.biometric_prompt_title),
                                    subtitle = getString(R.string.biometric_prompt_subtitle),
                                    onSuccess = { isUnlocked = true }
                                )
                            }
                        )
                    } else {
                        val startDestination = if (uiState.isOnboardingCompleted) {
                            Screen.Home.route
                        } else {
                            Screen.Onboarding.route
                        }
                        MainAppNavHost(startDestination = startDestination)
                    }
                }
            }
        }
    }
}
