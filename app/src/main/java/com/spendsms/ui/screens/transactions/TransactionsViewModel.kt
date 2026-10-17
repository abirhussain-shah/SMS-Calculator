package com.spendsms.ui.screens.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendsms.data.local.entity.TransactionEntity
import com.spendsms.domain.model.TransactionType
import com.spendsms.domain.repository.TransactionRepository
import com.spendsms.ui.util.DateTimeFormatters
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class TransactionFilterType {
    ALL, DEBIT, CREDIT
}

data class TransactionsUiState(
    val searchQuery: String = "",
    val filterType: TransactionFilterType = TransactionFilterType.ALL,
    val selectedCategory: String? = null,
    val groupedTransactions: Map<String, List<TransactionEntity>> = emptyMap(),
    val totalCount: Int = 0,
    val selectedTransaction: TransactionEntity? = null,
    val isDetailSheetOpen: Boolean = false,
    val isLoading: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TransactionsViewModel @Inject constructor(
    private val repository: TransactionRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filterType = MutableStateFlow(TransactionFilterType.ALL)
    val filterType: StateFlow<TransactionFilterType> = _filterType.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    private val _selectedTransaction = MutableStateFlow<TransactionEntity?>(null)
    val selectedTransaction: StateFlow<TransactionEntity?> = _selectedTransaction.asStateFlow()

    private val _isDetailSheetOpen = MutableStateFlow(false)
    val isDetailSheetOpen: StateFlow<Boolean> = _isDetailSheetOpen.asStateFlow()

    val uiState: StateFlow<TransactionsUiState> = combine(
        _searchQuery,
        _filterType,
        _selectedCategory,
        _selectedTransaction,
        _isDetailSheetOpen
    ) { query, typeFilter, catFilter, selectedTx, isSheetOpen ->
        FilterParams(query, typeFilter, catFilter, selectedTx, isSheetOpen)
    }.flatMapLatest { params ->
        val flow = if (params.query.isBlank()) {
            repository.getAllTransactions()
        } else {
            repository.searchTransactions(params.query.trim())
        }

        flow.combine(MutableStateFlow(params)) { transactions, p ->
            val filtered = transactions.filter { tx ->
                val matchesType = when (p.typeFilter) {
                    TransactionFilterType.ALL -> true
                    TransactionFilterType.DEBIT -> tx.type == TransactionType.DEBIT
                    TransactionFilterType.CREDIT -> tx.type == TransactionType.CREDIT
                }

                val matchesCategory = p.catFilter == null || tx.category.equals(p.catFilter, ignoreCase = true)

                matchesType && matchesCategory
            }

            // Group by formatted date
            val grouped = filtered.groupBy { tx ->
                DateTimeFormatters.formatTransactionDate(tx.timestamp)
            }

            TransactionsUiState(
                searchQuery = p.query,
                filterType = p.typeFilter,
                selectedCategory = p.catFilter,
                groupedTransactions = grouped,
                totalCount = filtered.size,
                selectedTransaction = p.selectedTx,
                isDetailSheetOpen = p.isSheetOpen,
                isLoading = false
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TransactionsUiState()
    )

    fun onSearchQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun onFilterTypeChange(type: TransactionFilterType) {
        _filterType.value = type
    }

    fun onCategoryFilterChange(category: String?) {
        _selectedCategory.value = category
    }

    fun onSelectTransaction(transaction: TransactionEntity) {
        _selectedTransaction.value = transaction
        _isDetailSheetOpen.value = true
    }

    fun onDismissDetailSheet() {
        _isDetailSheetOpen.value = false
        _selectedTransaction.value = null
    }

    fun onUpdateCategory(transactionId: Long, newCategory: String, rememberForMerchant: Boolean) {
        viewModelScope.launch {
            repository.updateTransactionCategory(transactionId, newCategory, rememberForMerchant)
            _isDetailSheetOpen.value = false
            _selectedTransaction.value = null
        }
    }

    fun onDeleteTransaction(transactionId: Long) {
        viewModelScope.launch {
            repository.deleteTransaction(transactionId)
            _isDetailSheetOpen.value = false
            _selectedTransaction.value = null
        }
    }

    private data class FilterParams(
        val query: String,
        val typeFilter: TransactionFilterType,
        val catFilter: String?,
        val selectedTx: TransactionEntity?,
        val isSheetOpen: Boolean
    )
}
