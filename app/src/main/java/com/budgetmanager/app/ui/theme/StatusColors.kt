package com.budgetmanager.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Single source for status colours, used on Home, Trends, and Messages.
 * See 05-budget-rules.md for the thresholds these colours represent.
 */
object StatusColors {
    val underBudget: Color = AccentBlue
    val warning: Color = WarningAmber
    val overBudget: Color = OverBudgetRed
    val accepted: Color = AcceptedGreen
    /** A month still accumulating spend, shown in Trends' Previous month and Historic bar charts
     *  instead of a status colour - it hasn't finished, so "within" or "over" wouldn't be final. */
    val inProgress: Color = InProgressBlue
}
