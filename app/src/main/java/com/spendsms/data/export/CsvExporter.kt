package com.spendsms.data.export

import com.spendsms.data.local.entity.TransactionEntity
import com.spendsms.ui.util.DateTimeFormatters
import java.io.BufferedWriter
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets

object CsvExporter {

    private val HEADERS = listOf(
        "ID",
        "Date",
        "Time",
        "Sender",
        "Merchant",
        "Category",
        "Type",
        "Amount (BDT)",
        "Balance (BDT)",
        "Reference ID",
        "Original SMS"
    )

    fun exportToStream(transactions: List<TransactionEntity>, outputStream: OutputStream): Int {
        val writer = BufferedWriter(OutputStreamWriter(outputStream, StandardCharsets.UTF_8))
        try {
            // Write CSV header
            writer.write(HEADERS.joinToString(","))
            writer.newLine()

            // Write transaction rows
            for (tx in transactions) {
                val row = listOf(
                    tx.id.toString(),
                    escapeCsv(DateTimeFormatters.formatTransactionDate(tx.timestamp)),
                    escapeCsv(DateTimeFormatters.formatTransactionTime(tx.timestamp)),
                    escapeCsv(tx.sender),
                    escapeCsv(tx.merchant),
                    escapeCsv(tx.category),
                    tx.type.name,
                    String.format(java.util.Locale.US, "%.2f", tx.amount),
                    if (tx.balance != null) String.format(java.util.Locale.US, "%.2f", tx.balance) else "",
                    escapeCsv(tx.referenceId.orEmpty()),
                    escapeCsv(tx.rawBody)
                )
                writer.write(row.joinToString(","))
                writer.newLine()
            }
            writer.flush()
            return transactions.size
        } finally {
            writer.close()
        }
    }

    fun escapeCsv(value: String): String {
        val needsQuotes = value.contains(',') || value.contains('"') || value.contains('\n') || value.contains('\r')
        return if (needsQuotes) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
    }
}
