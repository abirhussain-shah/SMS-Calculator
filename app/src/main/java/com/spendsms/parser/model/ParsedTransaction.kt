package com.spendsms.parser.model

import com.spendsms.domain.model.TransactionType

data class ParsedTransaction(
    val amount: Double,
    val type: TransactionType,
    val merchant: String,
    val timestamp: Long? = null,
    val balance: Double? = null,
    val referenceId: String? = null,
    val category: String,
    val provider: String
)
