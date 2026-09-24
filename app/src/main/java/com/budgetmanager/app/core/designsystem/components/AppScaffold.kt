package com.budgetmanager.app.core.designsystem.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

/**
 * Standard page frame: top bar (with an optional menu button), an optional bottom bar,
 * a snackbar host, and the padding every screen needs to apply to its content.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold(
    title: String,
    modifier: Modifier = Modifier,
    onMenuClick: (() -> Unit)? = null,
    bottomBar: @Composable () -> Unit = {},
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    content: @Composable (Modifier) -> Unit
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    onMenuClick?.let { onClick ->
                        IconButton(onClick = onClick) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    }
                }
            )
        },
        bottomBar = bottomBar,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        content(Modifier.padding(innerPadding))
    }
}
