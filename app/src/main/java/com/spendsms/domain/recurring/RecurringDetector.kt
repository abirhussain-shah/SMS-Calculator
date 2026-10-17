package com.spendsms.domain.recurring

import com.spendsms.data.local.entity.RecurringPaymentEntity
import com.spendsms.data.local.entity.TransactionEntity
import com.spendsms.domain.model.TransactionType
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.abs

object RecurringDetector {

    private const val DAY_IN_MILLIS = 24L * 60L * 60L * 1000L

    fun detectRecurringPayments(
        transactions: List<TransactionEntity>,
        nowMillis: Long = System.currentTimeMillis()
    ): List<RecurringPaymentEntity> {
        val debitTransactions = transactions
            .filter { it.type == TransactionType.DEBIT && it.merchant.isNotBlank() }
            .sortedBy { it.timestamp }

        val merchantGroups = debitTransactions.groupBy { it.merchant.trim().lowercase() }
        val detected = mutableListOf<RecurringPaymentEntity>()

        for ((_, group) in merchantGroups) {
            if (group.size < 2) continue

            val merchantName = group.last().merchant.trim()
            val category = group.groupingBy { it.category }.eachCount().maxByOrNull { it.value }?.key ?: "bills"

            // Analyze intervals between consecutive payments
            val intervals = mutableListOf<Long>()
            for (i in 0 until group.size - 1) {
                val diffDays = (group[i + 1].timestamp - group[i].timestamp) / DAY_IN_MILLIS
                if (diffDays > 0) {
                    intervals.add(diffDays)
                }
            }

            if (intervals.isEmpty()) continue

            // Check if amounts are reasonably consistent (+/- 25% tolerance from average)
            val avgAmount = group.map { it.amount }.average()
            val amountsConsistent = group.all { abs(it.amount - avgAmount) / avgAmount <= 0.25 }
            if (!amountsConsistent) continue

            val isMonthly = intervals.all { it in 25..36 } || (intervals.size >= 2 && intervals.count { it in 25..36 } >= (intervals.size * 0.6))
            val isWeekly = intervals.all { it in 5..9 } || (intervals.size >= 2 && intervals.count { it in 5..9 } >= (intervals.size * 0.6))

            if (!isMonthly && !isWeekly) continue

            val frequency = if (isMonthly) {
                RecurringPaymentEntity.FREQUENCY_MONTHLY
            } else {
                RecurringPaymentEntity.FREQUENCY_WEEKLY
            }

            val lastTransaction = group.last()
            val lastPaymentDate = lastTransaction.timestamp
            val zoneId = ZoneId.systemDefault()
            val lastZdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(lastPaymentDate), zoneId)
            val approximateDay = lastZdt.dayOfMonth

            // Calculate next expected date
            var nextZdt = if (frequency == RecurringPaymentEntity.FREQUENCY_MONTHLY) {
                lastZdt.plusMonths(1)
            } else {
                lastZdt.plusWeeks(1)
            }

            val nowZdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(nowMillis), zoneId)
            // If already in the past, advance until in the future or current period
            while (nextZdt.isBefore(nowZdt.minusDays(1))) {
                nextZdt = if (frequency == RecurringPaymentEntity.FREQUENCY_MONTHLY) {
                    nextZdt.plusMonths(1)
                } else {
                    nextZdt.plusWeeks(1)
                }
            }

            val nextExpectedDate = nextZdt.toInstant().toEpochMilli()

            detected.add(
                RecurringPaymentEntity(
                    merchant = merchantName,
                    amount = lastTransaction.amount,
                    category = category,
                    frequency = frequency,
                    approximateDay = approximateDay,
                    lastPaymentDate = lastPaymentDate,
                    nextExpectedDate = nextExpectedDate,
                    isDismissed = false,
                    isConfirmed = false,
                    reminderDaysBefore = 2
                )
            )
        }

        return detected
    }
}
