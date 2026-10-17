package com.spendsms.ui.screens.rules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendsms.data.local.entity.CategoryRuleEntity
import com.spendsms.domain.repository.TransactionRepository
import com.spendsms.ui.components.CategoryVisuals
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CategoryRulesUiState(
    val rules: List<CategoryRuleEntity> = emptyList(),
    val filteredRules: List<CategoryRuleEntity> = emptyList(),
    val searchQuery: String = "",
    val isAddDialogOpen: Boolean = false,
    val addMerchant: String = "",
    val addCategory: String = CategoryVisuals.allCategories.first(),
    val ruleToDelete: CategoryRuleEntity? = null,
    val userMessage: String? = null,
    val isLoading: Boolean = false
)

@HiltViewModel
class CategoryRulesViewModel @Inject constructor(
    private val repository: TransactionRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _dialogState = MutableStateFlow(DialogState())

    val uiState: StateFlow<CategoryRulesUiState> = combine(
        repository.getAllCategoryRules(),
        _searchQuery,
        _dialogState
    ) { allRules, query, dialog ->
        val filtered = if (query.isBlank()) {
            allRules
        } else {
            val q = query.trim().lowercase()
            allRules.filter {
                it.merchant.lowercase().contains(q) || it.category.lowercase().contains(q)
            }
        }

        CategoryRulesUiState(
            rules = allRules,
            filteredRules = filtered,
            searchQuery = query,
            isAddDialogOpen = dialog.isAddDialogOpen,
            addMerchant = dialog.addMerchant,
            addCategory = dialog.addCategory,
            ruleToDelete = dialog.ruleToDelete,
            userMessage = dialog.userMessage,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CategoryRulesUiState()
    )

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun openAddDialog() {
        _dialogState.value = _dialogState.value.copy(
            isAddDialogOpen = true,
            addMerchant = "",
            addCategory = CategoryVisuals.allCategories.first()
        )
    }

    fun onAddMerchantChange(merchant: String) {
        _dialogState.value = _dialogState.value.copy(addMerchant = merchant)
    }

    fun onAddCategoryChange(category: String) {
        _dialogState.value = _dialogState.value.copy(addCategory = category)
    }

    fun saveRule() {
        val merchant = _dialogState.value.addMerchant.trim()
        if (merchant.isBlank()) {
            _dialogState.value = _dialogState.value.copy(userMessage = "invalid_input")
            return
        }

        val category = _dialogState.value.addCategory
        viewModelScope.launch {
            repository.saveCategoryRule(merchant, category)
            _dialogState.value = _dialogState.value.copy(
                isAddDialogOpen = false,
                userMessage = "rule_saved"
            )
        }
    }

    fun promptDelete(rule: CategoryRuleEntity) {
        _dialogState.value = _dialogState.value.copy(ruleToDelete = rule)
    }

    fun confirmDelete() {
        val toDelete = _dialogState.value.ruleToDelete ?: return
        viewModelScope.launch {
            repository.deleteCategoryRule(toDelete.merchant)
            _dialogState.value = _dialogState.value.copy(
                ruleToDelete = null,
                userMessage = "rule_deleted"
            )
        }
    }

    fun dismissDialog() {
        _dialogState.value = _dialogState.value.copy(
            isAddDialogOpen = false,
            ruleToDelete = null
        )
    }

    fun clearMessage() {
        _dialogState.value = _dialogState.value.copy(userMessage = null)
    }

    private data class DialogState(
        val isAddDialogOpen: Boolean = false,
        val addMerchant: String = "",
        val addCategory: String = CategoryVisuals.allCategories.first(),
        val ruleToDelete: CategoryRuleEntity? = null,
        val userMessage: String? = null
    )
}
