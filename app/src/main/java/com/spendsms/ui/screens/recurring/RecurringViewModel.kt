package com.spendsms.ui.screens.recurring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendsms.data.local.entity.RecurringPaymentEntity
import com.spendsms.domain.repository.RecurringRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

data class RecurringUiState(
    val activePayments: List<RecurringPaymentEntity> = emptyList(),
    val pendingSuggestions: List<RecurringPaymentEntity> = emptyList(),
    val isAddDialogOpen: Boolean = false,
    val addMerchant: String = "",
    val addAmount: String = "",
    val addCategory: String = "bills",
    val addFrequency: String = RecurringPaymentEntity.FREQUENCY_MONTHLY,
    val addApproximateDay: String = "1",
    val paymentToDelete: RecurringPaymentEntity? = null,
    val userMessage: String? = null,
    val isLoading: Boolean = false
)

@HiltViewModel
class RecurringViewModel @Inject constructor(
    private val recurringRepository: RecurringRepository
) : ViewModel() {

    private val _dialogState = MutableStateFlow(DialogState())

    init {
        // Trigger auto-detection on ViewModel init so latest SMS data is processed
        viewModelScope.launch {
            recurringRepository.runAutoDetection()
        }
    }

    val uiState: StateFlow<RecurringUiState> = combine(
        recurringRepository.getActiveRecurringPayments(),
        recurringRepository.getPendingSuggestions(),
        _dialogState
    ) { active, pending, dialog ->
        RecurringUiState(
            activePayments = active,
            pendingSuggestions = pending,
            isAddDialogOpen = dialog.isAddDialogOpen,
            addMerchant = dialog.addMerchant,
            addAmount = dialog.addAmount,
            addCategory = dialog.addCategory,
            addFrequency = dialog.addFrequency,
            addApproximateDay = dialog.addApproximateDay,
            paymentToDelete = dialog.paymentToDelete,
            userMessage = dialog.userMessage,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = RecurringUiState()
    )

    fun confirmSuggestion(id: Long) {
        viewModelScope.launch {
            recurringRepository.confirmRecurring(id)
            _dialogState.value = _dialogState.value.copy(userMessage = "recurring_confirmed")
        }
    }

    fun dismissSuggestion(id: Long) {
        viewModelScope.launch {
            recurringRepository.dismissRecurring(id)
            _dialogState.value = _dialogState.value.copy(userMessage = "recurring_dismissed")
        }
    }

    fun openAddDialog() {
        val today = LocalDate.now()
        _dialogState.value = _dialogState.value.copy(
            isAddDialogOpen = true,
            addMerchant = "",
            addAmount = "",
            addCategory = "bills",
            addFrequency = RecurringPaymentEntity.FREQUENCY_MONTHLY,
            addApproximateDay = today.dayOfMonth.toString()
        )
    }

    fun onMerchantChange(merchant: String) {
        _dialogState.value = _dialogState.value.copy(addMerchant = merchant)
    }

    fun onAmountChange(amount: String) {
        val filtered = amount.filter { it.isDigit() || it == '.' }
        _dialogState.value = _dialogState.value.copy(addAmount = filtered)
    }

    fun onCategoryChange(category: String) {
        _dialogState.value = _dialogState.value.copy(addCategory = category)
    }

    fun onFrequencyChange(frequency: String) {
        _dialogState.value = _dialogState.value.copy(addFrequency = frequency)
    }

    fun onDayChange(day: String) {
        val filtered = day.filter { it.isDigit() }.take(2)
        _dialogState.value = _dialogState.value.copy(addApproximateDay = filtered)
    }

    fun saveRecurringPayment() {
        val merchant = _dialogState.value.addMerchant.trim()
        val amount = _dialogState.value.addAmount.toDoubleOrNull()
        val day = _dialogState.value.addApproximateDay.toIntOrNull()?.coerceIn(1, 31) ?: 1

        if (merchant.isBlank() || amount == null || amount <= 0.0) {
            _dialogState.value = _dialogState.value.copy(userMessage = "invalid_input")
            return
        }

        val today = LocalDate.now()
        val targetMonth = if (day >= today.dayOfMonth) today else today.plusMonths(1)
        val safeDay = day.coerceAtMost(targetMonth.lengthOfMonth())
        val nextDueDate = targetMonth.withDayOfMonth(safeDay)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        viewModelScope.launch {
            val payment = RecurringPaymentEntity(
                merchant = merchant,
                amount = amount,
                category = _dialogState.value.addCategory,
                frequency = _dialogState.value.addFrequency,
                approximateDay = safeDay,
                lastPaymentDate = System.currentTimeMillis(),
                nextExpectedDate = nextDueDate,
                isDismissed = false,
                isConfirmed = true,
                reminderDaysBefore = 2
            )
            recurringRepository.upsertRecurringPayment(payment)
            _dialogState.value = _dialogState.value.copy(
                isAddDialogOpen = false,
                userMessage = "recurring_saved"
            )
        }
    }

    fun promptDelete(payment: RecurringPaymentEntity) {
        _dialogState.value = _dialogState.value.copy(paymentToDelete = payment)
    }

    fun confirmDelete() {
        val toDelete = _dialogState.value.paymentToDelete ?: return
        viewModelScope.launch {
            recurringRepository.deleteRecurring(toDelete.id)
            _dialogState.value = _dialogState.value.copy(
                paymentToDelete = null,
                userMessage = "recurring_deleted"
            )
        }
    }

    fun dismissDialog() {
        _dialogState.value = _dialogState.value.copy(
            isAddDialogOpen = false,
            paymentToDelete = null
        )
    }

    fun clearMessage() {
        _dialogState.value = _dialogState.value.copy(userMessage = null)
    }

    private data class DialogState(
        val isAddDialogOpen: Boolean = false,
        val addMerchant: String = "",
        val addAmount: String = "",
        val addCategory: String = "bills",
        val addFrequency: String = RecurringPaymentEntity.FREQUENCY_MONTHLY,
        val addApproximateDay: String = "1",
        val paymentToDelete: RecurringPaymentEntity? = null,
        val userMessage: String? = null
    )
}
