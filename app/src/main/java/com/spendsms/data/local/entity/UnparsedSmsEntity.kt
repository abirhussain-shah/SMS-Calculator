package com.spendsms.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "unparsed_sms",
    indices = [
        Index(value = ["sms_id", "timestamp"], unique = true),
        Index(value = ["is_ignored"])
    ]
)
data class UnparsedSmsEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "sms_id")
    val smsId: Long,

    val sender: String,

    val body: String,

    val timestamp: Long,

    val reason: String,

    @ColumnInfo(name = "is_ignored", defaultValue = "0")
    val isIgnored: Boolean = false
)
