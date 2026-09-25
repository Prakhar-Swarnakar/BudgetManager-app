package com.budgetmanager.app.feature.categories.components

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
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.budgetmanager.app.core.designsystem.components.NoSwipeAction
import com.budgetmanager.app.core.designsystem.components.SwipeAction
import com.budgetmanager.app.core.designsystem.components.SwipeRow
import com.budgetmanager.app.core.model.Category
import com.budgetmanager.app.ui.theme.StatusColors
import kotlin.math.roundToInt

/**
 * A plain `Column`, not `LazyColumn`: category counts stay small for a single-user app, and
 * this trades list virtualisation for a much simpler drag implementation. Reordering happens
 * entirely in local state and only touches the repository once, on drag end - so a drag never
 * has to round-trip through Room's `Flow` mid-gesture.
 *
 * Combining a drag handle with swipe-to-archive on the same row is the part of M5 most worth
 * testing carefully on a real device (R30 in 09-risks-and-phases.md flags exactly this
 * combination as a place two gestures could conflict).
 */
@Composable
fun CategoryList(
    categories: List<Category>,
    onRowClick: (Long) -> Unit,
    onArchive: (Long) -> Unit,
    onReorder: (List<Long>) -> Unit,
    modifier: Modifier = Modifier
) {
    var localOrder by remember(categories) { mutableStateOf(categories) }
    var rowHeightPx by remember { mutableStateOf(0f) }
    var draggedId by remember { mutableStateOf<Long?>(null) }
    var dragOffset by remember { mutableStateOf(0f) }
    val archiveAction = SwipeAction(Icons.Default.Archive, StatusColors.warning)

    Column(modifier = modifier) {
        localOrder.forEach { category ->
            val isDragged = category.id == draggedId
            Box(
                modifier = Modifier
                    .zIndex(if (isDragged) 1f else 0f)
                    .graphicsLayer { translationY = if (isDragged) dragOffset else 0f }
                    .onGloballyPositioned { coordinates ->
                        if (rowHeightPx == 0f) rowHeightPx = coordinates.size.height.toFloat()
                    }
            ) {
                SwipeRow(
                    onSwipeStart = {},
                    onSwipeEnd = { onArchive(category.id) },
                    startAction = NoSwipeAction,
                    endAction = archiveAction
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onRowClick(category.id) }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.DragHandle,
                            contentDescription = "Drag to reorder",
                            modifier = Modifier.pointerInput(category.id) {
                                detectDragGestures(
                                    onDragStart = {
                                        draggedId = category.id
                                        dragOffset = 0f
                                    },
                                    onDragEnd = {
                                        draggedId = null
                                        dragOffset = 0f
                                        onReorder(localOrder.map { it.id })
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
                                            // Resolved by id, not by the index captured when this
                                            // gesture started - pointerInput's key is the id, so
                                            // this block only re-launches when the ROW's id
                                            // changes, not when its position in the list does.
                                            val current = localOrder.indexOfFirst { it.id == id }
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
                        Text(category.emoji, style = MaterialTheme.typography.headlineSmall)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            category.name,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}
