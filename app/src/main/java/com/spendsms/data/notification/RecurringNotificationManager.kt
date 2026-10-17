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
import com.spendsms.data.local.entity.RecurringPaymentEntity
import com.spendsms.ui.MainActivity
import com.spendsms.ui.util.DateTimeFormatters
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecurringNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    init {
        createNotificationChannel()
    }

    fun createNotificationChannel() {
        val channelName = try {
            context.getString(R.string.notif_channel_recurring_name)
        } catch (_: Exception) {
            "Recurring Payment Reminders"
        }
        val channelDesc = try {
            context.getString(R.string.notif_channel_recurring_desc)
        } catch (_: Exception) {
            "Reminders for upcoming bills, subscriptions, and recurring payments"
        }

        val channel = NotificationChannel(
            CHANNEL_ID,
            channelName,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = channelDesc
            enableVibration(true)
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.createNotificationChannel(channel)
    }

    fun showRecurringReminder(payment: RecurringPaymentEntity) {
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
            payment.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val amountStr = DateTimeFormatters.formatCurrency(payment.amount)
        val dueDateStr = DateTimeFormatters.formatDate(payment.nextExpectedDate)

        val title = try {
            context.getString(R.string.notif_recurring_title, payment.merchant)
        } catch (_: Exception) {
            "Upcoming Bill: ${payment.merchant}"
        }

        val body = try {
            context.getString(R.string.notif_recurring_body, payment.merchant, amountStr, dueDateStr)
        } catch (_: Exception) {
            "${payment.merchant} bill of $amountStr is due on $dueDateStr."
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_budget)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationId = ("recurring_" + payment.id).hashCode()
        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (_: SecurityException) {
            // Permission revoked at runtime
        }
    }

    companion object {
        const val CHANNEL_ID = "recurring_reminders"
    }
}
