package com.budgetmanager.app.core.designsystem.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * A large, editable emoji field. Tapping it brings up the phone's own keyboard, where the user
 * picks one emoji - the app has no emoji picker of its own (03-user-flows.md, flow 11).
 */
@Composable
fun EmojiIconBox(
    emoji: String,
    onEmojiChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false
) {
    OutlinedTextField(
        value = emoji,
        onValueChange = onEmojiChanged,
        modifier = modifier.size(width = 88.dp, height = 64.dp),
        textStyle = MaterialTheme.typography.headlineMedium.copy(textAlign = TextAlign.Center),
        isError = isError,
        singleLine = true
    )
}
