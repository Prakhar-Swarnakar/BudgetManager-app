package com.budgetmanager.app.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.budgetmanager.app.ui.theme.StatusColors

/** What swiping in one direction will actually do, for the reveal strip. A null [icon] with
 *  [enabled] = false shows a muted, icon-less strip - used when that direction is currently a
 *  no-op, so the swipe never visually promises an action ("accept") it won't perform. */
data class SwipeAction(
    val icon: ImageVector?,
    val color: Color,
    val enabled: Boolean = true
)

/**
 * Wraps Compose's swipe-to-dismiss with configurable reveal strips for each direction, so the
 * colour/icon always matches what the swipe will really do (e.g. green+check only when it
 * actually accepts; a neutral "undo" when it just reverts to Not assigned) rather than always
 * showing the same accept/reject look regardless of outcome. See R28 in 09-risks-and-phases.md.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeRow(
    onSwipeStart: () -> Unit,
    onSwipeEnd: () -> Unit,
    modifier: Modifier = Modifier,
    startAction: SwipeAction = SwipeAction(Icons.Default.Check, StatusColors.accepted),
    endAction: SwipeAction = SwipeAction(Icons.Default.Close, StatusColors.overBudget),
    content: @Composable () -> Unit
) {
    // Always veto the dismiss (return false): the row must never actually settle in the
    // "dismissed" position, since accepting/rejecting changes the row's status rather than
    // removing it from the list. Returning false makes the box auto-snap back to Settled on
    // its own; a manual reset() call here was racy and could leave the full-strength swipe
    // background stuck on screen instead of the (correctly tinted) row content.
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> if (startAction.enabled) onSwipeStart()
                SwipeToDismissBoxValue.EndToStart -> if (endAction.enabled) onSwipeEnd()
                SwipeToDismissBoxValue.Settled -> Unit
            }
            false
        }
    )

    SwipeToDismissBox(
        state = state,
        modifier = modifier,
        backgroundContent = {
            val action = when (state.dismissDirection) {
                SwipeToDismissBoxValue.StartToEnd -> startAction
                SwipeToDismissBoxValue.EndToStart -> endAction
                SwipeToDismissBoxValue.Settled -> null
            }
            val alignment = if (state.dismissDirection == SwipeToDismissBoxValue.StartToEnd) {
                Alignment.CenterStart
            } else {
                Alignment.CenterEnd
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(action?.color ?: Color.Transparent)
                    .padding(horizontal = 20.dp),
                contentAlignment = alignment
            ) {
                action?.icon?.let { Icon(it, contentDescription = null, tint = Color.White) }
            }
        }
    ) {
        content()
    }
}

/** The neutral "this swipe just reverts to Not assigned" strip, shared by Accepted and
 *  Rejected rows in Messages. */
@Composable
fun revertSwipeAction(): SwipeAction =
    SwipeAction(Icons.Default.Undo, MaterialTheme.colorScheme.outline)

/** No visible action for this direction on this row - swiping it does nothing. */
val NoSwipeAction = SwipeAction(icon = null, color = Color.Transparent, enabled = false)
