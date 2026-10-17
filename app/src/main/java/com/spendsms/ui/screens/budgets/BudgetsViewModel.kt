package com.spendsms.ui.screens.budgets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendsms.data.local.entity.BudgetEntity
import com.spendsms.domain.repository.TransactionRepository
import com.spendsms.ui.components.CategoryVisuals
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
import java.time.YearMonth
import javax.inject.Inject

data class CategoryBudgetUiModel(
    val categoryId: String,
    val monthlyLimit: Double?,
    val spent: Double,
    val percentage: Int,
    val isOverBudget: Boolean,
    val isNearBudget: Boolean
)

data class BudgetsUiState(
    val selectedMonth: YearMonth = YearMonth.now(),
    val overallBudget: BudgetEntity? = null,
    val overallSpent: Double = 0.0,
    val categoryBudgets: List<CategoryBudgetUiModel> = emptyList(),
    val isEditing: Boolean = false,
    val editingCategory: String? = null,
    val editingLimitInput: String = "",
    val categoryToDelete: String? = null,
    val showDeleteConfirm: Boolean = false,
    val userMessage: String? = null,
    val isLoading: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class BudgetsViewModel @Inject constructor(
    private val repository: TransactionRepository
) : ViewModel() {

    private val _selectedMonth = MutableStateFlow(YearMonth.now())
    val selectedMonth: StateFlow<YearMonth> = _selectedMonth.asStateFlow()

    private val _dialogState = MutableStateFlow(
        DialogState(
            isEditing = false,
            editingCategory = null,
            editingLimitInput = "",
            categoryToDelete = null,
            showDeleteConfirm = false,
            userMessage = null
        )
    )

    private val expenseCategories = listOf(
        "food",
        "transport",
        "bills",
        "shopping",
        "recharge",
        "transfer",
        "other"
    )

    val uiState: StateFlow<BudgetsUiState> = combine(
        _selectedMonth.flatMapLatest { month ->
            val (start, end) = DateTimeFormatters.getStartAndEndOfMonth(month)
            combine(
                repository.getAllBudgets(),
                repository.getTotalSpentInDateRange(start, end),
                repository.getCategorySpendingInDateRange(start, end)
            ) { budgets, totalSpent, categorySpendings ->
                val overallBudget = budgets.find { it.category == BudgetEntity.OVERALL_CATEGORY }

                val spendingMap = categorySpendings.associate { it.category.lowercase() to it.totalAmount }
                val budgetMap = budgets.associate { it.category.lowercase() to it }

                val categoryModels = expenseCategories.map { categoryId ->
                    val budget = budgetMap[categoryId]
                    val limit = budget?.monthlyLimit
                    val spent = spendingMap[categoryId] ?: 0.0
                    val pct = if (limit != null && limit > 0) {
                        ((spent / limit) * 100).toInt()
                    } else 0

                    CategoryBudgetUiModel(
                        categoryId = categoryId,
                        monthlyLimit = limit,
                        spent = spent,
                        percentage = pct,
                        isOverBudget = limit != null && limit > 0 && spent >= limit,
                        isNearBudget = limit != null && limit > 0 && spent >= (limit * 0.8) && spent < limit
                    )
                }

                Triple(overallBudget, totalSpent, categoryModels)
            }
        },
        _selectedMonth,
        _dialogState
    ) { (overallBudget, totalSpent, categoryModels), month, dialog ->
        BudgetsUiState(
            selectedMonth = month,
            overallBudget = overallBudget,
            overallSpent = totalSpent,
            categoryBudgets = categoryModels,
            isEditing = dialog.isEditing,
            editingCategory = dialog.editingCategory,
            editingLimitInput = dialog.editingLimitInput,
            categoryToDelete = dialog.categoryToDelete,
            showDeleteConfirm = dialog.showDeleteConfirm,
            userMessage = dialog.userMessage,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = BudgetsUiState()
    )

    fun onPreviousMonth() {
        _selectedMonth.value = _selectedMonth.value.minusMonths(1)
    }

    fun onNextMonth() {
        _selectedMonth.value = _selectedMonth.value.plusMonths(1)
    }

    fun onResetCurrentMonth() {
        _selectedMonth.value = YearMonth.now()
    }

    fun openEditDialog(category: String, currentLimit: Double?) {
        _dialogState.value = _dialogState.value.copy(
            isEditing = true,
            editingCategory = category,
            editingLimitInput = currentLimit?.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() } ?: ""
        )
    }

    fun onLimitInputChange(newInput: String) {
        val filtered = newInput.filter { it.isDigit() || it == '.' }
        _dialogState.value = _dialogState.value.copy(editingLimitInput = filtered)
    }

    fun saveBudget() {
        val category = _dialogState.value.editingCategory ?: return
        val amount = _dialogState.value.editingLimitInput.toDoubleOrNull()
        if (amount == null || amount <= 0.0) {
            _dialogState.value = _dialogState.value.copy(userMessage = "invalid_amount")
            return
        }

        viewModelScope.launch {
            repository.setBudget(category, amount)
            _dialogState.value = _dialogState.value.copy(
                isEditing = false,
                editingCategory = null,
                editingLimitInput = "",
                userMessage = "budget_saved"
            )
        }
    }

    fun promptDeleteBudget(category: String) {
        _dialogState.value = _dialogState.value.copy(
            categoryToDelete = category,
            showDeleteConfirm = true
        )
    }

    fun confirmDeleteBudget() {
        val category = _dialogState.value.categoryToDelete ?: return
        viewModelScope.launch {
            repository.deleteBudget(category)
            _dialogState.value = _dialogState.value.copy(
                categoryToDelete = null,
                showDeleteConfirm = false,
                userMessage = "budget_deleted"
            )
        }
    }

    fun dismissDialog() {
        _dialogState.value = _dialogState.value.copy(
            isEditing = false,
            editingCategory = null,
            editingLimitInput = "",
            categoryToDelete = null,
            showDeleteConfirm = false
        )
    }

    fun clearUserMessage() {
        _dialogState.value = _dialogState.value.copy(userMessage = null)
    }

    private data class DialogState(
        val isEditing: Boolean,
        val editingCategory: String?,
        val editingLimitInput: String,
        val categoryToDelete: String?,
        val showDeleteConfirm: Boolean,
        val userMessage: String?
    )
}
