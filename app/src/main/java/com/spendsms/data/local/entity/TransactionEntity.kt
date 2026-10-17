package com.spendsms.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.spendsms.domain.model.TransactionType

@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["sms_id", "timestamp"], unique = true),
        Index(value = ["timestamp"]),
        Index(value = ["category"]),
        Index(value = ["merchant"])
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "sms_id")
    val smsId: Long,

    val sender: String,

    val amount: Double,

    val type: TransactionType,

    val merchant: String,

    val category: String,

    val timestamp: Long,

    val balance: Double? = null,

    @ColumnInfo(name = "reference_id")
    val referenceId: String? = null,

    @ColumnInfo(name = "raw_body")
    val rawBody: String,

    @ColumnInfo(name = "is_user_edited", defaultValue = "0")
    val isUserEdited: Boolean = false
)
