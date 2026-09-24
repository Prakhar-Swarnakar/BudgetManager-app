package com.budgetmanager.app.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.budgetmanager.app.ui.theme.StatusColors

/**
 * Wraps Compose's swipe-to-dismiss with a start strip (accept, green) and end strip
 * (reject, red). See R28 in 09-risks-and-phases.md.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeRow(
    onSwipeStart: () -> Unit,
    onSwipeEnd: () -> Unit,
    modifier: Modifier = Modifier,
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
                SwipeToDismissBoxValue.StartToEnd -> onSwipeStart()
                SwipeToDismissBoxValue.EndToStart -> onSwipeEnd()
                SwipeToDismissBoxValue.Settled -> Unit
            }
            false
        }
    )

    SwipeToDismissBox(
        state = state,
        modifier = modifier,
        backgroundContent = {
            val color = when (state.dismissDirection) {
                SwipeToDismissBoxValue.StartToEnd -> StatusColors.accepted
                SwipeToDismissBoxValue.EndToStart -> StatusColors.overBudget
                SwipeToDismissBoxValue.Settled -> Color.Transparent
            }
            val icon = when (state.dismissDirection) {
                SwipeToDismissBoxValue.StartToEnd -> Icons.Default.Check
                SwipeToDismissBoxValue.EndToStart -> Icons.Default.Close
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
                    .background(color)
                    .padding(horizontal = 20.dp),
                contentAlignment = alignment
            ) {
                icon?.let { Icon(it, contentDescription = null, tint = Color.White) }
            }
        }
    ) {
        content()
    }
}
