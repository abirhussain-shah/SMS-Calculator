package com.spendsms.domain.repository

import com.spendsms.data.local.entity.BudgetEntity
import com.spendsms.data.local.entity.CategoryRuleEntity
import com.spendsms.data.local.entity.TransactionEntity
import com.spendsms.data.local.entity.UnparsedSmsEntity
import com.spendsms.data.local.model.CategorySpending
import com.spendsms.domain.model.BudgetAlert
import kotlinx.coroutines.flow.Flow
import java.time.YearMonth

interface TransactionRepository {

    fun getAllTransactions(): Flow<List<TransactionEntity>>

    fun getRecentTransactions(limit: Int = 5): Flow<List<TransactionEntity>>

    fun getTransactionById(id: Long): Flow<TransactionEntity?>

    fun getTransactionsInDateRange(startTime: Long, endTime: Long): Flow<List<TransactionEntity>>

    fun getTotalSpentInDateRange(startTime: Long, endTime: Long): Flow<Double>

    fun getTotalReceivedInDateRange(startTime: Long, endTime: Long): Flow<Double>

    fun getCategorySpendingInDateRange(startTime: Long, endTime: Long): Flow<List<CategorySpending>>

    fun searchTransactions(query: String): Flow<List<TransactionEntity>>

    suspend fun insertTransaction(transaction: TransactionEntity): Long

    suspend fun insertTransactions(transactions: List<TransactionEntity>): List<Long>

    suspend fun updateTransactionCategory(transactionId: Long, newCategory: String, rememberForMerchant: Boolean)

    suspend fun deleteTransaction(id: Long)

    suspend fun deleteAllData()

    suspend fun isTransactionDuplicate(smsId: Long, timestamp: Long): Boolean

    // Needs Review / Unparsed SMS
    fun getPendingReviewSms(): Flow<List<UnparsedSmsEntity>>

    fun getPendingReviewCount(): Flow<Int>

    suspend fun insertUnparsedSms(unparsedSms: UnparsedSmsEntity): Long

    suspend fun ignoreUnparsedSms(id: Long)

    suspend fun deleteUnparsedSms(id: Long)

    // Category Rules
    fun getAllCategoryRules(): Flow<List<CategoryRuleEntity>>

    suspend fun getCategoryRuleForMerchant(merchant: String): CategoryRuleEntity?

    suspend fun saveCategoryRule(merchant: String, category: String)

    suspend fun deleteCategoryRule(merchant: String)

    // Category Budgets & Overall Budget
    fun getAllBudgets(): Flow<List<BudgetEntity>>

    fun getBudgetForCategory(category: String): Flow<BudgetEntity?>

    suspend fun setBudget(category: String, monthlyLimit: Double)

    suspend fun deleteBudget(category: String)

    suspend fun checkBudgetAlerts(yearMonth: YearMonth): List<BudgetAlert>
}
