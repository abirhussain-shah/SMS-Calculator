package com.spendsms.parser.rule

import com.spendsms.domain.model.TransactionType

data class SmsPatternRule(
    val id: String,
    val provider: String,
    val pattern: Regex,
    val amountGroup: String = "amount",
    val merchantGroup: String? = "merchant",
    val defaultMerchant: String? = null,
    val balanceGroup: String? = "balance",
    val referenceIdGroup: String? = "refId",
    val transactionType: TransactionType = TransactionType.DEBIT,
    val typeIndicatorGroup: String? = null
)
