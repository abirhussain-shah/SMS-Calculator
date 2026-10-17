package com.spendsms.ui

import com.spendsms.data.local.entity.CategoryRuleEntity
import com.spendsms.data.local.entity.TransactionEntity
import com.spendsms.data.local.entity.UnparsedSmsEntity
import com.spendsms.data.local.model.CategorySpending
import com.spendsms.domain.model.TransactionType
import com.spendsms.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

class FakeTransactionRepository : TransactionRepository {

    val transactionsFlow = MutableStateFlow<List<TransactionEntity>>(emptyList())
    val unparsedSmsFlow = MutableStateFlow<List<UnparsedSmsEntity>>(emptyList())
    val categoryRulesFlow = MutableStateFlow<List<CategoryRuleEntity>>(emptyList())

    override fun getAllTransactions(): Flow<List<TransactionEntity>> = transactionsFlow.asStateFlow()

    override fun getRecentTransactions(limit: Int): Flow<List<TransactionEntity>> {
        return transactionsFlow.map { list ->
            list.sortedByDescending { it.timestamp }.take(limit)
        }
    }

    override fun getTransactionById(id: Long): Flow<TransactionEntity?> {
        return transactionsFlow.map { list -> list.find { it.id == id } }
    }

    override fun getTransactionsInDateRange(startTime: Long, endTime: Long): Flow<List<TransactionEntity>> {
        return transactionsFlow.map { list ->
            list.filter { it.timestamp in startTime..endTime }
                .sortedByDescending { it.timestamp }
        }
    }

    override fun getTotalSpentInDateRange(startTime: Long, endTime: Long): Flow<Double> {
        return transactionsFlow.map { list ->
            list.filter { it.timestamp in startTime..endTime && it.type == TransactionType.DEBIT }
                .sumOf { it.amount }
        }
    }

    override fun getTotalReceivedInDateRange(startTime: Long, endTime: Long): Flow<Double> {
        return transactionsFlow.map { list ->
            list.filter { it.timestamp in startTime..endTime && it.type == TransactionType.CREDIT }
                .sumOf { it.amount }
        }
    }

    override fun getCategorySpendingInDateRange(startTime: Long, endTime: Long): Flow<List<CategorySpending>> {
        return transactionsFlow.map { list ->
            list.filter { it.timestamp in startTime..endTime && it.type == TransactionType.DEBIT }
                .groupBy { it.category }
                .map { (cat, items) ->
                    CategorySpending(
                        category = cat,
                        totalAmount = items.sumOf { it.amount },
                        transactionCount = items.size
                    )
                }
        }
    }

    override fun searchTransactions(query: String): Flow<List<TransactionEntity>> {
        return transactionsFlow.map { list ->
            list.filter {
                it.merchant.contains(query, ignoreCase = true) ||
                        it.rawBody.contains(query, ignoreCase = true) ||
                        (it.referenceId?.contains(query, ignoreCase = true) == true)
            }
        }
    }

    override suspend fun insertTransaction(transaction: TransactionEntity): Long {
        val nextId = (transactionsFlow.value.maxOfOrNull { it.id } ?: 0L) + 1
        val item = transaction.copy(id = nextId)
        transactionsFlow.value = transactionsFlow.value + item
        return nextId
    }

    override suspend fun insertTransactions(transactions: List<TransactionEntity>): List<Long> {
        return transactions.map { insertTransaction(it) }
    }

    override suspend fun updateTransactionCategory(
        transactionId: Long,
        newCategory: String,
        rememberForMerchant: Boolean
    ) {
        val updated = transactionsFlow.value.map { tx ->
            if (tx.id == transactionId) {
                if (rememberForMerchant) {
                    saveCategoryRule(tx.merchant, newCategory)
                }
                tx.copy(category = newCategory, isUserEdited = true)
            } else tx
        }
        transactionsFlow.value = updated
    }

    override suspend fun deleteTransaction(id: Long) {
        transactionsFlow.value = transactionsFlow.value.filterNot { it.id == id }
    }

    override suspend fun deleteAllData() {
        transactionsFlow.value = emptyList()
        unparsedSmsFlow.value = emptyList()
        categoryRulesFlow.value = emptyList()
    }

    override suspend fun isTransactionDuplicate(smsId: Long, timestamp: Long): Boolean {
        return transactionsFlow.value.any { it.smsId == smsId && it.timestamp == timestamp }
    }

    override fun getPendingReviewSms(): Flow<List<UnparsedSmsEntity>> {
        return unparsedSmsFlow.map { list -> list.filterNot { it.isIgnored } }
    }

    override fun getPendingReviewCount(): Flow<Int> {
        return unparsedSmsFlow.map { list -> list.count { !it.isIgnored } }
    }

    override suspend fun insertUnparsedSms(unparsedSms: UnparsedSmsEntity): Long {
        val nextId = (unparsedSmsFlow.value.maxOfOrNull { it.id } ?: 0L) + 1
        unparsedSmsFlow.value = unparsedSmsFlow.value + unparsedSms.copy(id = nextId)
        return nextId
    }

    override suspend fun ignoreUnparsedSms(id: Long) {
        unparsedSmsFlow.value = unparsedSmsFlow.value.map {
            if (it.id == id) it.copy(isIgnored = true) else it
        }
    }

    override suspend fun deleteUnparsedSms(id: Long) {
        unparsedSmsFlow.value = unparsedSmsFlow.value.filterNot { it.id == id }
    }

    override fun getAllCategoryRules(): Flow<List<CategoryRuleEntity>> = categoryRulesFlow.asStateFlow()

    override suspend fun getCategoryRuleForMerchant(merchant: String): CategoryRuleEntity? {
        return categoryRulesFlow.value.find { it.merchant.equals(merchant, ignoreCase = true) }
    }

    override suspend fun saveCategoryRule(merchant: String, category: String) {
        categoryRulesFlow.value = categoryRulesFlow.value.filterNot { it.merchant.equals(merchant, ignoreCase = true) } +
                CategoryRuleEntity(merchant = merchant, category = category)
    }

    override suspend fun deleteCategoryRule(merchant: String) {
        categoryRulesFlow.value = categoryRulesFlow.value.filterNot { it.merchant.equals(merchant, ignoreCase = true) }
    }

    val budgetsFlow = MutableStateFlow<List<com.spendsms.data.local.entity.BudgetEntity>>(emptyList())

    override fun getAllBudgets(): Flow<List<com.spendsms.data.local.entity.BudgetEntity>> = budgetsFlow.asStateFlow()

    override fun getBudgetForCategory(category: String): Flow<com.spendsms.data.local.entity.BudgetEntity?> {
        return budgetsFlow.map { list -> list.find { it.category.equals(category, ignoreCase = true) } }
    }

    override suspend fun setBudget(category: String, monthlyLimit: Double) {
        val current = budgetsFlow.value.filterNot { it.category.equals(category, ignoreCase = true) }
        budgetsFlow.value = current + com.spendsms.data.local.entity.BudgetEntity(
            category = category,
            monthlyLimit = monthlyLimit
        )
    }

    override suspend fun deleteBudget(category: String) {
        budgetsFlow.value = budgetsFlow.value.filterNot { it.category.equals(category, ignoreCase = true) }
    }

    override suspend fun checkBudgetAlerts(yearMonth: java.time.YearMonth): List<com.spendsms.domain.model.BudgetAlert> {
        val (start, end) = com.spendsms.ui.util.DateTimeFormatters.getStartAndEndOfMonth(yearMonth)
        val monthString = yearMonth.toString()
        val alerts = mutableListOf<com.spendsms.domain.model.BudgetAlert>()

        val currentBudgets = budgetsFlow.value
        val spentMap = transactionsFlow.value
            .filter { it.timestamp in start..end && it.type == TransactionType.DEBIT }
            .groupBy { it.category }
            .mapValues { (_, list) -> list.sumOf { it.amount } }
        val overallSpent = transactionsFlow.value
            .filter { it.timestamp in start..end && it.type == TransactionType.DEBIT }
            .sumOf { it.amount }

        val updatedBudgets = currentBudgets.map { budget ->
            val spent = if (budget.category == com.spendsms.data.local.entity.BudgetEntity.OVERALL_CATEGORY) {
                overallSpent
            } else {
                spentMap[budget.category] ?: 0.0
            }

            if (budget.monthlyLimit <= 0.0) return@map budget

            if (spent >= budget.monthlyLimit) {
                if (budget.alert100NotifiedMonth != monthString) {
                    alerts.add(
                        com.spendsms.domain.model.BudgetAlert(
                            category = budget.category,
                            percentage = 100,
                            spent = spent,
                            limit = budget.monthlyLimit
                        )
                    )
                    budget.copy(alert100NotifiedMonth = monthString)
                } else budget
            } else if (spent >= budget.monthlyLimit * 0.8) {
                if (budget.alert80NotifiedMonth != monthString) {
                    alerts.add(
                        com.spendsms.domain.model.BudgetAlert(
                            category = budget.category,
                            percentage = 80,
                            spent = spent,
                            limit = budget.monthlyLimit
                        )
                    )
                    budget.copy(alert80NotifiedMonth = monthString)
                } else budget
            } else budget
        }

        budgetsFlow.value = updatedBudgets
        return alerts
    }
}
