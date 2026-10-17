package com.spendsms.domain.model

enum class TransactionType {
    DEBIT,
    CREDIT;

    companion object {
        fun fromString(value: String): TransactionType {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: DEBIT
        }
    }
}
