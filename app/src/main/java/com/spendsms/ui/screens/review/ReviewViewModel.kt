package com.spendsms.ui.screens.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendsms.data.local.entity.TransactionEntity
import com.spendsms.data.local.entity.UnparsedSmsEntity
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

import com.spendsms.domain.category.ExtractedSmsData
import com.spendsms.domain.category.SmartCategorizer

data class ReviewUiState(
    val pendingList: List<UnparsedSmsEntity> = emptyList(),
    val selectedForConversion: UnparsedSmsEntity? = null,
    val isConvertDialogOpen: Boolean = false,
    val extractedData: ExtractedSmsData? = null,
    val isLoading: Boolean = false
)

@HiltViewModel
class ReviewViewModel @Inject constructor(
    private val repository: TransactionRepository
) : ViewModel() {

    private val _selectedForConversion = MutableStateFlow<UnparsedSmsEntity?>(null)
    val selectedForConversion: StateFlow<UnparsedSmsEntity?> = _selectedForConversion.asStateFlow()

    private val _isConvertDialogOpen = MutableStateFlow(false)
    val isConvertDialogOpen: StateFlow<Boolean> = _isConvertDialogOpen.asStateFlow()

    private val _extractedData = MutableStateFlow<ExtractedSmsData?>(null)

    val uiState: StateFlow<ReviewUiState> = combine(
        repository.getPendingReviewSms(),
        _selectedForConversion,
        _isConvertDialogOpen,
        _extractedData
    ) { pending, selected, isDialogOpen, extracted ->
        ReviewUiState(
            pendingList = pending,
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

    fun onIgnore(id: Long) {
        viewModelScope.launch {
            repository.ignoreUnparsedSms(id)
        }
    }

    fun onDelete(id: Long) {
        viewModelScope.launch {
            repository.deleteUnparsedSms(id)
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
