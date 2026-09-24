package com.budgetmanager.app.navigation

/** Every screen reachable from the bottom bar or the side panel. See 08-pages-and-navigation.md. */
sealed interface Destination {
    data object Home : Destination
    data object Messages : Destination
    data object Trends : Destination
    data object MonthlyBudget : Destination
    data object Categories : Destination
    data object Settings : Destination
}
