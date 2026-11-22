package com.budgetmanager.app.feature.messages.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** Which way a row is slid open. */
enum class RevealSide { SwipedRight, SwipedLeft }

/** The button a swipe reveals. [onClick] is the only thing that acts. */
class RevealAction(
    val label: String,
    val icon: ImageVector,
    val color: Color,
    val onClick: () -> Unit
)

private val RevealWidth = 104.dp

/**
 * A row that slides open to show one button and then waits for a tap, instead of acting when the
 * swipe passes a threshold. Which side is open is owned by the caller ([open]), so the caller can
 * keep one row open at a time and close it on scroll or after a timeout. A side with a null action
 * will not slide that way. TalkBack gets the same actions as custom actions, so nothing depends on
 * swiping.
 */
@Composable
fun RevealSwipeRow(
    swipeRightAction: RevealAction?,
    swipeLeftAction: RevealAction?,
    open: RevealSide?,
    onOpenChange: (RevealSide?) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val widthPx = with(LocalDensity.current) { RevealWidth.toPx() }
    val offset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    fun targetFor(side: RevealSide?) = when (side) {
        RevealSide.SwipedRight -> widthPx
        RevealSide.SwipedLeft -> -widthPx
        null -> 0f
    }

    // Follows the caller: closing by timeout, by scroll, or by another row opening all land here.
    LaunchedEffect(open, widthPx) { offset.animateTo(targetFor(open), tween(180)) }

    val minOffset = if (swipeLeftAction != null) -widthPx else 0f
    val maxOffset = if (swipeRightAction != null) widthPx else 0f
    val dragState = rememberDraggableState { delta ->
        // UNDISPATCHED so each step lands before the next one, and before onDragStopped reads it.
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            offset.snapTo((offset.value + delta).coerceIn(minOffset, maxOffset))
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                customActions = listOfNotNull(swipeRightAction, swipeLeftAction).map { action ->
                    CustomAccessibilityAction(action.label) { action.onClick(); true }
                }
            }
    ) {
        // Behind the row; the row's own opaque background hides them until it slides away.
        Row(modifier = Modifier.matchParentSize().clearAndSetSemantics { }) {
            if (swipeRightAction != null) ActionPanel(swipeRightAction) { onOpenChange(null) }
            Spacer(Modifier.weight(1f))
            if (swipeLeftAction != null) ActionPanel(swipeLeftAction) { onOpenChange(null) }
        }
        Box(
            modifier = Modifier
                .offset { IntOffset(offset.value.roundToInt(), 0) }
                .draggable(
                    state = dragState,
                    orientation = Orientation.Horizontal,
                    onDragStopped = {
                        val side = when {
                            offset.value >= widthPx / 2 -> RevealSide.SwipedRight
                            offset.value <= -widthPx / 2 -> RevealSide.SwipedLeft
                            else -> null
                        }
                        onOpenChange(side)
                        offset.animateTo(targetFor(side), tween(180))
                    }
                )
        ) {
            content()
        }
    }
}

/** Darkened so white text on it keeps enough contrast. */
@Composable
private fun ActionPanel(action: RevealAction, onDone: () -> Unit) {
    Column(
        modifier = Modifier
            .width(RevealWidth)
            .fillMaxHeight()
            .background(lerp(action.color, Color.Black, 0.25f))
            .clickable {
                action.onClick()
                onDone()
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(action.icon, contentDescription = null, tint = Color.White)
        Text(
            action.label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )
    }
}
