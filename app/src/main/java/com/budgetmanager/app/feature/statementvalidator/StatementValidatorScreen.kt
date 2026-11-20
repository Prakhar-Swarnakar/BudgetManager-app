package com.budgetmanager.app.feature.statementvalidator

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import java.io.IOException

@Composable
fun StatementValidatorScreen(
    onNavigateToAddTransaction: (
        prefillAmountPaise: Long,
        prefillTitle: String,
        prefillDateEpochDay: Long,
        prefillBannerText: String
    ) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StatementValidatorViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val pickLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            try {
                val bytes = context.contentResolver.openInputStream(uri)
                    ?.use { it.readBytes() }
                    ?: throw IOException("openInputStream returned null")
                viewModel.onFileSelected(queryDisplayName(context, uri) ?: "Statement", bytes)
            } catch (e: IOException) {
                viewModel.onFileReadFailed()
            }
        }
    }

    LaunchedEffect(state.navigateToAddTransactionForLine) {
        val line = state.navigateToAddTransactionForLine ?: return@LaunchedEffect
        onNavigateToAddTransaction(
            line.amountPaise,
            line.description,
            line.dateEpochDay,
            "From statement: ${line.description} · ${line.dateLabel} · ${line.amountText}"
        )
        viewModel.onNavigationHandled()
    }

    StatementValidatorContent(
        state = state,
        onPickFile = {
            pickLauncher.launch(
                arrayOf(
                    "text/csv", "text/comma-separated-values", "application/csv", "text/plain",
                    "application/pdf",
                    "application/vnd.ms-excel",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    "*/*"
                )
            )
        },
        onAddRequested = viewModel::onAddRequested,
        onReset = viewModel::onReset,
        onDismissError = viewModel::onDismissError,
        onPasswordSubmitted = viewModel::onPasswordSubmitted,
        onPasswordPromptCancelled = viewModel::onPasswordPromptCancelled,
        modifier = modifier
    )
}

/** The file picker only guarantees a content Uri, not a human-readable name - this is the
 *  standard SAF query for one, same idea as any file manager's "Choose file" result. */
private fun queryDisplayName(context: Context, uri: Uri): String? {
    return context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
        ?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
        }
}
