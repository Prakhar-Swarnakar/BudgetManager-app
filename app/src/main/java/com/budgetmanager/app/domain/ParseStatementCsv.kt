package com.budgetmanager.app.domain

import com.budgetmanager.app.core.model.MessageDirection
import com.budgetmanager.app.core.model.Money
import com.budgetmanager.app.core.model.StatementLine
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.util.Locale

sealed interface StatementCsvResult {
    data class Success(val lines: List<StatementLine>, val skippedRowCount: Int) : StatementCsvResult
    data class Failure(val reason: String) : StatementCsvResult
}

/**
 * Reads a bank/credit-card statement CSV into [StatementLine]s. Column layout varies bank to
 * bank - this is a best-effort generic reader (headers matched by keyword, not a fixed schema),
 * the trade-off accepted for starting with CSV before any real sample statements exist to build
 * precise per-bank layouts from, the same reasoning M2b originally used for the SMS parser's own
 * generic fallback (06-backlog.md, "Credit card / bank statement import").
 *
 * Recognised header keywords (case-insensitive, matched as a substring):
 * - Date: "date"
 * - Description: "narration", "description", "particulars", "remarks", "details"
 * - Either a separate Debit/Withdrawal and Credit/Deposit column, or one Amount column paired
 *   with a Type/Dr-Cr column, or one Amount column alone (sign gives direction - a negative
 *   amount is a debit).
 *
 * A row that can't be read (bad date, no usable amount) is skipped and counted, not silently
 * dropped without a trace - the caller can tell you "N rows couldn't be read" if that count is
 * ever non-zero.
 */
object ParseStatementCsv {

    /** How many leading rows to scan for the real header before giving up - shared with
     *  [ParseStatementExcel]. 10 wasn't enough for a real ICICI statement export (an 11-row
     *  search-criteria preamble - "DETAILED STATEMENT", "Search", "Account Number", "Transaction
     *  Date from", …) pushed the real header to row 12; 25 gives real exports room to have an
     *  even longer preamble. */
    internal const val HEADER_SEARCH_WINDOW = 25

    private val dateFormatters: List<DateTimeFormatter> = listOf(
        "d/M/yyyy", "d-M-yyyy", "yyyy-M-d", "d/M/yy", "d-M-yy"
    ).map { DateTimeFormatter.ofPattern(it, Locale.ENGLISH) } + listOf(
        DateTimeFormatterBuilder().appendPattern("d-MMM-yyyy").toFormatter(Locale.ENGLISH),
        DateTimeFormatterBuilder().appendPattern("d-MMM-yy").toFormatter(Locale.ENGLISH),
        DateTimeFormatterBuilder().appendPattern("d MMM yyyy").toFormatter(Locale.ENGLISH),
        DateTimeFormatterBuilder().appendPattern("d MMM yy").toFormatter(Locale.ENGLISH)
    )

    operator fun invoke(csvText: String): StatementCsvResult {
        val rows = csvText.lines()
            .map { it.trimEnd('\r') }
            .filter { it.isNotBlank() }
            .map { splitCsvLine(it) }
        if (rows.isEmpty()) return StatementCsvResult.Failure("That file has no rows to read.")

        // The real header is sometimes a few rows down, past account-summary/disclaimer lines
        // common in Indian bank exports - scan ahead for one that looks like a header rather
        // than assuming row 0 always is.
        val headerIndex = rows.take(HEADER_SEARCH_WINDOW).indexOfFirst { looksLikeHeaderRow(it) }
        if (headerIndex < 0) {
            return StatementCsvResult.Failure(
                "Couldn't find a Date and Amount column in this file's header row."
            )
        }

        val header = rows[headerIndex].map { it.trim().lowercase(Locale.ENGLISH) }
        val columns = detectColumns(header)
        if (columns.date < 0) return StatementCsvResult.Failure("Couldn't find a Date column in this file.")
        if (columns.debit < 0 && columns.credit < 0 && columns.amount < 0) {
            return StatementCsvResult.Failure("Couldn't find a Debit/Credit or Amount column in this file.")
        }

        val lines = mutableListOf<StatementLine>()
        var skipped = 0
        for (row in rows.drop(headerIndex + 1)) {
            val line = parseRow(row, columns)
            if (line != null) lines += line else skipped++
        }

        if (lines.isEmpty()) {
            return StatementCsvResult.Failure("Found a header row, but no readable transaction rows after it.")
        }
        return StatementCsvResult.Success(lines, skipped)
    }

    internal data class Columns(
        val date: Int,
        val description: Int,
        val debit: Int,
        val credit: Int,
        val amount: Int,
        val type: Int
    )

    /** Scans a header row's cells and builds a [Columns] layout from keyword matches - shared
     *  with [com.budgetmanager.app.domain.ParseStatementExcel], which feeds it a cell grid read
     *  from a workbook instead of CSV text. */
    internal fun detectColumns(header: List<String>): Columns = Columns(
        date = header.indexOfFirst { "date" in it },
        description = header.indexOfFirst {
            "narration" in it || "description" in it || "particulars" in it ||
                "remarks" in it || "details" in it
        },
        debit = header.indexOfFirst { "debit" in it || "withdrawal" in it },
        credit = header.indexOfFirst { "credit" in it || "deposit" in it },
        amount = header.indexOfFirst { it == "amount" || it.startsWith("amount") },
        type = header.indexOfFirst { "type" in it || "dr/cr" in it || "dr / cr" in it }
    )

    internal fun looksLikeHeaderRow(cells: List<String>): Boolean {
        val lower = cells.map { it.trim().lowercase(Locale.ENGLISH) }
        val hasDate = lower.any { "date" in it }
        val hasAmountShape = lower.any {
            "debit" in it || "withdrawal" in it || "credit" in it || "deposit" in it || it == "amount" || it.startsWith("amount")
        }
        return hasDate && hasAmountShape
    }

    internal fun parseRow(row: List<String>, columns: Columns): StatementLine? {
        val dateCell = row.getOrNull(columns.date)?.trim().orEmpty()
        val date = parseDate(dateCell) ?: return null

        val description = columns.description.takeIf { it >= 0 }
            ?.let { row.getOrNull(it)?.trim() }
            ?: ""

        val (amount, direction) = resolveAmountAndDirection(row, columns) ?: return null
        return StatementLine(date = date, description = description, amount = amount, direction = direction)
    }

    internal fun resolveAmountAndDirection(row: List<String>, columns: Columns): Pair<Money, MessageDirection>? {
        if (columns.debit >= 0 || columns.credit >= 0) {
            val debitAmount = columns.debit.takeIf { it >= 0 }?.let { row.getOrNull(it) }?.let { parseAmount(it) }
            val creditAmount = columns.credit.takeIf { it >= 0 }?.let { row.getOrNull(it) }?.let { parseAmount(it) }
            return when {
                debitAmount != null && debitAmount.paise > 0 -> debitAmount to MessageDirection.DEBIT
                creditAmount != null && creditAmount.paise > 0 -> creditAmount to MessageDirection.CREDIT
                else -> null
            }
        }
        val rawAmount = row.getOrNull(columns.amount)?.trim().orEmpty()
        if (rawAmount.isEmpty()) return null
        val isNegative = rawAmount.trimStart().startsWith("-")
        val amount = parseAmount(rawAmount) ?: return null
        if (amount.paise == 0L) return null

        val typeCell = columns.type.takeIf { it >= 0 }?.let { row.getOrNull(it)?.trim()?.lowercase(Locale.ENGLISH) }
        val direction = when {
            typeCell != null && (typeCell.startsWith("dr") || typeCell == "debit") -> MessageDirection.DEBIT
            typeCell != null && (typeCell.startsWith("cr") || typeCell == "credit") -> MessageDirection.CREDIT
            typeCell != null -> return null // an explicit type column we don't recognise - don't guess
            isNegative -> MessageDirection.DEBIT
            else -> MessageDirection.CREDIT
        }
        return amount to direction
    }

    internal fun parseAmount(raw: String): Money? {
        val cleaned = raw.trim().removePrefix("-").replace(",", "").removePrefix("Rs.").removePrefix("Rs")
            .removePrefix("INR").removePrefix("₹").trim()
        if (cleaned.isEmpty()) return null
        return Money.parseRupeeInput(cleaned)
    }

    private val excelEpoch: LocalDate = LocalDate.of(1899, 12, 30)

    /** Text formats first, same as always. A bare integer that falls through every text format
     *  is tried as an Excel date serial (days since 1899-12-30) as a last resort - XLSX/XLS cells
     *  store dates this way, not as text, so [com.budgetmanager.app.domain.ParseStatementExcel]
     *  needs this path; a CSV date column is never a bare 5-digit number in practice, so this
     *  never changes CSV's own behaviour. */
    internal fun parseDate(raw: String): LocalDate? {
        if (raw.isBlank()) return null
        for (formatter in dateFormatters) {
            try {
                return LocalDate.parse(raw, formatter)
            } catch (_: Exception) {
                // try the next format
            }
        }
        val serial = raw.trim().toLongOrNull() ?: return null
        if (serial !in 20000L..60000L) return null // roughly 1954-2064 - a plausible statement date
        return excelEpoch.plusDays(serial)
    }

    /** A minimal CSV splitter: handles double-quoted fields (so a comma inside a quoted
     *  description doesn't split it) without pulling in a full CSV library for one column shape. */
    private fun splitCsvLine(line: String): List<String> {
        val cells = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                c == '"' -> inQuotes = !inQuotes
                c == ',' && !inQuotes -> {
                    cells += current.toString()
                    current.clear()
                }
                else -> current.append(c)
            }
            i++
        }
        cells += current.toString()
        return cells
    }
}
