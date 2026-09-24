package com.budgetmanager.app.navigation

/** Every screen reachable from the bottom bar or the side panel. See 08-pages-and-navigation.md. */
sealed interface Destination {
    data object Home : Destination
    data object Messages : Destination
    data object Trends : Destination
    data object MonthlyBudget : Destination
    data object Categories : Destination
    data object Settings : Destination

    /** Manual add when both are null, accepting a message when [messageId] is set, editing an
     *  existing transaction when [transactionId] is set. Never both at once. */
    data class AddTransaction(val messageId: Long? = null, val transactionId: Long? = null) : Destination
}
