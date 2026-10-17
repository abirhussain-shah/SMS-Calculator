package com.spendsms.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.spendsms.data.local.entity.TransactionEntity
import com.spendsms.data.local.model.CategorySpending
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTransactions(transactions: List<TransactionEntity>): List<Long>

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Query("UPDATE transactions SET category = :newCategory, is_user_edited = :isUserEdited WHERE id = :transactionId")
    suspend fun updateCategory(transactionId: Long, newCategory: String, isUserEdited: Boolean)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: Long)

    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions()

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    fun getTransactionById(id: Long): Flow<TransactionEntity?>

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentTransactions(limit: Int = 5): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    fun getTransactionsInDateRange(startTime: Long, endTime: Long): Flow<List<TransactionEntity>>

    @Query("""
        SELECT COALESCE(SUM(amount), 0.0) FROM transactions 
        WHERE type = 'DEBIT' AND timestamp BETWEEN :startTime AND :endTime
    """)
    fun getTotalSpentInDateRange(startTime: Long, endTime: Long): Flow<Double>

    @Query("""
        SELECT COALESCE(SUM(amount), 0.0) FROM transactions 
        WHERE type = 'CREDIT' AND timestamp BETWEEN :startTime AND :endTime
    """)
    fun getTotalReceivedInDateRange(startTime: Long, endTime: Long): Flow<Double>

    @Query("""
        SELECT category, SUM(amount) AS total_amount, COUNT(id) AS transaction_count 
        FROM transactions 
        WHERE type = 'DEBIT' AND timestamp BETWEEN :startTime AND :endTime 
        GROUP BY category 
        ORDER BY total_amount DESC
    """)
    fun getCategorySpendingInDateRange(startTime: Long, endTime: Long): Flow<List<CategorySpending>>

    @Query("""
        SELECT * FROM transactions 
        WHERE merchant LIKE '%' || :query || '%' 
           OR reference_id LIKE '%' || :query || '%' 
           OR raw_body LIKE '%' || :query || '%'
        ORDER BY timestamp DESC
    """)
    fun searchTransactions(query: String): Flow<List<TransactionEntity>>

    @Query("SELECT COUNT(*) > 0 FROM transactions WHERE sms_id = :smsId AND timestamp = :timestamp")
    suspend fun exists(smsId: Long, timestamp: Long): Boolean

    @Query("SELECT COUNT(*) FROM transactions")
    suspend fun getTransactionCount(): Int
}
