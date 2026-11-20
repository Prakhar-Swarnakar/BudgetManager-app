package com.budgetmanager.app.domain

import com.budgetmanager.app.core.model.MessageDirection
import com.budgetmanager.app.core.model.MessageStatus
import com.budgetmanager.app.core.model.Money
import com.budgetmanager.app.core.model.SmsMessage
import com.budgetmanager.app.core.model.StatementLine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class MatchStatementTest {

    private val zone = ZoneId.systemDefault()

    private fun instantOf(date: LocalDate) = date.atStartOfDay(zone).toInstant()

    private fun testMessage(
        id: Long,
        amount: Money?,
        date: LocalDate,
        direction: MessageDirection = MessageDirection.DEBIT,
        status: MessageStatus = MessageStatus.NOT_ASSIGNED
    ) = SmsMessage(
        id = id, sender = "TESTBANK", body = "test", receivedAt = instantOf(date),
        smsProviderId = null, dedupeKey = "k$id", parsedAmount = amount, merchant = "Test",
        suggestedCategoryId = null, status = status, isNew = false, direction = direction
    )

    private fun testLine(amount: Money, date: LocalDate, direction: MessageDirection = MessageDirection.DEBIT) =
        StatementLine(date = date, description = "Test line", amount = amount, direction = direction)

    @Test
    fun `an exact amount+direction+date match to a Not assigned message lands in needsReview`() {
        val date = LocalDate.of(2026, 9, 5)
        val line = testLine(Money.ofRupees(3000), date)
        val message = testMessage(1, Money.ofRupees(3000), date)

        val result = MatchStatement(listOf(line), listOf(message))

        assertEquals(1, result.needsReview.size)
        assertEquals(message.id, result.needsReview.first().message.id)
        assertTrue(result.missing.isEmpty())
    }

    @Test
    fun `a match to a Rejected message lands in rejectedButOnStatement`() {
        val date = LocalDate.of(2026, 9, 5)
        val line = testLine(Money.ofRupees(5000), date)
        val message = testMessage(1, Money.ofRupees(5000), date, status = MessageStatus.REJECTED)

        val result = MatchStatement(listOf(line), listOf(message))

        assertEquals(1, result.rejectedButOnStatement.size)
        assertTrue(result.needsReview.isEmpty())
    }

    @Test
    fun `a match to an already Accepted message is matched silently, not re-flagged`() {
        val date = LocalDate.of(2026, 9, 5)
        val line = testLine(Money.ofRupees(3000), date)
        val message = testMessage(1, Money.ofRupees(3000), date, status = MessageStatus.ACCEPTED)

        val result = MatchStatement(listOf(line), listOf(message))

        assertTrue(result.needsReview.isEmpty())
        assertTrue(result.rejectedButOnStatement.isEmpty())
        assertTrue(result.missing.isEmpty())
        assertTrue(result.possibleFalsePositives.isEmpty())
    }

    @Test
    fun `a statement line with no matching message at all is missing`() {
        val line = testLine(Money.ofRupees(590), LocalDate.of(2026, 9, 1))

        val result = MatchStatement(listOf(line), emptyList())

        assertEquals(1, result.missing.size)
    }

    @Test
    fun `a date more than the tolerance away never matches, even with the same amount`() {
        val line = testLine(Money.ofRupees(3000), LocalDate.of(2026, 9, 1))
        val message = testMessage(1, Money.ofRupees(3000), LocalDate.of(2026, 9, 10))

        val result = MatchStatement(listOf(line), listOf(message), dateToleranceDays = 3)

        assertEquals(1, result.missing.size)
        assertTrue(result.needsReview.isEmpty())
    }

    @Test
    fun `a date within the tolerance still matches - settlement commonly lags a day or two`() {
        val line = testLine(Money.ofRupees(3000), LocalDate.of(2026, 9, 1))
        val message = testMessage(1, Money.ofRupees(3000), LocalDate.of(2026, 9, 3))

        val result = MatchStatement(listOf(line), listOf(message), dateToleranceDays = 3)

        assertEquals(1, result.needsReview.size)
    }

    @Test
    fun `a small amount difference within tolerance is an amount mismatch, not missing`() {
        val date = LocalDate.of(2026, 9, 5)
        val line = testLine(Money.ofRupees(500), date)
        val message = testMessage(1, Money(49800), date) // Rs 498.00 - Rs 2 off

        val result = MatchStatement(listOf(line), listOf(message))

        assertEquals(1, result.amountMismatches.size)
        assertTrue(result.missing.isEmpty())
    }

    @Test
    fun `a large amount difference beyond tolerance is missing, not an amount mismatch`() {
        val date = LocalDate.of(2026, 9, 5)
        val line = testLine(Money.ofRupees(500), date)
        val message = testMessage(1, Money.ofRupees(50), date) // way off

        val result = MatchStatement(listOf(line), listOf(message))

        assertEquals(1, result.missing.size)
        assertTrue(result.amountMismatches.isEmpty())
    }

    @Test
    fun `an Accepted message with no matching statement line is a possible false positive`() {
        val message = testMessage(
            1, Money.ofRupees(310), LocalDate.of(2026, 9, 5), status = MessageStatus.ACCEPTED
        )

        val result = MatchStatement(emptyList(), listOf(message))

        assertEquals(1, result.possibleFalsePositives.size)
    }

    @Test
    fun `a Not assigned message with no matching statement line is never flagged as a false positive`() {
        val message = testMessage(1, Money.ofRupees(310), LocalDate.of(2026, 9, 5))

        val result = MatchStatement(emptyList(), listOf(message))

        assertTrue(result.possibleFalsePositives.isEmpty())
    }

    @Test
    fun `a message with no parsed amount is never matched against anything`() {
        val date = LocalDate.of(2026, 9, 5)
        val line = testLine(Money.ofRupees(3000), date)
        val message = testMessage(1, amount = null, date = date)

        val result = MatchStatement(listOf(line), listOf(message))

        assertEquals(1, result.missing.size)
        assertTrue(result.needsReview.isEmpty())
    }

    @Test
    fun `direction must match - a credit line never matches a debit message at the same amount and date`() {
        val date = LocalDate.of(2026, 9, 5)
        val line = testLine(Money.ofRupees(3000), date, direction = MessageDirection.CREDIT)
        val message = testMessage(1, Money.ofRupees(3000), date, direction = MessageDirection.DEBIT)

        val result = MatchStatement(listOf(line), listOf(message))

        assertEquals(1, result.missing.size)
    }

    @Test
    fun `coverage percent and debit totals are computed correctly`() {
        val date = LocalDate.of(2026, 9, 5)
        val matchedLine = testLine(Money.ofRupees(3000), date)
        val missingLine = testLine(Money.ofRupees(590), date)
        val matchedMessage = testMessage(1, Money.ofRupees(3000), date, status = MessageStatus.ACCEPTED)
        val unrelatedAcceptedDebit = testMessage(
            2, Money.ofRupees(1000), date, status = MessageStatus.ACCEPTED
        )

        val result = MatchStatement(listOf(matchedLine, missingLine), listOf(matchedMessage, unrelatedAcceptedDebit))

        assertEquals(50, result.coveragePercent) // 1 of 2 lines matched
        assertEquals(Money.ofRupees(3590), result.statementDebitTotal)
        // In-app total is every Accepted debit in range, not just the matched ones.
        assertEquals(Money.ofRupees(4000), result.inAppDebitTotal)
    }

    @Test
    fun `an empty statement is 100 percent coverage, not a division by zero`() {
        val result = MatchStatement(emptyList(), emptyList())
        assertEquals(100, result.coveragePercent)
    }
}
