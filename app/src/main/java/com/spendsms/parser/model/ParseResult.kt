package com.spendsms.parser.model

sealed interface ParseResult {
    data class Parsed(val transaction: ParsedTransaction) : ParseResult
    data class Ignored(val reason: String) : ParseResult
    data class Failed(val rawBody: String, val reason: String) : ParseResult
}
