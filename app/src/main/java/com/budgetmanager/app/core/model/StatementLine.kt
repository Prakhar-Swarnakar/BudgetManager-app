package com.budgetmanager.app.core.model

import java.time.LocalDate

/** One real line from an imported bank/credit-card statement - the ground truth the Messages
 *  pipeline is checked against (06-backlog.md, "Credit card / bank statement import"). A
 *  validator, not a second way to add transactions: nothing here is ever saved on its own. */
data class StatementLine(
    val date: LocalDate,
    val description: String,
    val amount: Money,
    val direction: MessageDirection
)
