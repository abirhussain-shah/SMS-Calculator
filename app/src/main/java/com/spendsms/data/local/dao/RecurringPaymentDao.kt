package com.spendsms.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.spendsms.data.local.entity.RecurringPaymentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringPaymentDao {

    @Query("SELECT * FROM recurring_payments ORDER BY is_confirmed DESC, next_expected_date ASC")
    fun getAllRecurringPayments(): Flow<List<RecurringPaymentEntity>>

    @Query("SELECT * FROM recurring_payments WHERE is_dismissed = 0 AND is_confirmed = 1 ORDER BY next_expected_date ASC")
    fun getActiveRecurringPayments(): Flow<List<RecurringPaymentEntity>>

    @Query("SELECT * FROM recurring_payments WHERE is_dismissed = 0 AND is_confirmed = 0 ORDER BY next_expected_date ASC")
    fun getPendingSuggestions(): Flow<List<RecurringPaymentEntity>>

    @Query("SELECT * FROM recurring_payments WHERE LOWER(merchant) = LOWER(:merchant) LIMIT 1")
    fun getRecurringPaymentByMerchant(merchant: String): Flow<RecurringPaymentEntity?>

    @Query("SELECT * FROM recurring_payments WHERE LOWER(merchant) = LOWER(:merchant) LIMIT 1")
    suspend fun getRecurringPaymentByMerchantDirect(merchant: String): RecurringPaymentEntity?

    @Query("SELECT * FROM recurring_payments WHERE id = :id LIMIT 1")
    suspend fun getRecurringPaymentById(id: Long): RecurringPaymentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRecurringPayment(entity: RecurringPaymentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRecurringPayments(entities: List<RecurringPaymentEntity>): List<Long>

    @Update
    suspend fun updateRecurringPayment(entity: RecurringPaymentEntity)

    @Query("UPDATE recurring_payments SET is_confirmed = 1, is_dismissed = 0 WHERE id = :id")
    suspend fun confirmRecurring(id: Long)

    @Query("UPDATE recurring_payments SET is_dismissed = 1 WHERE id = :id")
    suspend fun dismissRecurring(id: Long)

    @Query("UPDATE recurring_payments SET last_reminded_date = :timestamp WHERE id = :id")
    suspend fun updateLastReminded(id: Long, timestamp: Long)

    @Query("UPDATE recurring_payments SET next_expected_date = :nextDate, last_payment_date = :lastDate WHERE id = :id")
    suspend fun updateExpectedDate(id: Long, nextDate: Long, lastDate: Long)

    @Query("DELETE FROM recurring_payments WHERE id = :id")
    suspend fun deleteRecurringPayment(id: Long)

    @Query("DELETE FROM recurring_payments")
    suspend fun deleteAllRecurringPayments()

    @Query("""
        SELECT * FROM recurring_payments 
        WHERE is_dismissed = 0 AND is_confirmed = 1 
          AND next_expected_date BETWEEN :startTime AND :endTime
        ORDER BY next_expected_date ASC
    """)
    suspend fun getUpcomingPaymentsDirect(startTime: Long, endTime: Long): List<RecurringPaymentEntity>

    @Query("SELECT * FROM recurring_payments WHERE is_dismissed = 0 AND is_confirmed = 1")
    suspend fun getAllActiveDirect(): List<RecurringPaymentEntity>
}
