package com.spendsms.data.export

import com.google.common.truth.Truth.assertThat
import com.spendsms.data.local.entity.TransactionEntity
import com.spendsms.domain.model.TransactionType
import org.junit.Test
import java.io.ByteArrayOutputStream

class CsvExporterTest {

    @Test
    fun exportToStream_writesCorrectCsvHeaderAndRows() {
        val tx1 = TransactionEntity(
            id = 1,
            smsId = 10,
            sender = "bKash",
            amount = 450.50,
            type = TransactionType.DEBIT,
            merchant = "Pizza Hut",
            category = "food",
            timestamp = 1729155000000L,
            balance = 2500.0,
            referenceId = "9K27XJ",
            rawBody = "Payment Tk 450.50 to Pizza Hut successful. TrxID 9K27XJ"
        )
        val tx2 = TransactionEntity(
            id = 2,
            smsId = 11,
            sender = "NAGAD",
            amount = 1000.0,
            type = TransactionType.CREDIT,
            merchant = "Sender, with comma & \"quotes\"",
            category = "salary",
            timestamp = 1729155000000L,
            balance = null,
            referenceId = null,
            rawBody = "You have received Tk 1,000\nLine 2"
        )

        val outputStream = ByteArrayOutputStream()
        val count = CsvExporter.exportToStream(listOf(tx1, tx2), outputStream)

        assertThat(count).isEqualTo(2)

        val csvContent = outputStream.toString("UTF-8")
        val lines = csvContent.lines().filter { it.isNotBlank() }

        // Header check
        assertThat(lines[0]).isEqualTo("ID,Date,Time,Sender,Merchant,Category,Type,Amount (BDT),Balance (BDT),Reference ID,Original SMS")

        // First row check
        assertThat(lines[1]).contains("bKash")
        assertThat(lines[1]).contains("Pizza Hut")
        assertThat(lines[1]).contains("DEBIT")
        assertThat(lines[1]).contains("450.50")
        assertThat(lines[1]).contains("2500.00")
        assertThat(lines[1]).contains("9K27XJ")

        // Second row check with escaping
        assertThat(csvContent).contains("\"Sender, with comma & \"\"quotes\"\"\"")
    }

    @Test
    fun escapeCsv_escapesCommasAndQuotesProperly() {
        assertThat(CsvExporter.escapeCsv("simple")).isEqualTo("simple")
        assertThat(CsvExporter.escapeCsv("one,two")).isEqualTo("\"one,two\"")
        assertThat(CsvExporter.escapeCsv("say \"hello\"")).isEqualTo("\"say \"\"hello\"\"\"")
        assertThat(CsvExporter.escapeCsv("line1\nline2")).isEqualTo("\"line1\nline2\"")
    }
}
