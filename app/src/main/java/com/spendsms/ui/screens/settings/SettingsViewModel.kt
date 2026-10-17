package com.spendsms.ui.screens.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.spendsms.data.datastore.ThemeMode
import com.spendsms.data.export.CsvExporter
import com.spendsms.data.sms.SmsImportWorker
import com.spendsms.domain.repository.SettingsRepository
import com.spendsms.domain.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.OutputStream
import javax.inject.Inject

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val isBiometricEnabled: Boolean = false,
    val monitoredSenders: Set<String> = emptySet(),
    val isAddSenderDialogOpen: Boolean = false,
    val isDeleteAllConfirmDialogOpen: Boolean = false,
    val isExporting: Boolean = false,
    val isReimporting: Boolean = false,
    val message: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val _isAddSenderDialogOpen = MutableStateFlow(false)
    val isAddSenderDialogOpen: StateFlow<Boolean> = _isAddSenderDialogOpen.asStateFlow()

    private val _isDeleteAllConfirmDialogOpen = MutableStateFlow(false)
    val isDeleteAllConfirmDialogOpen: StateFlow<Boolean> = _isDeleteAllConfirmDialogOpen.asStateFlow()

    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()

    private val _isReimporting = MutableStateFlow(false)
    val isReimporting: StateFlow<Boolean> = _isReimporting.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _actionsState = combine(
        _isAddSenderDialogOpen,
        _isDeleteAllConfirmDialogOpen,
        _isExporting,
        _isReimporting,
        _message
    ) { isAddOpen, isDeleteOpen, exporting, reimporting, msg ->
        ActionsState(isAddOpen, isDeleteOpen, exporting, reimporting, msg)
    }

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.userPreferences,
        _actionsState
    ) { prefs, actions ->
        SettingsUiState(
            themeMode = prefs.themeMode,
            isBiometricEnabled = prefs.isBiometricEnabled,
            monitoredSenders = prefs.monitoredSenders,
            isAddSenderDialogOpen = actions.isAddOpen,
            isDeleteAllConfirmDialogOpen = actions.isDeleteOpen,
            isExporting = actions.exporting,
            isReimporting = actions.reimporting,
            message = actions.msg
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    private data class ActionsState(
        val isAddOpen: Boolean,
        val isDeleteOpen: Boolean,
        val exporting: Boolean,
        val reimporting: Boolean,
        val msg: String?
    )

    fun onThemeModeChange(newMode: ThemeMode) {
        viewModelScope.launch {
            settingsRepository.setThemeMode(newMode)
        }
    }

    fun onBiometricToggle(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setBiometricEnabled(enabled)
        }
    }

    fun onOpenAddSenderDialog() {
        _isAddSenderDialogOpen.value = true
    }

    fun onDismissAddSenderDialog() {
        _isAddSenderDialogOpen.value = false
    }

    fun onConfirmAddSender(sender: String) {
        val trimmed = sender.trim()
        if (trimmed.isNotBlank()) {
            viewModelScope.launch {
                settingsRepository.addMonitoredSender(trimmed)
                _isAddSenderDialogOpen.value = false
                _message.value = "Sender added"
            }
        }
    }

    fun onRemoveSender(sender: String) {
        viewModelScope.launch {
            settingsRepository.removeMonitoredSender(sender)
            _message.value = "Sender removed"
        }
    }

    fun onOpenDeleteAllDialog() {
        _isDeleteAllConfirmDialogOpen.value = true
    }

    fun onDismissDeleteAllDialog() {
        _isDeleteAllConfirmDialogOpen.value = false
    }

    fun onConfirmDeleteAllData() {
        viewModelScope.launch {
            transactionRepository.deleteAllData()
            settingsRepository.clearAll()
            _isDeleteAllConfirmDialogOpen.value = false
            _message.value = "All data cleared"
        }
    }

    fun onExportCsv(outputStream: OutputStream) {
        viewModelScope.launch {
            _isExporting.value = true
            try {
                val transactions = transactionRepository.getAllTransactions().first()
                val count = CsvExporter.exportToStream(transactions, outputStream)
                _message.value = "Exported $count transactions"
            } catch (e: Exception) {
                _message.value = "CSV export failed: ${e.message}"
            } finally {
                _isExporting.value = false
            }
        }
    }

    fun onReimportSms() {
        viewModelScope.launch {
            _isReimporting.value = true
            try {
                // Reset sync timestamp so full history is imported
                settingsRepository.setLastSyncTimestamp(0L)

                val request = OneTimeWorkRequestBuilder<SmsImportWorker>().build()
                WorkManager.getInstance(context).enqueueUniqueWork(
                    "sms_manual_reimport",
                    ExistingWorkPolicy.REPLACE,
                    request
                )
                _message.value = "SMS re-sync scheduled in background"
            } catch (e: Exception) {
                _message.value = "Could not schedule SMS re-import"
            } finally {
                _isReimporting.value = false
            }
        }
    }

    fun clearMessage() {
        _message.value = null
    }
}
