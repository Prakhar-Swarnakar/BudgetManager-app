package com.budgetmanager.app.domain

import com.budgetmanager.app.core.model.MessageStatus
import com.budgetmanager.app.core.model.SmsMessage
import java.time.Duration

/**
 * Flags messages that may describe the same real payment as another message - for example a
 * bank's debit alert and a UPI app's own confirmation both arriving for one purchase. Distinct
 * from `sms.DedupeKey`, which catches the exact same SMS counted twice (same sender, same body);
 * this looks for two *different* messages, from different senders, that still look like the same
 * spend (06-backlog.md, "Duplicate detection").
 *
 * Only Not assigned messages are considered - once a message has been Accepted or Rejected the
 * user has already decided, and flagging it again is just noise.
 */
object DetectPossibleDuplicates {
    private val timeWindow: Duration = Duration.ofMinutes(10)

    /** Maps a flagged message's id to the id of the other message it looks like a duplicate of.
     *  When a message is close to more than one candidate, it points at the closest in time. */
    operator fun invoke(messages: List<SmsMessage>): Map<Long, Long> {
        val candidates = messages.filter { it.status == MessageStatus.NOT_ASSIGNED && it.parsedAmount != null }
        val bestMatch = mutableMapOf<Long, Pair<Long, Duration>>()

        for (i in candidates.indices) {
            for (j in i + 1 until candidates.size) {
                val a = candidates[i]
                val b = candidates[j]
                if (a.sender == b.sender) continue
                if (a.parsedAmount != b.parsedAmount) continue
                val gap = Duration.between(a.receivedAt, b.receivedAt).abs()
                if (gap > timeWindow) continue

                if ((bestMatch[a.id]?.second ?: Duration.ofDays(1)) > gap) bestMatch[a.id] = b.id to gap
                if ((bestMatch[b.id]?.second ?: Duration.ofDays(1)) > gap) bestMatch[b.id] = a.id to gap
            }
        }

        return bestMatch.mapValues { it.value.first }
    }
}
