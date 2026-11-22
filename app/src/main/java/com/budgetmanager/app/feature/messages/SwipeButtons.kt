package com.budgetmanager.app.feature.messages

import com.budgetmanager.app.core.model.MessageStatus

/** The one button a swipe reveals. The tap on it is what acts; the swipe itself never does. */
enum class SwipeButton(val label: String) { CONFIRM("Confirm"), REJECT("Reject"), UNDO("Undo") }

/** [onSwipeRight] shows on the left after swiping right, [onSwipeLeft] on the right after swiping
 *  left. A null side has nothing to reveal, so the row will not slide that way. */
data class SwipeButtons(val onSwipeRight: SwipeButton?, val onSwipeLeft: SwipeButton?)

fun swipeButtonsFor(status: MessageStatus): SwipeButtons = when (status) {
    MessageStatus.NOT_ASSIGNED -> SwipeButtons(SwipeButton.CONFIRM, SwipeButton.REJECT)
    MessageStatus.ACCEPTED -> SwipeButtons(null, SwipeButton.UNDO)
    MessageStatus.REJECTED -> SwipeButtons(SwipeButton.UNDO, null)
}
