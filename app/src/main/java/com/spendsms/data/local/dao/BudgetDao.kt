package com.spendsms.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.spendsms.data.local.entity.BudgetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {

    @Query("SELECT * FROM budgets")
    fun getAllBudgets(): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets WHERE category = :category")
    fun getBudgetForCategory(category: String): Flow<BudgetEntity?>

    @Query("SELECT * FROM budgets WHERE category = :category")
    suspend fun getBudgetForCategoryDirect(category: String): BudgetEntity?

    @Upsert
    suspend fun upsertBudget(budget: BudgetEntity)

    @Query("DELETE FROM budgets WHERE category = :category")
    suspend fun deleteBudget(category: String)

    @Query("UPDATE budgets SET alert_80_notified_month = :month WHERE category = :category")
    suspend fun updateAlert80Month(category: String, month: String)

    @Query("UPDATE budgets SET alert_100_notified_month = :month WHERE category = :category")
    suspend fun updateAlert100Month(category: String, month: String)

    @Query("DELETE FROM budgets")
    suspend fun deleteAllBudgets()
}
