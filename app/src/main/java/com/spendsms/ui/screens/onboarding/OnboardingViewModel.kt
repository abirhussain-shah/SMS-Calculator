package com.spendsms.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendsms.data.sms.SmsReader
import com.spendsms.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OnboardingUiState(
    val isImporting: Boolean = false,
    val importedCount: Int = 0,
    val isCompleted: Boolean = false,
    val showRationale: Boolean = false,
    val showSettingsPrompt: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val smsReader: SmsReader,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun onPermissionsGranted() {
        _uiState.update { it.copy(isImporting = true, showRationale = false, showSettingsPrompt = false) }
        viewModelScope.launch {
            try {
                val count = smsReader.importSmsInbox()
                settingsRepository.setOnboardingCompleted(true)
                _uiState.update {
                    it.copy(
                        isImporting = false,
                        importedCount = count,
                        isCompleted = true
                    )
                }
            } catch (e: Exception) {
                settingsRepository.setOnboardingCompleted(true)
                _uiState.update {
                    it.copy(
                        isImporting = false,
                        isCompleted = true,
                        errorMessage = e.localizedMessage
                    )
                }
            }
        }
    }

    fun onPermissionDenied(permanentlyDenied: Boolean) {
        if (permanentlyDenied) {
            _uiState.update { it.copy(showSettingsPrompt = true, showRationale = false) }
        } else {
            _uiState.update { it.copy(showRationale = true, showSettingsPrompt = false) }
        }
    }

    fun dismissRationale() {
        _uiState.update { it.copy(showRationale = false) }
    }

    fun dismissSettingsPrompt() {
        _uiState.update { it.copy(showSettingsPrompt = false) }
    }
}
