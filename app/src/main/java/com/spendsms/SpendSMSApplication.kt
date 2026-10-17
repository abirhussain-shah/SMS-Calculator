package com.spendsms

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.spendsms.data.notification.BudgetNotificationManager
import com.spendsms.data.notification.RecurringNotificationManager
import com.spendsms.data.recurring.RecurringReminderWorker
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class SpendSMSApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var budgetNotificationManager: BudgetNotificationManager

    @Inject
    lateinit var recurringNotificationManager: RecurringNotificationManager

    override fun onCreate() {
        super.onCreate()
        budgetNotificationManager.createNotificationChannel()
        recurringNotificationManager.createNotificationChannel()
        RecurringReminderWorker.schedule(this)
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
