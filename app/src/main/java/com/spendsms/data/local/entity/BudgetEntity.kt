package com.spendsms.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey
    val category: String,

    @ColumnInfo(name = "monthly_limit")
    val monthlyLimit: Double,

    @ColumnInfo(name = "alert_80_notified_month")
    val alert80NotifiedMonth: String? = null,

    @ColumnInfo(name = "alert_100_notified_month")
    val alert100NotifiedMonth: String? = null
) {
    companion object {
        const val OVERALL_CATEGORY = "overall"
    }
}
