package com.spendsms.domain.recurring

import com.google.common.truth.Truth.assertThat
import com.spendsms.data.local.entity.RecurringPaymentEntity
import com.spendsms.data.local.entity.TransactionEntity
import com.spendsms.domain.model.TransactionType
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class RecurringDetectorTest {

    private val zoneId = ZoneId.systemDefault()
    private val dayMillis = 24L * 60L * 60L * 1000L

    private fun dateToMillis(year: Int, month: Int, day: Int): Long {
        return LocalDate.of(year, month, day)
            .atStartOfDay(zoneId)
            .toInstant()
            .toEpochMilli()
    }

    @Test
    fun detectMonthlyRecurringPayments_identifiesRegularMonthlyBills() {
        // 3 consecutive monthly payments for Carnival Internet on the 5th
        val t1 = TransactionEntity(
            smsId = 1L, sender = "bKash", amount = 1150.0,
            type = TransactionType.DEBIT, merchant = "Carnival Internet",
            category = "utilities", timestamp = dateToMillis(2026, 7, 5),
            rawBody = "Payment of Tk 1150 to Carnival Internet"
        )
        val t2 = TransactionEntity(
            smsId = 2L, sender = "bKash", amount = 1150.0,
            type = TransactionType.DEBIT, merchant = "Carnival Internet",
            category = "utilities", timestamp = dateToMillis(2026, 8, 5),
            rawBody = "Payment of Tk 1150 to Carnival Internet"
        )
        val t3 = TransactionEntity(
            smsId = 3L, sender = "bKash", amount = 1150.0,
            type = TransactionType.DEBIT, merchant = "Carnival Internet",
            category = "utilities", timestamp = dateToMillis(2026, 9, 5),
            rawBody = "Payment of Tk 1150 to Carnival Internet"
        )

        val now = dateToMillis(2026, 9, 20)
        val result = RecurringDetector.detectRecurringPayments(listOf(t1, t2, t3), now)

        assertThat(result).hasSize(1)
        val recurring = result[0]
        assertThat(recurring.merchant).isEqualTo("Carnival Internet")
        assertThat(recurring.frequency).isEqualTo(RecurringPaymentEntity.FREQUENCY_MONTHLY)
        assertThat(recurring.amount).isEqualTo(1150.0)
        assertThat(recurring.approximateDay).isEqualTo(5)
        assertThat(recurring.category).isEqualTo("utilities")
        // Next expected date should be in October 2026
        val nextDate = LocalDate.ofInstant(java.time.Instant.ofEpochMilli(recurring.nextExpectedDate), zoneId)
        assertThat(nextDate.year).isEqualTo(2026)
        assertThat(nextDate.monthValue).isEqualTo(10)
        assertThat(nextDate.dayOfMonth).isEqualTo(5)
    }

    @Test
    fun detectWeeklyRecurringPayments_identifiesWeeklySubscriptions() {
        // 3 consecutive weekly payments spaced 7 days apart
        val baseDate = dateToMillis(2026, 8, 1)
        val t1 = TransactionEntity(
            smsId = 10L, sender = "NAGAD", amount = 250.0,
            type = TransactionType.DEBIT, merchant = "Weekly Milk",
            category = "groceries", timestamp = baseDate,
            rawBody = "Tk 250 to Weekly Milk"
        )
        val t2 = TransactionEntity(
            smsId = 11L, sender = "NAGAD", amount = 250.0,
            type = TransactionType.DEBIT, merchant = "Weekly Milk",
            category = "groceries", timestamp = baseDate + 7 * dayMillis,
            rawBody = "Tk 250 to Weekly Milk"
        )
        val t3 = TransactionEntity(
            smsId = 12L, sender = "NAGAD", amount = 250.0,
            type = TransactionType.DEBIT, merchant = "Weekly Milk",
            category = "groceries", timestamp = baseDate + 14 * dayMillis,
            rawBody = "Tk 250 to Weekly Milk"
        )

        val now = baseDate + 15 * dayMillis
        val result = RecurringDetector.detectRecurringPayments(listOf(t1, t2, t3), now)

        assertThat(result).hasSize(1)
        val recurring = result[0]
        assertThat(recurring.merchant).isEqualTo("Weekly Milk")
        assertThat(recurring.frequency).isEqualTo(RecurringPaymentEntity.FREQUENCY_WEEKLY)
        assertThat(recurring.amount).isEqualTo(250.0)
    }

    @Test
    fun ignoresSingleTransactions() {
        val t1 = TransactionEntity(
            smsId = 1L, sender = "bKash", amount = 5000.0,
            type = TransactionType.DEBIT, merchant = "One Time Vendor",
            category = "shopping", timestamp = dateToMillis(2026, 8, 1),
            rawBody = "Tk 5000 paid"
        )

        val result = RecurringDetector.detectRecurringPayments(listOf(t1))
        assertThat(result).isEmpty()
    }

    @Test
    fun ignoresInconsistentAmounts() {
        // Payments spaced 30 days apart but amount changes dramatically (> 25% tolerance)
        val t1 = TransactionEntity(
            smsId = 1L, sender = "bKash", amount = 1000.0,
            type = TransactionType.DEBIT, merchant = "Aarong",
            category = "shopping", timestamp = dateToMillis(2026, 7, 1),
            rawBody = "Tk 1000 paid"
        )
        val t2 = TransactionEntity(
            smsId = 2L, sender = "bKash", amount = 8500.0,
            type = TransactionType.DEBIT, merchant = "Aarong",
            category = "shopping", timestamp = dateToMillis(2026, 8, 1),
            rawBody = "Tk 8500 paid"
        )

        val result = RecurringDetector.detectRecurringPayments(listOf(t1, t2))
        assertThat(result).isEmpty()
    }

    @Test
    fun ignoresSporadicNonPeriodicIntervals() {
        // Random intervals: 3 days, 12 days - neither weekly nor monthly
        val base = dateToMillis(2026, 8, 1)
        val t1 = TransactionEntity(
            smsId = 1L, sender = "bKash", amount = 500.0,
            type = TransactionType.DEBIT, merchant = "Random Store",
            category = "food", timestamp = base,
            rawBody = "Tk 500"
        )
        val t2 = TransactionEntity(
            smsId = 2L, sender = "bKash", amount = 500.0,
            type = TransactionType.DEBIT, merchant = "Random Store",
            category = "food", timestamp = base + 3 * dayMillis,
            rawBody = "Tk 500"
        )
        val t3 = TransactionEntity(
            smsId = 3L, sender = "bKash", amount = 500.0,
            type = TransactionType.DEBIT, merchant = "Random Store",
            category = "food", timestamp = base + 15 * dayMillis,
            rawBody = "Tk 500"
        )

        val result = RecurringDetector.detectRecurringPayments(listOf(t1, t2, t3))
        assertThat(result).isEmpty()
    }
}
