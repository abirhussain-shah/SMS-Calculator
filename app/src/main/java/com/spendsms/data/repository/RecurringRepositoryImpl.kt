package com.spendsms.data.repository

import com.spendsms.data.local.dao.RecurringPaymentDao
import com.spendsms.data.local.dao.TransactionDao
import com.spendsms.data.local.entity.RecurringPaymentEntity
import com.spendsms.domain.recurring.RecurringDetector
import com.spendsms.domain.repository.RecurringRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecurringRepositoryImpl @Inject constructor(
    private val recurringPaymentDao: RecurringPaymentDao,
    private val transactionDao: TransactionDao
) : RecurringRepository {

    override fun getActiveRecurringPayments(): Flow<List<RecurringPaymentEntity>> =
        recurringPaymentDao.getActiveRecurringPayments()

    override fun getPendingSuggestions(): Flow<List<RecurringPaymentEntity>> =
        recurringPaymentDao.getPendingSuggestions()

    override fun getAllRecurringPayments(): Flow<List<RecurringPaymentEntity>> =
        recurringPaymentDao.getAllRecurringPayments()

    override suspend fun upsertRecurringPayment(recurringPayment: RecurringPaymentEntity): Long =
        recurringPaymentDao.upsertRecurringPayment(recurringPayment)

    override suspend fun confirmRecurring(id: Long) =
        recurringPaymentDao.confirmRecurring(id)

    override suspend fun dismissRecurring(id: Long) =
        recurringPaymentDao.dismissRecurring(id)

    override suspend fun deleteRecurring(id: Long) =
        recurringPaymentDao.deleteRecurringPayment(id)

    override suspend fun runAutoDetection(): List<RecurringPaymentEntity> {
        val transactions = transactionDao.getAllTransactions().firstOrNull() ?: emptyList()
        val detected = RecurringDetector.detectRecurringPayments(transactions)

        val newlyInserted = mutableListOf<RecurringPaymentEntity>()

        for (candidate in detected) {
            val existing = recurringPaymentDao.getRecurringPaymentByMerchantDirect(candidate.merchant)
            if (existing == null) {
                val id = recurringPaymentDao.upsertRecurringPayment(candidate)
                newlyInserted.add(candidate.copy(id = id))
            } else {
                if (existing.isDismissed) continue
                // Update latest payment and next expected date if newer
                if (candidate.lastPaymentDate > existing.lastPaymentDate) {
                    recurringPaymentDao.updateExpectedDate(
                        id = existing.id,
                        nextDate = candidate.nextExpectedDate,
                        lastDate = candidate.lastPaymentDate
                    )
                }
            }
        }

        return newlyInserted
    }

    override suspend fun checkUpcomingReminders(nowMillis: Long): List<RecurringPaymentEntity> {
        val active = recurringPaymentDao.getAllActiveDirect()
        val dueReminders = mutableListOf<RecurringPaymentEntity>()
        val dayMillis = 24L * 60L * 60L * 1000L

        for (payment in active) {
            val reminderThreshold = payment.nextExpectedDate - (payment.reminderDaysBefore * dayMillis)
            if (nowMillis >= reminderThreshold) {
                // Ensure we don't notify multiple times in the same 24-hour cycle
                val lastReminded = payment.lastRemindedDate ?: 0L
                if (nowMillis - lastReminded >= dayMillis) {
                    dueReminders.add(payment)
                }
            }
        }

        return dueReminders
    }

    override suspend fun markReminded(id: Long, timestamp: Long) {
        recurringPaymentDao.updateLastReminded(id, timestamp)
    }
}
