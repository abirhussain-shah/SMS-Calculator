package com.spendsms.parser

import com.spendsms.parser.model.ParseResult

object SmsParser {
    private val defaultEngine = SmsParserEngine()

    fun parse(sender: String?, rawBody: String?, smsTimestamp: Long? = null): ParseResult {
        return defaultEngine.parse(sender, rawBody, smsTimestamp)
    }
}
