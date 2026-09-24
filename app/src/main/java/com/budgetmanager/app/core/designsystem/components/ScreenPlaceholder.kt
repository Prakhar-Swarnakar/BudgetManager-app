package com.budgetmanager.app.core.designsystem.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/** Temporary content for a screen not yet built. Each feature milestone replaces its own use of this. */
@Composable
fun ScreenPlaceholder(label: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("$label – coming soon")
    }
}
