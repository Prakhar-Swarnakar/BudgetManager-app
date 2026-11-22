package com.budgetmanager.app.feature.messages

import com.budgetmanager.app.core.model.MessageStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SwipeButtonsTest {

    @Test
    fun `To review has Confirm on a right swipe and Reject on a left swipe`() {
        val buttons = swipeButtonsFor(MessageStatus.NOT_ASSIGNED)
        assertEquals(SwipeButton.CONFIRM, buttons.onSwipeRight)
        assertEquals(SwipeButton.REJECT, buttons.onSwipeLeft)
    }

    @Test
    fun `Confirmed can only be undone, by a left swipe`() {
        val buttons = swipeButtonsFor(MessageStatus.ACCEPTED)
        assertNull(buttons.onSwipeRight)
        assertEquals(SwipeButton.UNDO, buttons.onSwipeLeft)
    }

    @Test
    fun `Rejected can only be undone, by a right swipe`() {
        val buttons = swipeButtonsFor(MessageStatus.REJECTED)
        assertEquals(SwipeButton.UNDO, buttons.onSwipeRight)
        assertNull(buttons.onSwipeLeft)
    }

    @Test
    fun `the button labels are the words on screen`() {
        assertEquals(listOf("Confirm", "Reject", "Undo"), SwipeButton.entries.map { it.label })
    }
}
