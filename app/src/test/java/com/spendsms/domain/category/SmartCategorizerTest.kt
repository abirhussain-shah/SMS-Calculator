package com.spendsms.domain.category

import com.google.common.truth.Truth.assertThat
import com.spendsms.domain.model.TransactionType
import org.junit.Test

class SmartCategorizerTest {

    @Test
    fun extractFromRawSms_extractsAmountMerchantAndSuggestsCategory() {
        val raw = "Payment of Tk 1,450.00 to Star Kabab was successful. Fee Tk 0.00. Balance Tk 3,200.00. Ref 883HX."
        val extracted = SmartCategorizer.extractFromRawSms(rawBody = raw, sender = "bKash")

        assertThat(extracted.detectedAmount).isEqualTo(1450.0)
        assertThat(extracted.detectedMerchant).isEqualTo("Star Kabab")
        assertThat(extracted.detectedType).isEqualTo(TransactionType.DEBIT)
        assertThat(extracted.suggestedCategory.categoryId).isEqualTo("food")
        assertThat(extracted.suggestedCategory.confidence).isEqualTo(CategorizationConfidence.MEDIUM_KEYWORD)
    }

    @Test
    fun extractFromRawSms_detectsCreditTransactionAndSalary() {
        val raw = "You have received Tk 55,000.00 from ACME Corp Salary. Ref SAL991."
        val extracted = SmartCategorizer.extractFromRawSms(rawBody = raw, sender = "16216")

        assertThat(extracted.detectedAmount).isEqualTo(55000.0)
        assertThat(extracted.detectedType).isEqualTo(TransactionType.CREDIT)
        assertThat(extracted.suggestedCategory.categoryId).isEqualTo("salary")
    }

    @Test
    fun getSuggestion_userRuleOverridesDefaultKeyword() {
        // "Daraz" by default classifies as shopping, but user defined custom rule "bills"
        val suggestion = SmartCategorizer.getSuggestion(
            merchant = "Daraz",
            rawBody = "Payment to Daraz Tk 1000",
            type = TransactionType.DEBIT,
            ruleCategory = "bills"
        )

        assertThat(suggestion.categoryId).isEqualTo("bills")
        assertThat(suggestion.confidence).isEqualTo(CategorizationConfidence.HIGH_USER_RULE)
        assertThat(suggestion.reason).contains("Daraz")
    }

    @Test
    fun getSuggestion_keywordFallbackWhenNoUserRule() {
        val suggestion = SmartCategorizer.getSuggestion(
            merchant = "Carnival Internet",
            rawBody = "Bill paid to Carnival Internet Tk 1150",
            type = TransactionType.DEBIT,
            ruleCategory = null
        )

        assertThat(suggestion.categoryId).isEqualTo("bills")
        assertThat(suggestion.confidence).isEqualTo(CategorizationConfidence.MEDIUM_KEYWORD)
    }

    @Test
    fun getSuggestion_fallsBackToOtherWhenUnknown() {
        val suggestion = SmartCategorizer.getSuggestion(
            merchant = "XYZ Enterprise",
            rawBody = "Payment of Tk 500 to XYZ Enterprise",
            type = TransactionType.DEBIT,
            ruleCategory = null
        )

        assertThat(suggestion.categoryId).isEqualTo("other")
        assertThat(suggestion.confidence).isEqualTo(CategorizationConfidence.LOW_FALLBACK)
    }
}
