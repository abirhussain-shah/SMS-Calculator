package com.spendsms.data.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.spendsms.R
import com.spendsms.data.local.entity.BudgetEntity
import com.spendsms.domain.model.BudgetAlert
import com.spendsms.domain.repository.TransactionRepository
import com.spendsms.ui.MainActivity
import com.spendsms.ui.util.DateTimeFormatters
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BudgetNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val transactionRepository: TransactionRepository
) {

    init {
        createNotificationChannel()
    }

    fun createNotificationChannel() {
        val channelName = try {
            context.getString(R.string.notif_channel_budgets_name)
        } catch (_: Exception) {
            "Budget Alerts"
        }
        val channelDesc = try {
            context.getString(R.string.notif_channel_budgets_desc)
        } catch (_: Exception) {
            "Notifications when approaching or exceeding category spending limits"
        }

        val channel = NotificationChannel(
            CHANNEL_ID,
            channelName,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = channelDesc
            enableVibration(true)
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.createNotificationChannel(channel)
    }

    suspend fun checkAndNotifyBudgets(yearMonth: YearMonth = YearMonth.now()): List<BudgetAlert> {
        val alerts = transactionRepository.checkBudgetAlerts(yearMonth)
        for (alert in alerts) {
            showBudgetAlert(alert)
        }
        return alerts
    }

    fun showBudgetAlert(alert: BudgetAlert) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionGranted = ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!permissionGranted) return
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            alert.category.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val categoryDisplayName = if (alert.category == BudgetEntity.OVERALL_CATEGORY) {
            context.getString(R.string.budgets_overall_title)
        } else {
            alert.category
        }

        val spentStr = DateTimeFormatters.formatCurrency(alert.spent)
        val limitStr = DateTimeFormatters.formatCurrency(alert.limit)

        val title = if (alert.percentage >= 100) {
            context.getString(R.string.notif_budget_100_title)
        } else {
            context.getString(R.string.notif_budget_80_title)
        }

        val body = if (alert.percentage >= 100) {
            context.getString(R.string.notif_budget_100_body, limitStr, categoryDisplayName, spentStr)
        } else {
            context.getString(R.string.notif_budget_80_body, spentStr, limitStr, categoryDisplayName)
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_budget)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationId = (alert.category + alert.percentage).hashCode()
        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (_: SecurityException) {
            // Permission check failed at runtime
        }
    }

    companion object {
        const val CHANNEL_ID = "budget_alerts"
    }
}
