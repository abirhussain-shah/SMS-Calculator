package com.spendsms.domain.repository

import com.spendsms.data.local.entity.RecurringPaymentEntity
import kotlinx.coroutines.flow.Flow

interface RecurringRepository {

    fun getActiveRecurringPayments(): Flow<List<RecurringPaymentEntity>>

    fun getPendingSuggestions(): Flow<List<RecurringPaymentEntity>>

    fun getAllRecurringPayments(): Flow<List<RecurringPaymentEntity>>

    suspend fun upsertRecurringPayment(recurringPayment: RecurringPaymentEntity): Long

    suspend fun confirmRecurring(id: Long)

    suspend fun dismissRecurring(id: Long)

    suspend fun deleteRecurring(id: Long)

    suspend fun runAutoDetection(): List<RecurringPaymentEntity>

    suspend fun checkUpcomingReminders(nowMillis: Long = System.currentTimeMillis()): List<RecurringPaymentEntity>

    suspend fun markReminded(id: Long, timestamp: Long)
}
