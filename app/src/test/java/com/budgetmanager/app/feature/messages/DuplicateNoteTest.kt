package com.budgetmanager.app.feature.messages

import com.budgetmanager.app.core.model.MessageStatus
import com.budgetmanager.app.core.model.MessageStatus.ACCEPTED
import com.budgetmanager.app.core.model.MessageStatus.NOT_ASSIGNED
import com.budgetmanager.app.core.model.MessageStatus.REJECTED
import com.budgetmanager.app.core.model.Money
import com.budgetmanager.app.core.model.SmsMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class DuplicateNoteTest {

    private fun message(sender: String, status: MessageStatus) = SmsMessage(
        id = 0, sender = sender, body = "body", receivedAt = Instant.parse("2026-11-21T06:38:00Z"),
        smsProviderId = null, dedupeKey = sender, parsedAmount = Money.ofRupees(1212), merchant = null,
        paymentMethod = null, suggestedCategoryId = null, status = status, isNew = false
    )

    private fun note(mine: MessageStatus, theirs: MessageStatus) =
        duplicateNote(message("AX-HDFCBK", mine), message("VM-PAYTMB", theirs))

    @Test
    fun `both still to review - confirm one and reject the other`() {
        val n = note(NOT_ASSIGNED, NOT_ASSIGNED)
        assertTrue(n.warn)
        assertEquals("These two look like the same payment", n.heading)
        assertEquals("Confirm one and reject the other.", n.body)
    }

    @Test
    fun `the other is confirmed - says it would be counted twice, with the amount`() {
        val n = note(NOT_ASSIGNED, ACCEPTED)
        assertTrue(n.warn)
        assertEquals("Already confirmed as the message from VM-PAYTMB", n.heading)
        assertEquals("Confirming this one too would count ${Money.ofRupees(1212).formatted()} twice.", n.body)
    }

    @Test
    fun `the other is rejected - a calm note that it is safe to confirm`() {
        val n = note(NOT_ASSIGNED, REJECTED)
        assertFalse(n.warn)
        assertEquals("The matching message from VM-PAYTMB was rejected", n.heading)
        assertEquals("Safe to confirm this one.", n.body)
    }

    @Test
    fun `both confirmed - warns the payment may be counted twice`() {
        val n = note(ACCEPTED, ACCEPTED)
        assertTrue(n.warn)
        assertTrue(n.heading.startsWith("Both are confirmed"))
        assertEquals("Undo this one if it is the same payment.", n.body)
    }

    @Test
    fun `confirmed while the other is still to review - points at the other one`() {
        val n = note(ACCEPTED, NOT_ASSIGNED)
        assertTrue(n.warn)
        assertEquals("Reject that one if it is the same payment.", n.body)
    }

    @Test
    fun `a rejected message and a confirmed one with a rejected match get calm notes`() {
        assertFalse(note(REJECTED, ACCEPTED).warn)
        assertNull(note(REJECTED, ACCEPTED).body)
        assertFalse(note(ACCEPTED, REJECTED).warn)
        assertNull(note(ACCEPTED, REJECTED).body)
    }
}
