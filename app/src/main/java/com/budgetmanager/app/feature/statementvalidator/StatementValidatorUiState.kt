package com.budgetmanager.app.feature.statementvalidator

sealed interface StatementValidatorStep {
    data object PickFile : StatementValidatorStep
    data class Report(
        val fileName: String,
        val dateRangeLabel: String,
        val coveragePercent: Int,
        val matchedCount: Int,
        val lineCount: Int,
        val skippedRowCount: Int,
        val statementTotalText: String,
        val inAppTotalText: String,
        val differenceText: String,
        val needsReview: List<StatementRowUi>,
        val missing: List<StatementLineUi>,
        val rejectedButOnStatement: List<StatementRowUi>,
        val amountMismatches: List<StatementMismatchUi>,
        val possibleFalsePositives: List<StatementRowUi>
    ) : StatementValidatorStep
}

/** A statement line already matched to a real message - shown read-only in this round (open
 *  Messages yourself to act on it; deep-linking a specific row in from here is a nice-to-have,
 *  not built yet). */
data class StatementRowUi(
    val description: String,
    val dateLabel: String,
    val amountText: String
)

/** A statement line with no matching message at all - the one bucket with a real action, since
 *  there's nothing to open and review, only something to add. */
data class StatementLineUi(
    val description: String,
    val dateLabel: String,
    val amountText: String,
    val amountPaise: Long,
    val dateEpochDay: Long
)

data class StatementMismatchUi(
    val description: String,
    val dateLabel: String,
    val statementAmountText: String,
    val ourAmountText: String
)

/** Shown over the picker when a selected PDF turned out to be password-protected - [error] is
 *  set after a wrong attempt so the field stays open for another try instead of bouncing back to
 *  square one. */
data class PasswordPrompt(val fileName: String, val error: String? = null)

data class StatementValidatorUiState(
    val step: StatementValidatorStep = StatementValidatorStep.PickFile,
    val isProcessing: Boolean = false,
    val errorMessage: String? = null,
    val navigateToAddTransactionForLine: StatementLineUi? = null,
    val passwordPrompt: PasswordPrompt? = null
)
