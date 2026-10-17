package com.spendsms.parser

import com.spendsms.parser.category.CategoryClassifier
import com.spendsms.parser.model.ParseResult
import com.spendsms.parser.model.ParsedTransaction
import com.spendsms.parser.rule.DefaultSmsRules
import com.spendsms.parser.rule.SmsPatternRule
import com.spendsms.parser.util.NumberNormalizer

class SmsParserEngine(
    private val rules: List<SmsPatternRule> = DefaultSmsRules.rules,
    private val ignorePatterns: List<Regex> = DefaultSmsRules.ignorePatterns,
    private val balancePatterns: List<Regex> = DefaultSmsRules.balancePatterns,
    private val referenceIdPatterns: List<Regex> = DefaultSmsRules.referenceIdPatterns
) {

    fun parse(
        sender: String?,
        rawBody: String?,
        smsTimestamp: Long? = null
    ): ParseResult {
        if (rawBody.isNullOrBlank()) {
            return ParseResult.Failed(
                rawBody = rawBody.orEmpty(),
                reason = "SMS body is empty"
            )
        }

        val cleanedBody = rawBody.trim()

        // 1. Check if the message matches any ignore pattern (OTPs, Security codes, marketing)
        for (pattern in ignorePatterns) {
            if (pattern.containsMatchIn(cleanedBody)) {
                return ParseResult.Ignored(reason = "Matched ignore pattern: ${pattern.pattern}")
            }
        }

        // 2. Convert any Bangla digits to ASCII for uniform pattern matching
        val normalizedBody = NumberNormalizer.convertBanglaDigits(cleanedBody)

        // 3. Try matching against our configurable rules
        for (rule in rules) {
            val match = rule.pattern.find(normalizedBody) ?: continue

            try {
                // Extract amount
                val amountStr = runCatching { match.groups[rule.amountGroup]?.value }.getOrNull()
                val amount = NumberNormalizer.parseAmount(amountStr) ?: continue

                if (amount <= 0.0) continue

                // Extract merchant / counterparty
                var merchant = rule.defaultMerchant
                if (rule.merchantGroup != null) {
                    val extracted = runCatching { match.groups[rule.merchantGroup]?.value }.getOrNull()?.trim()
                    if (!extracted.isNullOrBlank()) {
                        merchant = extracted
                    }
                }
                merchant = cleanMerchant(merchant ?: sender ?: "Unknown")

                // Extract optional balance using dedicated balance patterns
                var balance: Double? = null
                for (bPattern in balancePatterns) {
                    val bMatch = bPattern.find(normalizedBody)
                    if (bMatch != null) {
                        val bStr = runCatching { bMatch.groups["balance"]?.value }.getOrNull()
                        balance = NumberNormalizer.parseAmount(bStr)
                        if (balance != null) break
                    }
                }

                // Extract optional reference / transaction ID using dedicated patterns
                var refId: String? = null
                for (rPattern in referenceIdPatterns) {
                    val rMatch = rPattern.find(normalizedBody)
                    if (rMatch != null) {
                        val idStr = runCatching { rMatch.groups["refId"]?.value }.getOrNull()?.trim()
                        if (!idStr.isNullOrBlank()) {
                            refId = idStr
                            break
                        }
                    }
                }

                // Categorize
                val category = CategoryClassifier.classify(
                    merchant = merchant,
                    rawText = cleanedBody,
                    type = rule.transactionType
                )

                return ParseResult.Parsed(
                    transaction = ParsedTransaction(
                        amount = amount,
                        type = rule.transactionType,
                        merchant = merchant,
                        timestamp = smsTimestamp,
                        balance = balance,
                        referenceId = refId,
                        category = category.id,
                        provider = rule.provider
                    )
                )
            } catch (e: Exception) {
                // Rule matching error - continue to next candidate rule
                continue
            }
        }

        // If no rule matched, return Failed so it appears in the "Needs review" list
        return ParseResult.Failed(
            rawBody = cleanedBody,
            reason = "No matching financial transaction pattern found"
        )
    }

    private fun cleanMerchant(raw: String): String {
        return raw
            .replace(Regex("""(?i)\b(?:successful|done|at|via|to|from|is)\b"""), "")
            .replace(Regex("""[.,:;]+$"""), "")
            .trim()
            .ifBlank { "Unknown Merchant" }
    }
}
