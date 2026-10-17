package com.spendsms.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.spendsms.data.local.entity.CategoryRuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryRuleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateRule(rule: CategoryRuleEntity)

    @Query("SELECT * FROM category_rules WHERE LOWER(merchant) = LOWER(:merchant) LIMIT 1")
    suspend fun getRuleForMerchant(merchant: String): CategoryRuleEntity?

    @Query("SELECT * FROM category_rules ORDER BY updated_at DESC")
    fun getAllRules(): Flow<List<CategoryRuleEntity>>

    @Query("DELETE FROM category_rules WHERE LOWER(merchant) = LOWER(:merchant)")
    suspend fun deleteRule(merchant: String)

    @Query("DELETE FROM category_rules")
    suspend fun deleteAllRules()

    @Query("UPDATE transactions SET category = :category WHERE LOWER(merchant) = LOWER(:merchant) AND is_user_edited = 0")
    suspend fun updateTransactionsCategoryForMerchant(merchant: String, category: String): Int
}
