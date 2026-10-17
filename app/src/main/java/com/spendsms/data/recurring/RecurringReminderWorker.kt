package com.spendsms.data.recurring

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.spendsms.data.notification.RecurringNotificationManager
import com.spendsms.domain.repository.RecurringRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

@HiltWorker
class RecurringReminderWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val recurringRepository: RecurringRepository,
    private val recurringNotificationManager: RecurringNotificationManager
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            // First run auto-detection to catch any newly formed recurring patterns
            recurringRepository.runAutoDetection()

            // Check for upcoming payments that require notifications
            val now = System.currentTimeMillis()
            val dueReminders = recurringRepository.checkUpcomingReminders(now)

            for (payment in dueReminders) {
                recurringNotificationManager.showRecurringReminder(payment)
                recurringRepository.markReminded(payment.id, now)
            }

            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "spendsms_recurring_reminder_worker"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .build()

            val workRequest = PeriodicWorkRequestBuilder<RecurringReminderWorker>(
                1, TimeUnit.DAYS
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }
    }
}
