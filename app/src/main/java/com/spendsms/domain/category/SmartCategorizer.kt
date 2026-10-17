package com.spendsms.domain.category

import com.spendsms.domain.model.Category
import com.spendsms.domain.model.TransactionType
import com.spendsms.parser.category.CategoryClassifier
import java.util.Locale
import java.util.regex.Pattern

enum class CategorizationConfidence {
    HIGH_USER_RULE,
    MEDIUM_KEYWORD,
    LOW_FALLBACK
}

data class CategorizationSuggestion(
    val categoryId: String,
    val confidence: CategorizationConfidence,
    val reason: String
)

data class ExtractedSmsData(
    val detectedAmount: Double?,
    val detectedMerchant: String?,
    val detectedType: TransactionType,
    val suggestedCategory: CategorizationSuggestion
)

object SmartCategorizer {

    private val amountPatterns = listOf(
        Pattern.compile("""(?:Tk|Tk\.|BDT|Tk\s*|BDT\s*)\s*([0-9,]+(?:\.[0-9]{1,2})?)""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""(?:Amount|Amt|Total)[:\s]+(?:Tk|BDT)?\s*([0-9,]+(?:\.[0-9]{1,2})?)""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""(?:paid|debited|spent|transferred|recharge of)\s+(?:Tk|BDT)?\s*([0-9,]+(?:\.[0-9]{1,2})?)""", Pattern.CASE_INSENSITIVE)
    )

    private val merchantToPatterns = listOf(
        Pattern.compile("""(?:payment to|paid to|to|at)\s+([A-Za-z0-9&']+(?:\s+[A-Za-z0-9&']+){0,3}?)(?:\s+(?:was|is|successful|ref|fee|on|\.|\,)|$)""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""(?:merchant:?\s*)([A-Za-z0-9&']+(?:\s+[A-Za-z0-9&']+){0,3}?)(?:\.|\,|$|\n|\s+fee)""", Pattern.CASE_INSENSITIVE)
    )

    private val stopWords = setOf("was", "is", "successful", "done", "fee", "ref", "on", "at", "to", "for", "you")
    private val creditKeywords = listOf("received", "credited", "cash in", "cashin", "deposit", "inward", "salary")

    fun getSuggestion(
        merchant: String,
        rawBody: String = "",
        type: TransactionType = TransactionType.DEBIT,
        ruleCategory: String? = null
    ): CategorizationSuggestion {
        // Priority 1: User learned / custom rule
        if (!ruleCategory.isNullOrBlank()) {
            return CategorizationSuggestion(
                categoryId = ruleCategory.lowercase(Locale.US),
                confidence = CategorizationConfidence.HIGH_USER_RULE,
                reason = "Matched saved rule for '$merchant'"
            )
        }

        // Priority 2: Built-in classifier keywords
        val classified = CategoryClassifier.classify(merchant = merchant, rawText = rawBody, type = type)
        if (classified != Category.OTHER) {
            return CategorizationSuggestion(
                categoryId = classified.id,
                confidence = CategorizationConfidence.MEDIUM_KEYWORD,
                reason = "Suggested from merchant keyword"
            )
        }

        // Priority 3: Fallback
        return CategorizationSuggestion(
            categoryId = Category.OTHER.id,
            confidence = CategorizationConfidence.LOW_FALLBACK,
            reason = "Default category"
        )
    }

    fun extractFromRawSms(
        rawBody: String,
        sender: String,
        existingRuleCategory: String? = null
    ): ExtractedSmsData {
        // Extract amount
        var detectedAmount: Double? = null
        for (pattern in amountPatterns) {
            val matcher = pattern.matcher(rawBody)
            if (matcher.find()) {
                val cleaned = matcher.group(1)?.replace(",", "")
                val parsed = cleaned?.toDoubleOrNull()
                if (parsed != null && parsed > 0.0) {
                    detectedAmount = parsed
                    break
                }
            }
        }

        // Detect type
        val lowerBody = rawBody.lowercase(Locale.US)
        val isCredit = creditKeywords.any { lowerBody.contains(it) }
        val detectedType = if (isCredit) TransactionType.CREDIT else TransactionType.DEBIT

        // Detect merchant
        var detectedMerchant: String? = null
        for (pattern in merchantToPatterns) {
            val matcher = pattern.matcher(rawBody)
            if (matcher.find()) {
                val candidate = matcher.group(1)?.trim()
                if (!candidate.isNullOrBlank()) {
                    val cleanWords = candidate.split(Regex("""\s+""")).filterNot { it.lowercase() in stopWords }
                    val cleaned = cleanWords.joinToString(" ").trim()
                    if (cleaned.length in 3..30) {
                        detectedMerchant = cleaned
                        break
                    }
                }
            }
        }
        if (detectedMerchant.isNullOrBlank()) {
            detectedMerchant = sender.trim().ifBlank { "Unknown" }
        }

        val suggestion = getSuggestion(
            merchant = detectedMerchant,
            rawBody = rawBody,
            type = detectedType,
            ruleCategory = existingRuleCategory
        )

        return ExtractedSmsData(
            detectedAmount = detectedAmount,
            detectedMerchant = detectedMerchant,
            detectedType = detectedType,
            suggestedCategory = suggestion
        )
    }
}
