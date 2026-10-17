package com.spendsms.parser.category

import com.spendsms.domain.model.Category
import com.spendsms.domain.model.TransactionType

object CategoryClassifier {

    private val foodKeywords = listOf(
        "kabab", "restaurant", "cafe", "pizza", "burger", "sultan", "kacchi",
        "foodpanda", "food", "dine", "dining", "bakery", "kitchen", "coffee",
        "bake", "shawarma", "chili", "sweet", "sweets", "tea", "bistro",
        "grill", "madchef", "takeout", "biryani", "roast", "catering"
    )

    private val transportKeywords = listOf(
        "uber", "pathao ride", "pathao car", "pathao bike", "shohoz", "railway",
        "train", "cng", "bus", "fuel", "filling", "petrol", "diesel", "octane",
        "biman", "us-bangla", "novoair", "airline", "metro", "toll", "parking"
    )

    private val billsKeywords = listOf(
        "desco", "dpdc", "nesco", "wasa", "titas", "bakhrabad", "karnaphuli",
        "electricity", "electric", "gas", "water", "internet", "carnival",
        "link3", "dot internet", "amberit", "btcl", "utility", "bill",
        "polli bidyut", "breb", "palli"
    )

    private val shoppingKeywords = listOf(
        "daraz", "aarong", "shwapno", "unimart", "agora", "meena bazar",
        "apex", "bata", "chaldal", "pickaboo", "yellow", "sailor", "lotto",
        "super shop", "store", "clothing", "fashion", "mart", "mall",
        "bazaar", "outlet", "market", "retail", "brand"
    )

    private val rechargeKeywords = listOf(
        "recharge", "flexiload", "grameenphone", "gp", "banglalink", "robi",
        "airtel", "teletalk", "topup", "top-up", "prepaid recharge"
    )

    private val transferKeywords = listOf(
        "cash out", "cashout", "send money", "atm", "cash in", "cashin",
        "fund transfer", "npsb", "beftn", "rtgs", "ibanking", "transfer",
        "received money", "withdrawal"
    )

    private val salaryKeywords = listOf(
        "salary", "payroll", "remittance", "inward remittance", "dividend",
        "bonus", "pension", "honorarium"
    )

    fun classify(
        merchant: String,
        rawText: String = "",
        type: TransactionType = TransactionType.DEBIT
    ): Category {
        val combined = "$merchant $rawText".lowercase()

        if (type == TransactionType.CREDIT) {
            if (salaryKeywords.any { combined.contains(it) }) {
                return Category.SALARY
            }
        }

        if (rechargeKeywords.any { combined.contains(it) }) return Category.RECHARGE
        if (foodKeywords.any { combined.contains(it) }) return Category.FOOD
        if (transportKeywords.any { combined.contains(it) }) return Category.TRANSPORT
        if (billsKeywords.any { combined.contains(it) }) return Category.BILLS
        if (shoppingKeywords.any { combined.contains(it) }) return Category.SHOPPING
        if (transferKeywords.any { combined.contains(it) }) return Category.TRANSFER

        return Category.OTHER
    }
}
