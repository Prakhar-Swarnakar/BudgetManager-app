package com.budgetmanager.app.domain

import com.budgetmanager.app.core.model.MessageStatus
import com.budgetmanager.app.core.model.Money
import com.budgetmanager.app.core.model.SmsMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class DetectPossibleDuplicatesTest {

    private fun message(
        id: Long,
        sender: String,
        amount: Long?,
        receivedAt: Instant,
        status: MessageStatus = MessageStatus.NOT_ASSIGNED
    ) = SmsMessage(
        id = id, sender = sender, body = "body", receivedAt = receivedAt, smsProviderId = null,
        dedupeKey = "k$id", parsedAmount = amount?.let { Money(it) }, merchant = null,
        paymentMethod = null, suggestedCategoryId = null, status = status, isNew = false
    )

    private val t0 = Instant.parse("2026-10-01T19:39:00Z")

    @Test
    fun `a bank alert and a UPI app confirmation for the same payment are both flagged`() {
        val bank = message(1, "AX-SBICRD-S", amount = 149900, receivedAt = t0)
        val upiApp = message(2, "VM-GOOGLEPAY", amount = 149900, receivedAt = t0.plusSeconds(90))

        val result = DetectPossibleDuplicates(listOf(bank, upiApp))

        assertEquals(2L, result[1L])
        assertEquals(1L, result[2L])
    }

    @Test
    fun `different amounts are never flagged`() {
        val a = message(1, "AX-SBICRD-S", amount = 100000, receivedAt = t0)
        val b = message(2, "VM-GOOGLEPAY", amount = 150000, receivedAt = t0)

        assertTrue(DetectPossibleDuplicates(listOf(a, b)).isEmpty())
    }

    @Test
    fun `the same sender is never flagged - that's a real second transaction or the dedupe key's job`() {
        val a = message(1, "AX-SBICRD-S", amount = 100000, receivedAt = t0)
        val b = message(2, "AX-SBICRD-S", amount = 100000, receivedAt = t0.plusSeconds(30))

        assertTrue(DetectPossibleDuplicates(listOf(a, b)).isEmpty())
    }

    @Test
    fun `outside the time window is not flagged`() {
        val a = message(1, "AX-SBICRD-S", amount = 100000, receivedAt = t0)
        val b = message(2, "VM-GOOGLEPAY", amount = 100000, receivedAt = t0.plusSeconds(11 * 60))

        assertTrue(DetectPossibleDuplicates(listOf(a, b)).isEmpty())
    }

    @Test
    fun `an already-accepted or rejected message is never flagged`() {
        val accepted = message(1, "AX-SBICRD-S", amount = 100000, receivedAt = t0, status = MessageStatus.ACCEPTED)
        val notAssigned = message(2, "VM-GOOGLEPAY", amount = 100000, receivedAt = t0)

        assertTrue(DetectPossibleDuplicates(listOf(accepted, notAssigned)).isEmpty())
    }

    @Test
    fun `a message with no parsed amount is never flagged`() {
        val unparsed = message(1, "AX-SBICRD-S", amount = null, receivedAt = t0)
        val other = message(2, "VM-GOOGLEPAY", amount = 100000, receivedAt = t0)

        assertTrue(DetectPossibleDuplicates(listOf(unparsed, other)).isEmpty())
    }

    @Test
    fun `a message matches its closest candidate when more than one is in range`() {
        val target = message(1, "AX-SBICRD-S", amount = 100000, receivedAt = t0)
        val far = message(2, "VM-GOOGLEPAY", amount = 100000, receivedAt = t0.plusSeconds(500))
        val close = message(3, "VK-PHONEPE", amount = 100000, receivedAt = t0.plusSeconds(30))

        val result = DetectPossibleDuplicates(listOf(target, far, close))

        assertEquals(3L, result[1L])
    }
}
