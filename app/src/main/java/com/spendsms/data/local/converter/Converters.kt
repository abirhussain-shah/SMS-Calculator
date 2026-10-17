package com.spendsms.data.local.converter

import androidx.room.TypeConverter
import com.spendsms.domain.model.TransactionType

class Converters {

    @TypeConverter
    fun fromTransactionType(type: TransactionType?): String? {
        return type?.name
    }

    @TypeConverter
    fun toTransactionType(value: String?): TransactionType? {
        return value?.let { TransactionType.fromString(it) }
    }
}
