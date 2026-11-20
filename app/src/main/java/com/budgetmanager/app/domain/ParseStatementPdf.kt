package com.budgetmanager.app.domain

import com.budgetmanager.app.core.model.MessageDirection
import com.budgetmanager.app.core.model.StatementLine
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.encryption.InvalidPasswordException
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.ByteArrayInputStream

sealed interface StatementPdfResult {
    data class Success(val lines: List<StatementLine>, val skippedRowCount: Int) : StatementPdfResult
    data class Failure(val reason: String) : StatementPdfResult
    /** The PDF is encrypted and no password (or the wrong one) was supplied - the caller should
     *  prompt for one and retry rather than treat this as a dead end. */
    data object PasswordRequired : StatementPdfResult
}

/**
 * Reads a bank/credit-card statement PDF into [StatementLine]s. PDF text extraction loses table
 * structure, so unlike CSV/Excel this can't reconstruct a Date/Debit/Credit column layout - it
 * instead looks for lines shaped like a single transaction: a date, a description, an amount, and
 * an explicit debit/credit marker (D/C, Dr/Cr) at the end of the line. This matches statements
 * that print one transaction per line with its own direction marker (confirmed against a real SBI
 * Card PDF statement); a statement that only prints separate Withdrawal/Deposit columns with no
 * per-line marker won't match - rather than guess which column a number belongs to, those lines
 * are skipped and counted, same "never silently dropped" rule the CSV reader already follows.
 */
object ParseStatementPdf {

    private val dateTokenPattern = """\d{1,2}[\s/\-](?:[A-Za-z]{3,9}|\d{1,2})[\s/\-]\d{2,4}"""
    private val dateLikeRegex = Regex(dateTokenPattern)
    private val transactionLineRegex = Regex(
        """^(?:\d+\s+)?($dateTokenPattern)\s+(.+?)\s+([\d,]+\.\d{1,2})\s+(D|C|Dr|Cr|Debit|Credit)\.?$""",
        RegexOption.IGNORE_CASE
    )

    operator fun invoke(bytes: ByteArray, password: String? = null): StatementPdfResult {
        val document = try {
            PDDocument.load(ByteArrayInputStream(bytes), password ?: "")
        } catch (e: InvalidPasswordException) {
            return if (password == null) {
                StatementPdfResult.PasswordRequired
            } else {
                StatementPdfResult.Failure("That password didn't open the file.")
            }
        } catch (e: Exception) {
            return StatementPdfResult.Failure("Couldn't read that PDF: ${e.message ?: "unknown error"}.")
        }

        val text = try {
            document.use { PDFTextStripper().apply { sortByPosition = true }.getText(it) }
        } catch (e: Exception) {
            return StatementPdfResult.Failure("Couldn't extract text from that PDF: ${e.message ?: "unknown error"}.")
        }

        val candidateLines = text.lines().map { it.trim() }.filter { it.isNotBlank() }
        var skipped = 0
        val lines = mutableListOf<StatementLine>()
        var sawAnyDateLikeLine = false

        for (rawLine in candidateLines) {
            val match = transactionLineRegex.find(rawLine)
            if (match == null) {
                if (dateLikeRegex.containsMatchIn(rawLine)) sawAnyDateLikeLine = true
                continue
            }
            sawAnyDateLikeLine = true
            val (dateText, description, amountText, markerText) = match.destructured
            val date = ParseStatementCsv.parseDate(dateText.trim())
            val amount = ParseStatementCsv.parseAmount(amountText)
            if (date == null || amount == null || amount.paise == 0L) {
                skipped++
                continue
            }
            val direction = when (markerText.lowercase()) {
                "d", "dr", "debit" -> MessageDirection.DEBIT
                "c", "cr", "credit" -> MessageDirection.CREDIT
                else -> null
            }
            if (direction == null) {
                skipped++
                continue
            }
            lines += StatementLine(date = date, description = description.trim(), amount = amount, direction = direction)
        }

        if (lines.isEmpty()) {
            return StatementPdfResult.Failure(
                if (sawAnyDateLikeLine) {
                    "Found $skipped date-like line(s) but none matched a readable " +
                        "\"date, description, amount, D/C\" transaction shape."
                } else {
                    "Couldn't find any transaction lines in this PDF - it may use a table layout " +
                        "this reader doesn't support yet."
                }
            )
        }
        return StatementPdfResult.Success(lines, skipped)
    }
}
