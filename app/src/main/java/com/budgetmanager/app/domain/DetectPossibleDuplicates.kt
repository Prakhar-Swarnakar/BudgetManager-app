package com.budgetmanager.app.domain

import com.budgetmanager.app.core.model.SmsMessage
import java.time.Duration

/**
 * Flags messages that may describe the same real payment as another message - for example a
 * bank's debit alert and a UPI app's own confirmation both arriving for one purchase. Distinct
 * from `sms.DedupeKey`, which catches the exact same SMS counted twice (same sender, same body);
 * this looks for two *different* messages, from different senders, that still look like the same
 * spend (06-backlog.md, "Duplicate detection").
 *
 * Messages of every status are compared, because a pair is only risky once you know what was
 * decided about each half: a still-open message that matches an already Confirmed one is the
 * "counted twice" case. What to show for each pairing is the screen's call (see DuplicateNote).
 */
object DetectPossibleDuplicates {
    private val timeWindow: Duration = Duration.ofMinutes(10)

    /** Maps a flagged message's id to the id of the other message it looks like a duplicate of.
     *  When a message is close to more than one candidate, it points at the closest in time. */
    operator fun invoke(messages: List<SmsMessage>): Map<Long, Long> {
        // Bucketed by amount first: a match requires the two amounts to be exactly equal, so
        // messages of different amounts can never pair up and comparing across buckets is wasted
        // work. Messages runs this over every message it has on each emission, which would be
        // quadratic in the whole history rather than in one amount's worth of it.
        val byAmount = messages
            .filter { it.parsedAmount != null }
            .groupBy { it.parsedAmount }
        val bestMatch = mutableMapOf<Long, Pair<Long, Duration>>()

        for (candidates in byAmount.values) {
            for (i in candidates.indices) {
                for (j in i + 1 until candidates.size) {
                    val a = candidates[i]
                    val b = candidates[j]
                    if (a.sender == b.sender) continue
                    val gap = Duration.between(a.receivedAt, b.receivedAt).abs()
                    if (gap > timeWindow) continue

                    if ((bestMatch[a.id]?.second ?: Duration.ofDays(1)) > gap) bestMatch[a.id] = b.id to gap
                    if ((bestMatch[b.id]?.second ?: Duration.ofDays(1)) > gap) bestMatch[b.id] = a.id to gap
                }
            }
        }

        return bestMatch.mapValues { it.value.first }
    }
}
