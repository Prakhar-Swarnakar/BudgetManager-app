package com.budgetmanager.app.navigation

import com.budgetmanager.app.core.model.MonthKey

/** Every screen reachable from the bottom bar or the side panel. See 08-pages-and-navigation.md. */
sealed interface Destination {
    data object Home : Destination
    data object Messages : Destination
    data object Trends : Destination
    data object OwedToYou : Destination
    data object MonthlyBudget : Destination
    data object Wallet : Destination
    data object Settings : Destination
    data object CategoryRules : Destination
    data object TaxonomyRules : Destination
    data object StatementValidator : Destination

    /** Manual add when both are null, accepting a message when [messageId] is set, editing an
     *  existing transaction when [transactionId] is set. Never both at once. The prefill fields
     *  are only ever used alongside the manual-add case (both ids null) - a statement line has
     *  no linked message to accept, just a date/amount/description worth starting the form from
     *  (06-backlog.md, "Credit card / bank statement import"). */
    data class AddTransaction(
        val messageId: Long? = null,
        val transactionId: Long? = null,
        val prefillAmountPaise: Long? = null,
        val prefillTitle: String? = null,
        val prefillDateEpochDay: Long? = null,
        val prefillBannerText: String? = null
    ) : Destination {
        /** A distinct key per (messageId, transactionId, prefill...): without it, every
         *  navigation to Add Transaction would share one Hilt-cached ViewModel instance (there's
         *  no per-NavEntry ViewModelStore scoping wired up for Navigation 3 here), so accepting
         *  message B after message A would still show the previous one's already-loaded state.
         *  Shared by AddTransactionScreen's own `hiltViewModel()` call and the top-bar Save
         *  button's `hiltViewModel()` call in AppNavigation, so both resolve the same instance. */
        fun viewModelKey(): String =
            "AddTransaction:$messageId:$transactionId:$prefillAmountPaise:$prefillDateEpochDay"
    }

    /** [categoryName] rides along so the top bar title doesn't need its own lookup, matching
     *  how [AddTransaction]'s title differs without one. [monthKey] is only the month to open
     *  on - after that the screen's own month selector takes over. */
    data class CategoryDetail(val categoryId: Long, val categoryName: String, val monthKey: MonthKey) : Destination
}
