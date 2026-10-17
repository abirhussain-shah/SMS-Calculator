package com.spendsms.parser.rule

import com.spendsms.domain.model.TransactionType

object DefaultSmsRules {

    val ignorePatterns = listOf(
        Regex("""(?i)\b(?:verification\s*code|security\s*code|otp|one\s*time\s*password)\b"""),
        Regex("""(?i)\b(?:your\s*pin\s*is|do\s*not\s*share\s*(?:your\s*)?(?:pin|otp)|never\s*share\s*(?:your\s*)?(?:pin|otp))\b"""),
        Regex("""(?i)\b(?:congratulations|cashback\s*offer|win\s*up\s*to|special\s*discount|promocode)\b"""),
        Regex("""(?i)\b(?:logged\s*in|login\s*attempt|device\s*registered)\b"""),
        Regex("""(?i)\b(?:your\s*current\s*balance\s*is\s*tk|available\s*balance\s*is)\b"""),
        Regex("""(?i)\b(?:you\s+have\s+only\s+[\d,.]+\s*(?:tk|bdt|৳)?\s*left|low\s*balance|recharge\s*now)\b""")
    )

    // Common extractor patterns for balance and reference IDs across all messages
    val balancePatterns = listOf(
        Regex("""(?i)(?:Avail(?:able)?\s*(?:Bal(?:ance)?|Limit)|Balance|Bal)[:\s]+(?:BDT|Tk\.?|৳)?\s*(?<balance>[\d,.]+)"""),
        Regex("""(?i)ব্যালেন্স[:\s]+(?:Tk\.?|৳)?\s*(?<balance>[\d,.]+)""")
    )

    val referenceIdPatterns = listOf(
        Regex("""(?i)\b(?:TrxID|TxnID|TxnId|Ref(?:erence)?(?:\s*ID)?)[:\s]+(?<refId>[A-Za-z0-9]+)\b"""),
        Regex("""(?i)\b(?:TrxID|TxnID)[:\s]*(?<refId>[A-Za-z0-9]+)\b""")
    )

    val rules: List<SmsPatternRule> = listOf(
        // ==========================
        // bKash Rules
        // ==========================
        // 1. bKash Merchant Payment
        SmsPatternRule(
            id = "bkash_payment",
            provider = "bKash",
            pattern = Regex(
                """(?i)Payment\s+(?:of\s+)?(?:Tk\.?|BDT|৳)?\s*(?<amount>[\d,.]+)\s+to\s+(?<merchant>[^.]+?)(?:\s+successful|\s+done|\.|\s+Fee)"""
            ),
            transactionType = TransactionType.DEBIT
        ),
        // 2. bKash Send Money
        SmsPatternRule(
            id = "bkash_send_money",
            provider = "bKash",
            pattern = Regex(
                """(?i)Send\s*Money\s+(?:of\s+)?(?:Tk\.?|BDT|৳)?\s*(?<amount>[\d,.]+)\s+to\s+(?<merchant>[^.]+?)(?:\s+successful|\s+done|\.|\s+Fee)"""
            ),
            defaultMerchant = "Send Money",
            transactionType = TransactionType.DEBIT
        ),
        // 3. bKash Cash Out
        SmsPatternRule(
            id = "bkash_cash_out",
            provider = "bKash",
            pattern = Regex(
                """(?i)Cash\s*Out\s+(?:of\s+)?(?:Tk\.?|BDT|৳)?\s*(?<amount>[\d,.]+)\s+(?:from|to)\s+(?<merchant>[^.]+?)(?:\s+successful|\s+done|\.|\s+Fee)"""
            ),
            defaultMerchant = "Cash Out",
            transactionType = TransactionType.DEBIT
        ),
        // 4. bKash Mobile Recharge
        SmsPatternRule(
            id = "bkash_recharge",
            provider = "bKash",
            pattern = Regex(
                """(?i)Mobile\s*Recharge\s+(?:of\s+)?(?:Tk\.?|BDT|৳)?\s*(?<amount>[\d,.]+)\s+to\s+(?<merchant>[^.]+?)(?:\s+successful|\s+done|\.|\s+Fee)"""
            ),
            defaultMerchant = "Mobile Recharge",
            transactionType = TransactionType.DEBIT
        ),
        // 5. bKash Received Money / Cash In (Credit)
        SmsPatternRule(
            id = "bkash_received",
            provider = "bKash",
            pattern = Regex(
                """(?i)You\s+have\s+received\s+(?:Tk\.?|BDT|৳)?\s*(?<amount>[\d,.]+)\s+from\s+(?<merchant>[^.]+?)(?:\.|\s+Fee)"""
            ),
            transactionType = TransactionType.CREDIT
        ),
        // 6. bKash Bangla SMS Format
        SmsPatternRule(
            id = "bkash_bangla",
            provider = "bKash",
            pattern = Regex(
                """(?i)(?<merchant>[^.]+?)(?:-এ| এ)?\s*(?:Tk\.?|৳)?\s*(?<amount>[\d,.]+)\s*টাকা\s*পেমেন্ট\s*সফল\s*হয়েছে"""
            ),
            transactionType = TransactionType.DEBIT
        ),

        // ==========================
        // Nagad Rules
        // ==========================
        // 7. Nagad Payment
        SmsPatternRule(
            id = "nagad_payment",
            provider = "Nagad",
            pattern = Regex(
                """(?i)Payment\s+(?:of\s+)?(?:Tk\.?|BDT|৳)?\s*(?<amount>[\d,.]+)\s+to\s+(?<merchant>[^.]+?)\s+is\s+successful"""
            ),
            transactionType = TransactionType.DEBIT
        ),
        // 8. Nagad Cash Out
        SmsPatternRule(
            id = "nagad_cash_out",
            provider = "Nagad",
            pattern = Regex(
                """(?i)Cash\s*Out\s+(?:of\s+)?(?:Tk\.?|BDT|৳)?\s*(?<amount>[\d,.]+)\s+(?:from|to)\s+(?<merchant>[^.]+?)\s+is\s+successful"""
            ),
            defaultMerchant = "Cash Out",
            transactionType = TransactionType.DEBIT
        ),
        // 9. Nagad Mobile Recharge
        SmsPatternRule(
            id = "nagad_recharge",
            provider = "Nagad",
            pattern = Regex(
                """(?i)Mobile\s*Recharge\s+(?:of\s+)?(?:Tk\.?|BDT|৳)?\s*(?<amount>[\d,.]+)\s+for\s+(?<merchant>[^.]+?)\s+is\s+successful"""
            ),
            defaultMerchant = "Mobile Recharge",
            transactionType = TransactionType.DEBIT
        ),
        // 10. Nagad Received (Credit)
        SmsPatternRule(
            id = "nagad_received",
            provider = "Nagad",
            pattern = Regex(
                """(?i)You\s+have\s+received\s+(?:Tk\.?|BDT|৳)?\s*(?<amount>[\d,.]+)\s+from\s+(?<merchant>[^.]+?)\."""
            ),
            transactionType = TransactionType.CREDIT
        ),

        // ==========================
        // Rocket (DBBL) Rules
        // ==========================
        // 11. Rocket Payment / Transfer
        SmsPatternRule(
            id = "rocket_payment",
            provider = "Rocket",
            pattern = Regex(
                """(?i)Payment\s+(?:Tk\.?|BDT|৳)?\s*(?<amount>[\d,.]+)\s+to\s+(?<merchant>[^.]+?)\s+successful"""
            ),
            transactionType = TransactionType.DEBIT
        ),
        // 12. Rocket Cash Out
        SmsPatternRule(
            id = "rocket_cash_out",
            provider = "Rocket",
            pattern = Regex(
                """(?i)Cash\s*Out\s+(?:Tk\.?|BDT|৳)?\s*(?<amount>[\d,.]+)\s+from\s+(?<merchant>[^.]+?)\s+successful"""
            ),
            defaultMerchant = "Cash Out",
            transactionType = TransactionType.DEBIT
        ),

        // ==========================
        // City Bank Card / Account Rules
        // ==========================
        // 13. City Bank Card POS / E-commerce Debit
        SmsPatternRule(
            id = "citybank_card_debit",
            provider = "CityBank",
            pattern = Regex(
                """(?i)Card\s+[*\d]+\s+has\s+been\s+debited\s+for\s+(?:BDT|Tk\.?|৳)\s*(?<amount>[\d,.]+)\s+at\s+(?<merchant>[^.]+?)\s+on"""
            ),
            transactionType = TransactionType.DEBIT
        ),
        // 14. City Bank Account Credit (e.g., Salary)
        SmsPatternRule(
            id = "citybank_acct_credit",
            provider = "CityBank",
            pattern = Regex(
                """(?i)Acct\s+[*\d]+\s+has\s+been\s+credited\s+with\s+(?:BDT|Tk\.?|৳)\s*(?<amount>[\d,.]+)\s+on\s+[\w-]+\s+towards\s+(?<merchant>[^.]+?)\."""
            ),
            transactionType = TransactionType.CREDIT
        ),
        // 15. City Bank ATM / Account Debit
        SmsPatternRule(
            id = "citybank_acct_debit",
            provider = "CityBank",
            pattern = Regex(
                """(?i)Acct\s+[*\d]+\s+is\s+debited\s+for\s+(?:BDT|Tk\.?|৳)\s*(?<amount>[\d,.]+)\s+on\s+[\w-]+\s+via\s+(?<merchant>[^.]+?)\."""
            ),
            transactionType = TransactionType.DEBIT
        ),

        // ==========================
        // BRAC Bank Rules
        // ==========================
        // 16. BRAC Bank Card Debit
        SmsPatternRule(
            id = "brac_card_debit",
            provider = "BRACBank",
            pattern = Regex(
                """(?i)Card\s+(?:ending\s+)?\w+\s+was\s+used\s+for\s+(?:BDT|Tk\.?|৳)\s*(?<amount>[\d,.]+)\s+at\s+(?<merchant>[^.]+?)\s+on"""
            ),
            transactionType = TransactionType.DEBIT
        ),
        // 17. BRAC Bank Account Debit
        SmsPatternRule(
            id = "brac_acct_debit",
            provider = "BRACBank",
            pattern = Regex(
                """(?i)A/C\s+[*\d]+\s+debited\s+by\s+(?:BDT|Tk\.?|৳)\s*(?<amount>[\d,.]+)\s+on\s+[\w-]+\.\s*Trans[:\s]+(?<merchant>[^.]+?)\."""
            ),
            transactionType = TransactionType.DEBIT
        ),

        // ==========================
        // EBL (Eastern Bank) Rules
        // ==========================
        // 18. EBL Card Debit
        SmsPatternRule(
            id = "ebl_card_debit",
            provider = "EBL",
            pattern = Regex(
                """(?i)Card\s+[*\d]+\s+has\s+been\s+debited\s+for\s+(?:BDT|Tk\.?|৳)\s*(?<amount>[\d,.]+)\s+at\s+(?<merchant>[^.]+?)\s+on"""
            ),
            transactionType = TransactionType.DEBIT
        ),
        // 19. EBL Account Debit
        SmsPatternRule(
            id = "ebl_acct_debit",
            provider = "EBL",
            pattern = Regex(
                """(?i)A/C\s+[*\d]+\s+is\s+debited\s+for\s+(?:BDT|Tk\.?|৳)\s*(?<amount>[\d,.]+)\s+on\s+[\w-]+"""
            ),
            defaultMerchant = "Bank Debit",
            transactionType = TransactionType.DEBIT
        ),

        // ==========================
        // SCB (Standard Chartered) Rules
        // ==========================
        // 20. SCB Card Debit
        SmsPatternRule(
            id = "scb_card_debit",
            provider = "SCB",
            pattern = Regex(
                """(?i)SCB\s+Card\s+(?:ending\s+)?\w+\s+spent\s+(?:BDT|Tk\.?|৳)\s*(?<amount>[\d,.]+)\s+at\s+(?<merchant>[^.]+?)\s+on"""
            ),
            transactionType = TransactionType.DEBIT
        ),

        // ==========================
        // Generic Bank Debit / Credit Fallbacks
        // ==========================
        // 21. Generic Card / Account Debit with merchant
        SmsPatternRule(
            id = "generic_debit_merchant",
            provider = "Bank",
            pattern = Regex(
                """(?i)(?:debited\s+(?:for|by|with)|spent)\s+(?:BDT|Tk\.?|৳)?\s*(?<amount>[\d,.]+)\s+(?:at|to|via)\s+(?<merchant>[^.,\n]+)"""
            ),
            transactionType = TransactionType.DEBIT
        ),
        // 22. Generic Card / Account Credit with sender/source
        SmsPatternRule(
            id = "generic_credit_source",
            provider = "Bank",
            pattern = Regex(
                """(?i)(?:credited\s+(?:for|by|with)|received)\s+(?:BDT|Tk\.?|৳)?\s*(?<amount>[\d,.]+)\s+(?:from|towards)\s+(?<merchant>[^.,\n]+)"""
            ),
            transactionType = TransactionType.CREDIT
        ),
        // 23. Simple Debit Fallback
        SmsPatternRule(
            id = "generic_debit_simple",
            provider = "Bank",
            pattern = Regex(
                """(?i)debited\s+(?:for|by|with)?\s*(?:BDT|Tk\.?|৳)?\s*(?<amount>[\d,.]+)"""
            ),
            defaultMerchant = "Bank Debit",
            transactionType = TransactionType.DEBIT
        ),
        // 24. Simple Credit Fallback
        SmsPatternRule(
            id = "generic_credit_simple",
            provider = "Bank",
            pattern = Regex(
                """(?i)credited\s+(?:for|by|with)?\s*(?:BDT|Tk\.?|৳)?\s*(?<amount>[\d,.]+)"""
            ),
            defaultMerchant = "Bank Credit",
            transactionType = TransactionType.CREDIT
        ),
        // 25. Telecom / Utility Service Deduction
        SmsPatternRule(
            id = "telecom_deduction",
            provider = "Telecom",
            pattern = Regex(
                """(?i)(?:Tk\.?|BDT|৳)?\s*(?<amount>[\d,.]+)\s+(?:has\s+been\s+)?deducted\s+for\s+(?<merchant>[^.,\n]+)"""
            ),
            transactionType = TransactionType.DEBIT
        )
    )
}
