package com.spendsms.ui.util

import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateTimeFormatters {

    private val currencyFormat = NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }

    private val monthYearFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
    private val dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.getDefault())
    private val timeFormatter = DateTimeFormatter.ofPattern("hh:mm a", Locale.getDefault())
    private val fullDateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a", Locale.getDefault())

    fun formatCurrency(amount: Double, showSymbol: Boolean = true): String {
        val formatted = currencyFormat.format(amount)
        return if (showSymbol) "৳ $formatted" else formatted
    }

    fun formatMonthYear(yearMonth: YearMonth): String {
        return yearMonth.format(monthYearFormatter)
    }

    fun formatTransactionDate(timestamp: Long): String {
        val zone = ZoneId.systemDefault()
        val txDate = Instant.ofEpochMilli(timestamp).atZone(zone).toLocalDate()
        val today = LocalDate.now(zone)

        return when {
            txDate == today -> "Today"
            txDate == today.minusDays(1) -> "Yesterday"
            else -> txDate.format(dateFormatter)
        }
    }

    fun formatDate(timestamp: Long): String {
        val zone = ZoneId.systemDefault()
        return Instant.ofEpochMilli(timestamp).atZone(zone).toLocalDate().format(dateFormatter)
    }

    fun formatTransactionTime(timestamp: Long): String {
        val zone = ZoneId.systemDefault()
        return Instant.ofEpochMilli(timestamp).atZone(zone).toLocalTime().format(timeFormatter)
    }

    fun formatFullDateTime(timestamp: Long): String {
        val zone = ZoneId.systemDefault()
        return Instant.ofEpochMilli(timestamp).atZone(zone).format(fullDateTimeFormatter)
    }

    fun getStartAndEndOfMonth(yearMonth: YearMonth): Pair<Long, Long> {
        val zone = ZoneId.systemDefault()
        val startOfMonth = yearMonth.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val endOfMonth = yearMonth.atEndOfMonth().atTime(23, 59, 59, 999_000_000).atZone(zone).toInstant().toEpochMilli()
        return Pair(startOfMonth, endOfMonth)
    }
}
