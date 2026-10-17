package com.spendsms.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "recurring_payments",
    indices = [
        Index(value = ["merchant"]),
        Index(value = ["next_expected_date"])
    ]
)
data class RecurringPaymentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "merchant")
    val merchant: String,

    @ColumnInfo(name = "amount")
    val amount: Double,

    @ColumnInfo(name = "category")
    val category: String,

    @ColumnInfo(name = "frequency")
    val frequency: String = FREQUENCY_MONTHLY, // "MONTHLY", "WEEKLY", "YEARLY"

    @ColumnInfo(name = "approximate_day")
    val approximateDay: Int = 1, // Day of month (1-31) or Day of week (1-7)

    @ColumnInfo(name = "last_payment_date")
    val lastPaymentDate: Long = 0L,

    @ColumnInfo(name = "next_expected_date")
    val nextExpectedDate: Long = 0L,

    @ColumnInfo(name = "is_dismissed")
    val isDismissed: Boolean = false,

    @ColumnInfo(name = "is_confirmed")
    val isConfirmed: Boolean = false,

    @ColumnInfo(name = "reminder_days_before")
    val reminderDaysBefore: Int = 2,

    @ColumnInfo(name = "last_reminded_date")
    val lastRemindedDate: Long? = null
) {
    companion object {
        const val FREQUENCY_MONTHLY = "MONTHLY"
        const val FREQUENCY_WEEKLY = "WEEKLY"
        const val FREQUENCY_YEARLY = "YEARLY"
    }
}
