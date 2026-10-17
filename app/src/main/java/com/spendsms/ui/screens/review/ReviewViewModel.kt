package com.spendsms.ui.screens.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendsms.data.local.entity.TransactionEntity
import com.spendsms.data.local.entity.UnparsedSmsEntity
import com.spendsms.domain.category.ExtractedSmsData
import com.spendsms.domain.category.SmartCategorizer
import com.spendsms.domain.model.TransactionType
import com.spendsms.domain.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReviewUiState(
    val pendingList: List<UnparsedSmsEntity> = emptyList(),
    val selectedIds: Set<Long> = emptySet(),
    val selectedForConversion: UnparsedSmsEntity? = null,
    val isConvertDialogOpen: Boolean = false,
    val extractedData: ExtractedSmsData? = null,
    val isLoading: Boolean = false
) {
    val isAllSelected: Boolean
        get() = pendingList.isNotEmpty() && selectedIds.size == pendingList.size

    val selectedCount: Int
        get() = selectedIds.size
}

@HiltViewModel
class ReviewViewModel @Inject constructor(
    private val repository: TransactionRepository
) : ViewModel() {

    private val _selectedIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedIds: StateFlow<Set<Long>> = _selectedIds.asStateFlow()

    private val _selectedForConversion = MutableStateFlow<UnparsedSmsEntity?>(null)
    val selectedForConversion: StateFlow<UnparsedSmsEntity?> = _selectedForConversion.asStateFlow()

    private val _isConvertDialogOpen = MutableStateFlow(false)
    val isConvertDialogOpen: StateFlow<Boolean> = _isConvertDialogOpen.asStateFlow()

    private val _extractedData = MutableStateFlow<ExtractedSmsData?>(null)

    val uiState: StateFlow<ReviewUiState> = combine(
        repository.getPendingReviewSms(),
        _selectedIds,
        _selectedForConversion,
        _isConvertDialogOpen,
        _extractedData
    ) { pending, selectedIds, selected, isDialogOpen, extracted ->
        val validSelectedIds = selectedIds.filter { id -> pending.any { it.id == id } }.toSet()
        ReviewUiState(
            pendingList = pending,
            selectedIds = validSelectedIds,
            selectedForConversion = selected,
            isConvertDialogOpen = isDialogOpen,
            extractedData = extracted,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ReviewUiState()
    )

    fun toggleSelectAll() {
        val currentPending = uiState.value.pendingList
        val currentSelected = _selectedIds.value
        if (currentPending.isNotEmpty() && currentSelected.size == currentPending.size) {
            _selectedIds.value = emptySet()
        } else {
            _selectedIds.value = currentPending.map { it.id }.toSet()
        }
    }

    fun toggleSelection(id: Long) {
        val current = _selectedIds.value.toMutableSet()
        if (current.contains(id)) {
            current.remove(id)
        } else {
            current.add(id)
        }
        _selectedIds.value = current
    }

    fun clearSelection() {
        _selectedIds.value = emptySet()
    }

    fun onIgnore(id: Long) {
        viewModelScope.launch {
            repository.ignoreUnparsedSms(id)
            if (_selectedIds.value.contains(id)) {
                _selectedIds.value = _selectedIds.value - id
            }
        }
    }

    fun onDelete(id: Long) {
        viewModelScope.launch {
            repository.deleteUnparsedSms(id)
            if (_selectedIds.value.contains(id)) {
                _selectedIds.value = _selectedIds.value - id
            }
        }
    }

    fun onIgnoreSelected() {
        val idsToIgnore = _selectedIds.value.toList()
        viewModelScope.launch {
            idsToIgnore.forEach { id ->
                repository.ignoreUnparsedSms(id)
            }
            _selectedIds.value = emptySet()
        }
    }

    fun onAutoConvertSelected() {
        val selectedItems = uiState.value.pendingList.filter { it.id in _selectedIds.value }
        viewModelScope.launch {
            for (sms in selectedItems) {
                val rule = repository.getCategoryRuleForMerchant(sms.sender)
                val extracted = SmartCategorizer.extractFromRawSms(
                    rawBody = sms.body,
                    sender = sms.sender,
                    existingRuleCategory = rule?.category
                )
                val amount = extracted.detectedAmount ?: 0.0
                val merchant = extracted.detectedMerchant?.takeIf { it.isNotBlank() } ?: sms.sender
                val category = extracted.suggestedCategory.categoryId.takeIf { it.isNotBlank() } ?: "other"
                val type = extracted.detectedType

                val transaction = TransactionEntity(
                    smsId = sms.smsId,
                    sender = sms.sender,
                    amount = amount,
                    type = type,
                    merchant = merchant,
                    category = category,
                    timestamp = sms.timestamp,
                    balance = null,
                    referenceId = null,
                    rawBody = sms.body,
                    isUserEdited = false
                )
                repository.insertTransaction(transaction)
                repository.deleteUnparsedSms(sms.id)
            }
            _selectedIds.value = emptySet()
        }
    }

    fun onStartConvert(sms: UnparsedSmsEntity) {
        viewModelScope.launch {
            val rule = repository.getCategoryRuleForMerchant(sms.sender)
            val extracted = SmartCategorizer.extractFromRawSms(
                rawBody = sms.body,
                sender = sms.sender,
                existingRuleCategory = rule?.category
            )
            _extractedData.value = extracted
            _selectedForConversion.value = sms
            _isConvertDialogOpen.value = true
        }
    }

    fun onDismissConvert() {
        _isConvertDialogOpen.value = false
        _selectedForConversion.value = null
        _extractedData.value = null
    }

    fun onConfirmConvert(
        amount: Double,
        type: TransactionType,
        merchant: String,
        category: String,
        rememberForMerchant: Boolean = true
    ) {
        val sms = _selectedForConversion.value ?: return

        viewModelScope.launch {
            val finalMerchant = merchant.trim().ifBlank { sms.sender }
            val transaction = TransactionEntity(
                smsId = sms.smsId,
                sender = sms.sender,
                amount = amount,
                type = type,
                merchant = finalMerchant,
                category = category,
                timestamp = sms.timestamp,
                balance = null,
                referenceId = null,
                rawBody = sms.body,
                isUserEdited = true
            )

            repository.insertTransaction(transaction)

            if (rememberForMerchant && finalMerchant.isNotBlank()) {
                repository.saveCategoryRule(finalMerchant, category)
            }

            repository.deleteUnparsedSms(sms.id)
            onDismissConvert()
        }
    }
}
