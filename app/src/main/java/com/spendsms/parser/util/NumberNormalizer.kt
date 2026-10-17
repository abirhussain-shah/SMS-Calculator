package com.spendsms.parser.util

object NumberNormalizer {

    private val banglaToAsciiDigits = mapOf(
        '০' to '0',
        '১' to '1',
        '২' to '2',
        '৩' to '3',
        '৪' to '4',
        '৫' to '5',
        '৬' to '6',
        '৭' to '7',
        '৮' to '8',
        '৯' to '9'
    )

    fun convertBanglaDigits(input: String): String {
        val sb = StringBuilder(input.length)
        for (char in input) {
            val converted = banglaToAsciiDigits[char]
            if (converted != null) {
                sb.append(converted)
            } else {
                sb.append(char)
            }
        }
        return sb.toString()
    }

    fun parseAmount(raw: String?): Double? {
        if (raw.isNullOrBlank()) return null
        return try {
            val normalized = convertBanglaDigits(raw)
                .replace("BDT", "", ignoreCase = true)
                .replace("TK.", "", ignoreCase = true)
                .replace("TK", "", ignoreCase = true)
                .replace("Tk.", "", ignoreCase = true)
                .replace("Tk", "", ignoreCase = true)
                .replace("৳", "")
                .replace(",", "")
                .trim()

            // Match numeric part with optional decimal
            val regex = Regex("""[+-]?\d+(?:\.\d+)?""")
            val match = regex.find(normalized) ?: return null
            match.value.toDoubleOrNull()
        } catch (_: Exception) {
            null
        }
    }
}
