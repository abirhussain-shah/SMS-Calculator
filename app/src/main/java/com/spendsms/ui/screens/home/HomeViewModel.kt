package com.spendsms.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendsms.data.local.entity.TransactionEntity
import com.spendsms.data.local.model.CategorySpending
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
import java.time.YearMonth
import javax.inject.Inject

data class HomeUiState(
    val selectedMonth: YearMonth = YearMonth.now(),
    val totalSpent: Double = 0.0,
    val totalReceived: Double = 0.0,
    val netBalance: Double = 0.0,
    val categorySpendings: List<CategorySpending> = emptyList(),
    val recentTransactions: List<TransactionEntity> = emptyList(),
    val pendingReviewCount: Int = 0,
    val overallBudget: com.spendsms.data.local.entity.BudgetEntity? = null,
    val isLoading: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: TransactionRepository
) : ViewModel() {

    private val _selectedMonth = MutableStateFlow(YearMonth.now())
    val selectedMonth: StateFlow<YearMonth> = _selectedMonth.asStateFlow()

    val uiState: StateFlow<HomeUiState> = _selectedMonth.flatMapLatest { month ->
        val (start, end) = DateTimeFormatters.getStartAndEndOfMonth(month)

        combine(
            combine(
                repository.getTotalSpentInDateRange(start, end),
                repository.getTotalReceivedInDateRange(start, end),
                repository.getCategorySpendingInDateRange(start, end)
            ) { spent, received, categories -> Triple(spent, received, categories) },
            combine(
                repository.getTransactionsInDateRange(start, end),
                repository.getPendingReviewCount(),
                repository.getBudgetForCategory(com.spendsms.data.local.entity.BudgetEntity.OVERALL_CATEGORY)
            ) { transactions, reviewCount, budget -> Triple(transactions, reviewCount, budget) }
        ) { (spent, received, categories), (transactions, reviewCount, budget) ->
            HomeUiState(
                selectedMonth = month,
                totalSpent = spent,
                totalReceived = received,
                netBalance = received - spent,
                categorySpendings = categories,
                recentTransactions = transactions.take(5),
                pendingReviewCount = reviewCount,
                overallBudget = budget,
                isLoading = false
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )

    fun onPreviousMonth() {
        _selectedMonth.value = _selectedMonth.value.minusMonths(1)
    }

    fun onNextMonth() {
        _selectedMonth.value = _selectedMonth.value.plusMonths(1)
    }

    fun onResetToCurrentMonth() {
        _selectedMonth.value = YearMonth.now()
    }
}
