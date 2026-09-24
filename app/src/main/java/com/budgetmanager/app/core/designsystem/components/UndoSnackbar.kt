package com.budgetmanager.app.core.designsystem.components

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

/**
 * Shows an "Undo" snackbar whenever [trigger] becomes non-null - driven by state rather than a
 * one-off event, per the Compose rule that a ViewModel must never fire events the UI could miss
 * or repeat. Calls [onUndo] if the user taps Undo, or [onDismissed] once the bar goes away
 * either way (tap elsewhere, or the short timeout).
 */
@Composable
fun <T> UndoSnackbarEffect(
    trigger: T?,
    snackbarHostState: SnackbarHostState,
    message: String,
    onUndo: () -> Unit,
    onDismissed: () -> Unit
) {
    LaunchedEffect(trigger) {
        if (trigger == null) return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message = message,
            actionLabel = "Undo",
            duration = SnackbarDuration.Short
        )
        if (result == SnackbarResult.ActionPerformed) onUndo() else onDismissed()
    }
}
