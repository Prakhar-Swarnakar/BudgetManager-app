package com.budgetmanager.app.feature.budget.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.budgetmanager.app.feature.budget.BudgetRowUi
import kotlin.math.roundToInt

/**
 * A plain `Column`, not `LazyColumn`: category counts stay small for a single-user app, and this
 * trades list virtualisation for a much simpler drag implementation. Reordering happens entirely
 * in local state and only touches the repository once, on drag end - so a drag never has to
 * round-trip through Room's `Flow` mid-gesture.
 *
 * Same drag mechanics as the removed Categories page's list, minus the swipe-to-archive gesture
 * that used to share the row with the drag handle (archiving was dropped from the app, so
 * there's no longer a second gesture on this row to conflict with).
 */
@Composable
fun BudgetCategoryList(
    rows: List<BudgetRowUi>,
    onRowClick: (Long) -> Unit,
    onReorder: (List<Long>) -> Unit,
    modifier: Modifier = Modifier
) {
    var localOrder by remember(rows) { mutableStateOf(rows) }
    var rowHeightPx by remember { mutableStateOf(0f) }
    var draggedId by remember { mutableStateOf<Long?>(null) }
    var dragOffset by remember { mutableStateOf(0f) }

    Column(modifier = modifier) {
        localOrder.forEach { row ->
            // key() makes Compose match each row by categoryId instead of by its position in
            // the forEach. Without it, a mid-drag reorder recomposes whichever SLOT the dragged
            // row started in with a DIFFERENT row (since other rows shifted under it), which has
            // the same effect as the id passed to pointerInput() changing: the gesture's
            // coroutine gets cancelled outright.
            key(row.categoryId) {
                val isDragged = row.categoryId == draggedId
                Box(
                    modifier = Modifier
                        .zIndex(if (isDragged) 1f else 0f)
                        .graphicsLayer { translationY = if (isDragged) dragOffset else 0f }
                        .onGloballyPositioned { coordinates ->
                            if (rowHeightPx == 0f) rowHeightPx = coordinates.size.height.toFloat()
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onRowClick(row.categoryId) }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.DragHandle,
                            contentDescription = "Drag to reorder",
                            modifier = Modifier.pointerInput(row.categoryId) {
                                detectDragGestures(
                                    onDragStart = {
                                        draggedId = row.categoryId
                                        dragOffset = 0f
                                    },
                                    onDragEnd = {
                                        draggedId = null
                                        dragOffset = 0f
                                        onReorder(localOrder.map { it.categoryId })
                                    },
                                    onDragCancel = {
                                        draggedId = null
                                        dragOffset = 0f
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        dragOffset += dragAmount.y
                                        val rowHeight = rowHeightPx
                                        val id = draggedId
                                        if (rowHeight > 0f && id != null) {
                                            // Resolved by id, not by the index captured when
                                            // this gesture started - the row's position can
                                            // change under it as other rows move.
                                            val current = localOrder.indexOfFirst { it.categoryId == id }
                                            val steps = (dragOffset / rowHeight).roundToInt()
                                            if (steps != 0 && current >= 0) {
                                                val target = (current + steps)
                                                    .coerceIn(0, localOrder.lastIndex)
                                                if (target != current) {
                                                    localOrder = localOrder.toMutableList().apply {
                                                        add(target, removeAt(current))
                                                    }
                                                    dragOffset -= steps * rowHeight
                                                }
                                            }
                                        }
                                    }
                                )
                            }
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(row.emoji, style = MaterialTheme.typography.headlineSmall)
                        Text(
                            row.name,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 12.dp).weight(1f)
                        )
                        Text(
                            row.amountText,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (row.hasAmount) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }
        }
    }
}
