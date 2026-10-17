package com.spendsms.data.repository

import com.spendsms.data.local.dao.BudgetDao
import com.spendsms.data.local.dao.CategoryRuleDao
import com.spendsms.data.local.dao.TransactionDao
import com.spendsms.data.local.dao.UnparsedSmsDao
import com.spendsms.data.local.entity.BudgetEntity
import com.spendsms.data.local.entity.CategoryRuleEntity
import com.spendsms.data.local.entity.TransactionEntity
import com.spendsms.data.local.entity.UnparsedSmsEntity
import com.spendsms.data.local.model.CategorySpending
import com.spendsms.domain.model.BudgetAlert
import com.spendsms.domain.repository.TransactionRepository
import com.spendsms.ui.util.DateTimeFormatters
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepositoryImpl @Inject constructor(
    private val transactionDao: TransactionDao,
    private val categoryRuleDao: CategoryRuleDao,
    private val unparsedSmsDao: UnparsedSmsDao,
    private val budgetDao: BudgetDao
) : TransactionRepository {

    override fun getAllTransactions(): Flow<List<TransactionEntity>> =
        transactionDao.getAllTransactions()

    override fun getRecentTransactions(limit: Int): Flow<List<TransactionEntity>> =
        transactionDao.getRecentTransactions(limit)

    override fun getTransactionById(id: Long): Flow<TransactionEntity?> =
        transactionDao.getTransactionById(id)

    override fun getTransactionsInDateRange(
        startTime: Long,
        endTime: Long
    ): Flow<List<TransactionEntity>> =
        transactionDao.getTransactionsInDateRange(startTime, endTime)

    override fun getTotalSpentInDateRange(startTime: Long, endTime: Long): Flow<Double> =
        transactionDao.getTotalSpentInDateRange(startTime, endTime)

    override fun getTotalReceivedInDateRange(startTime: Long, endTime: Long): Flow<Double> =
        transactionDao.getTotalReceivedInDateRange(startTime, endTime)

    override fun getCategorySpendingInDateRange(
        startTime: Long,
        endTime: Long
    ): Flow<List<CategorySpending>> =
        transactionDao.getCategorySpendingInDateRange(startTime, endTime)

    override fun searchTransactions(query: String): Flow<List<TransactionEntity>> =
        transactionDao.searchTransactions(query)

    override suspend fun insertTransaction(transaction: TransactionEntity): Long {
        val normalizedMerchant = transaction.merchant.trim().lowercase()
        val customRule = categoryRuleDao.getRuleForMerchant(normalizedMerchant)
        val finalTransaction = if (customRule != null && !transaction.isUserEdited) {
            transaction.copy(category = customRule.category)
        } else {
            transaction
        }
        return transactionDao.insertTransaction(finalTransaction)
    }

    override suspend fun insertTransactions(transactions: List<TransactionEntity>): List<Long> {
        val finalTransactions = transactions.map { tx ->
            val normalizedMerchant = tx.merchant.trim().lowercase()
            val customRule = categoryRuleDao.getRuleForMerchant(normalizedMerchant)
            if (customRule != null && !tx.isUserEdited) {
                tx.copy(category = customRule.category)
            } else {
                tx
            }
        }
        return transactionDao.insertTransactions(finalTransactions)
    }

    override suspend fun updateTransactionCategory(
        transactionId: Long,
        newCategory: String,
        rememberForMerchant: Boolean
    ) {
        val transaction = transactionDao.getTransactionById(transactionId).firstOrNull() ?: return

        transactionDao.updateCategory(
            transactionId = transactionId,
            newCategory = newCategory,
            isUserEdited = true
        )

        if (rememberForMerchant) {
            saveCategoryRule(
                merchant = transaction.merchant,
                category = newCategory
            )
        }
    }

    override suspend fun deleteTransaction(id: Long) {
        transactionDao.deleteTransactionById(id)
    }

    override suspend fun deleteAllData() {
        transactionDao.deleteAllTransactions()
        unparsedSmsDao.deleteAllUnparsedSms()
        categoryRuleDao.deleteAllRules()
        budgetDao.deleteAllBudgets()
    }

    override suspend fun isTransactionDuplicate(smsId: Long, timestamp: Long): Boolean {
        return transactionDao.exists(smsId, timestamp)
    }

    override fun getPendingReviewSms(): Flow<List<UnparsedSmsEntity>> =
        unparsedSmsDao.getPendingReviewSms()

    override fun getPendingReviewCount(): Flow<Int> =
        unparsedSmsDao.getPendingCount()

    override suspend fun insertUnparsedSms(unparsedSms: UnparsedSmsEntity): Long {
        return unparsedSmsDao.insertUnparsedSms(unparsedSms)
    }

    override suspend fun ignoreUnparsedSms(id: Long) {
        unparsedSmsDao.ignoreSms(id)
    }

    override suspend fun deleteUnparsedSms(id: Long) {
        unparsedSmsDao.deleteUnparsedSms(id)
    }

    override fun getAllCategoryRules(): Flow<List<CategoryRuleEntity>> =
        categoryRuleDao.getAllRules()

    override suspend fun getCategoryRuleForMerchant(merchant: String): CategoryRuleEntity? =
        categoryRuleDao.getRuleForMerchant(merchant.trim().lowercase())

    override suspend fun saveCategoryRule(merchant: String, category: String) {
        val trimmed = merchant.trim()
        categoryRuleDao.insertOrUpdateRule(
            CategoryRuleEntity(
                merchant = trimmed.lowercase(),
                category = category,
                updatedAt = System.currentTimeMillis()
            )
        )
        // Retroactively categorize unedited historical transactions matching this merchant
        categoryRuleDao.updateTransactionsCategoryForMerchant(trimmed, category)
    }

    override suspend fun deleteCategoryRule(merchant: String) {
        categoryRuleDao.deleteRule(merchant.trim())
    }

    override fun getAllBudgets(): Flow<List<BudgetEntity>> =
        budgetDao.getAllBudgets()

    override fun getBudgetForCategory(category: String): Flow<BudgetEntity?> =
        budgetDao.getBudgetForCategory(category)

    override suspend fun setBudget(category: String, monthlyLimit: Double) {
        val existing = budgetDao.getBudgetForCategoryDirect(category)
        val budget = existing?.copy(monthlyLimit = monthlyLimit)
            ?: BudgetEntity(category = category, monthlyLimit = monthlyLimit)
        budgetDao.upsertBudget(budget)
    }

    override suspend fun deleteBudget(category: String) {
        budgetDao.deleteBudget(category)
    }

    override suspend fun checkBudgetAlerts(yearMonth: YearMonth): List<BudgetAlert> {
        val (start, end) = DateTimeFormatters.getStartAndEndOfMonth(yearMonth)
        val monthString = yearMonth.toString()

        val budgets = budgetDao.getAllBudgets().firstOrNull() ?: emptyList()
        if (budgets.isEmpty()) return emptyList()

        val categorySpendings = transactionDao.getCategorySpendingInDateRange(start, end).firstOrNull() ?: emptyList()
        val totalSpentOverall = transactionDao.getTotalSpentInDateRange(start, end).firstOrNull() ?: 0.0

        val alerts = mutableListOf<BudgetAlert>()

        for (budget in budgets) {
            val spent = if (budget.category == BudgetEntity.OVERALL_CATEGORY) {
                totalSpentOverall
            } else {
                categorySpendings.find { it.category.equals(budget.category, ignoreCase = true) }?.totalAmount ?: 0.0
            }

            if (budget.monthlyLimit <= 0.0) continue

            if (spent >= budget.monthlyLimit) {
                if (budget.alert100NotifiedMonth != monthString) {
                    alerts.add(
                        BudgetAlert(
                            category = budget.category,
                            percentage = 100,
                            spent = spent,
                            limit = budget.monthlyLimit
                        )
                    )
                    budgetDao.updateAlert100Month(budget.category, monthString)
                }
            } else if (spent >= budget.monthlyLimit * 0.8) {
                if (budget.alert80NotifiedMonth != monthString) {
                    alerts.add(
                        BudgetAlert(
                            category = budget.category,
                            percentage = 80,
                            spent = spent,
                            limit = budget.monthlyLimit
                        )
                    )
                    budgetDao.updateAlert80Month(budget.category, monthString)
                }
            }
        }

        return alerts
    }
}
