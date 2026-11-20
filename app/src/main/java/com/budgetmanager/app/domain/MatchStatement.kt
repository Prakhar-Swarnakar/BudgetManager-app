package com.budgetmanager.app.domain

import com.budgetmanager.app.core.model.MessageDirection
import com.budgetmanager.app.core.model.MessageStatus
import com.budgetmanager.app.core.model.Money
import com.budgetmanager.app.core.model.SmsMessage
import com.budgetmanager.app.core.model.StatementLine
import java.time.ZoneId
import kotlin.math.abs

data class StatementMatch(val line: StatementLine, val message: SmsMessage)

data class StatementMatchResult(
    /** Matched a real message, but you haven't reviewed it yet. */
    val needsReview: List<StatementMatch>,
    /** Matched a message you rejected - worth a second look either way, since the statement
     *  shows it really happened. */
    val rejectedButOnStatement: List<StatementMatch>,
    /** On the statement, no matching message at all - SMS detection missed it completely. */
    val missing: List<StatementLine>,
    /** Matched by date and direction, but the amount differs by more than a rounding tolerance -
     *  a tip, fee, or partial refund folded into one line is the common real cause. */
    val amountMismatches: List<StatementMatch>,
    /** A message you Accepted as a real spend, with nothing matching it on the statement - a
     *  possible false positive (a duplicate SMS, a pre-notice counted as real, a reversed
     *  authorization). */
    val possibleFalsePositives: List<SmsMessage>,
    val statementDebitTotal: Money,
    val inAppDebitTotal: Money,
    val coveragePercent: Int
)

/**
 * Compares a real statement against what the SMS pipeline actually caught (06-backlog.md,
 * "Credit card / bank statement import") - a validator, not a second way to add transactions.
 * Matching is deliberately simple: amount (exact) + direction + date within [dateToleranceDays]
 * (credit card settlement commonly lags the SMS by a day or two), closest-amount-within-date-
 * window picked when more than one message could match. [messagesInRange] should already be
 * scoped to the statement's own date range (±[dateToleranceDays]) by the caller - this function
 * doesn't re-derive that range itself, since it has no notion of "the statement's period" beyond
 * the lines it's given.
 */
object MatchStatement {

    private const val AMOUNT_MISMATCH_TOLERANCE_PAISE = 500L // ₹5 - a tip/rounding-shaped gap, not a missed amount

    operator fun invoke(
        statementLines: List<StatementLine>,
        messagesInRange: List<SmsMessage>,
        dateToleranceDays: Long = 3
    ): StatementMatchResult {
        val zone = ZoneId.systemDefault()
        val usedMessageIds = mutableSetOf<Long>()
        val needsReview = mutableListOf<StatementMatch>()
        val rejectedButOnStatement = mutableListOf<StatementMatch>()
        val missing = mutableListOf<StatementLine>()
        val amountMismatches = mutableListOf<StatementMatch>()

        statementLines.forEach { line ->
            val candidates = messagesInRange.filter { message ->
                message.id !in usedMessageIds &&
                    message.direction == line.direction &&
                    message.parsedAmount != null &&
                    abs(java.time.temporal.ChronoUnit.DAYS.between(
                        message.receivedAt.atZone(zone).toLocalDate(), line.date
                    )) <= dateToleranceDays
            }

            val exact = candidates.firstOrNull { it.parsedAmount == line.amount }
            if (exact != null) {
                usedMessageIds += exact.id
                when (exact.status) {
                    MessageStatus.NOT_ASSIGNED -> needsReview += StatementMatch(line, exact)
                    MessageStatus.REJECTED -> rejectedButOnStatement += StatementMatch(line, exact)
                    MessageStatus.ACCEPTED -> Unit // matched and already trusted - nothing to flag
                }
                return@forEach
            }

            val closest = candidates.minByOrNull { abs(it.parsedAmount!!.paise - line.amount.paise) }
            if (closest != null && abs(closest.parsedAmount!!.paise - line.amount.paise) <= AMOUNT_MISMATCH_TOLERANCE_PAISE) {
                usedMessageIds += closest.id
                amountMismatches += StatementMatch(line, closest)
            } else {
                missing += line
            }
        }

        val possibleFalsePositives = messagesInRange.filter {
            it.status == MessageStatus.ACCEPTED && it.id !in usedMessageIds
        }

        val statementDebitTotal = statementLines
            .filter { it.direction == MessageDirection.DEBIT }
            .fold(Money.Zero) { acc, line -> acc + line.amount }
        val inAppDebitTotal = messagesInRange
            .filter { it.status == MessageStatus.ACCEPTED && it.direction == MessageDirection.DEBIT }
            .fold(Money.Zero) { acc, message -> acc + (message.parsedAmount ?: Money.Zero) }

        val coveragePercent = if (statementLines.isEmpty()) {
            100
        } else {
            ((statementLines.size - missing.size) * 100) / statementLines.size
        }

        return StatementMatchResult(
            needsReview = needsReview,
            rejectedButOnStatement = rejectedButOnStatement,
            missing = missing,
            amountMismatches = amountMismatches,
            possibleFalsePositives = possibleFalsePositives,
            statementDebitTotal = statementDebitTotal,
            inAppDebitTotal = inAppDebitTotal,
            coveragePercent = coveragePercent
        )
    }
}
