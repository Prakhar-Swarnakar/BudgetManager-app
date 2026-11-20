package com.budgetmanager.app.domain

import com.budgetmanager.app.core.model.MessageDirection
import com.budgetmanager.app.core.model.Money
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ParseStatementCsvTest {

    @Test
    fun `separate debit and credit columns are read correctly`() {
        val csv = """
            Date,Narration,Debit,Credit,Balance
            05/09/2026,INVESCO SIP,3000.00,,50000.00
            20/09/2026,Salary,,45000.00,95000.00
        """.trimIndent()

        val result = ParseStatementCsv(csv) as StatementCsvResult.Success
        assertEquals(2, result.lines.size)
        assertEquals(LocalDate.of(2026, 9, 5), result.lines[0].date)
        assertEquals(Money.ofRupees(3000), result.lines[0].amount)
        assertEquals(MessageDirection.DEBIT, result.lines[0].direction)
        assertEquals("INVESCO SIP", result.lines[0].description)
        assertEquals(Money.ofRupees(45000), result.lines[1].amount)
        assertEquals(MessageDirection.CREDIT, result.lines[1].direction)
    }

    @Test
    fun `a single Amount column with a Dr-Cr type column is read correctly`() {
        val csv = """
            Transaction Date,Particulars,Amount,Type
            06-Sep-2026,JM Flexi Cap SIP,2999.85,Dr
            07-Sep-2026,Interest credit,50.00,Cr
        """.trimIndent()

        val result = ParseStatementCsv(csv) as StatementCsvResult.Success
        assertEquals(2, result.lines.size)
        assertEquals(LocalDate.of(2026, 9, 6), result.lines[0].date)
        assertEquals(MessageDirection.DEBIT, result.lines[0].direction)
        assertEquals(MessageDirection.CREDIT, result.lines[1].direction)
    }

    @Test
    fun `a single signed Amount column with no type column uses the sign for direction`() {
        val csv = """
            Date,Description,Amount
            05/09/2026,SIP instalment,-3000.00
            20/09/2026,Refund,500.00
        """.trimIndent()

        val result = ParseStatementCsv(csv) as StatementCsvResult.Success
        assertEquals(MessageDirection.DEBIT, result.lines[0].direction)
        assertEquals(Money.ofRupees(3000), result.lines[0].amount)
        assertEquals(MessageDirection.CREDIT, result.lines[1].direction)
    }

    @Test
    fun `metadata rows before the real header are skipped`() {
        val csv = """
            Account Statement
            Account No: XXXXXXXX663
            Period: 01-Sep-2026 to 30-Sep-2026

            Date,Narration,Debit,Credit,Balance
            05/09/2026,SIP,3000.00,,50000.00
        """.trimIndent()

        val result = ParseStatementCsv(csv) as StatementCsvResult.Success
        assertEquals(1, result.lines.size)
    }

    @Test
    fun `a header more than 10 rows down (a long search-criteria preamble) is still found`() {
        // Confirmed against a real ICICI statement export: an 11-row search-criteria block
        // ("DETAILED STATEMENT", "Search", "Account Number", "Transaction Date from", …) pushed
        // the real header to row 12 - this used to be missed entirely by a 10-row scan window.
        val preamble = (1..11).joinToString("\n") { "Metadata row $it" }
        val csv = """
            $preamble
            Date,Narration,Debit,Credit,Balance
            05/09/2026,SIP,3000.00,,50000.00
        """.trimIndent()

        val result = ParseStatementCsv(csv) as StatementCsvResult.Success
        assertEquals(1, result.lines.size)
    }

    @Test
    fun `a quoted description containing a comma is read as one field`() {
        val csv = """
            Date,Narration,Debit,Credit,Balance
            05/09/2026,"Payment, via UPI",3000.00,,50000.00
        """.trimIndent()

        val result = ParseStatementCsv(csv) as StatementCsvResult.Success
        assertEquals("Payment, via UPI", result.lines[0].description)
    }

    @Test
    fun `a row with an unreadable date is skipped and counted, not silently dropped`() {
        val csv = """
            Date,Narration,Debit,Credit,Balance
            05/09/2026,Good row,3000.00,,50000.00
            not-a-date,Bad row,100.00,,49900.00
        """.trimIndent()

        val result = ParseStatementCsv(csv) as StatementCsvResult.Success
        assertEquals(1, result.lines.size)
        assertEquals(1, result.skippedRowCount)
    }

    @Test
    fun `a file with no Date or Amount-shaped header anywhere fails clearly`() {
        val csv = """
            Name,Address,Phone
            Prakhar,Bangalore,9999999999
        """.trimIndent()

        val result = ParseStatementCsv(csv)
        assertTrue(result is StatementCsvResult.Failure)
    }

    @Test
    fun `an empty file fails clearly`() {
        assertTrue(ParseStatementCsv("") is StatementCsvResult.Failure)
    }
}
