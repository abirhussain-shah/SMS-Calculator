package com.spendsms.data.local.model

import androidx.room.ColumnInfo

data class CategorySpending(
    @ColumnInfo(name = "category")
    val category: String,

    @ColumnInfo(name = "total_amount")
    val totalAmount: Double,

    @ColumnInfo(name = "transaction_count")
    val transactionCount: Int
)
