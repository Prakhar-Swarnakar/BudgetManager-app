package com.budgetmanager.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.automirrored.filled.Rule
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import com.budgetmanager.app.core.designsystem.components.AppScaffold
import com.budgetmanager.app.feature.budget.MonthlyBudgetScreen
import com.budgetmanager.app.feature.wallet.WalletScreen
import com.budgetmanager.app.feature.categoryrules.CategoryRulesScreen
import com.budgetmanager.app.feature.budget.MonthlyBudgetViewModel
import com.budgetmanager.app.feature.categorydetail.CategoryDetailScreen
import com.budgetmanager.app.feature.home.HomeScreen
import com.budgetmanager.app.feature.messages.MessagesScreen
import com.budgetmanager.app.feature.owedtoyou.OwedToYouScreen
import com.budgetmanager.app.feature.settings.SettingsScreen
import com.budgetmanager.app.feature.statementvalidator.StatementValidatorScreen
import com.budgetmanager.app.feature.taxonomyrules.TaxonomyRulesScreen
import com.budgetmanager.app.feature.transaction.AddTransactionScreen
import com.budgetmanager.app.feature.transaction.AddTransactionViewModel
import com.budgetmanager.app.feature.trends.TrendsScreen
import kotlinx.coroutines.launch

private data class NavItem(val destination: Destination, val label: String, val icon: ImageVector)

private val bottomBarItems = listOf(
    NavItem(Destination.Home, "Home", Icons.Default.Home),
    NavItem(Destination.Messages, "Messages", Icons.Default.Mail),
    NavItem(Destination.Trends, "Trends", Icons.Default.ShowChart),
    NavItem(Destination.OwedToYou, "Owed to you", Icons.Default.Groups)
)

private val sidePanelItems = listOf(
    NavItem(Destination.MonthlyBudget, "Monthly budget", Icons.Default.AccountBalanceWallet),
    NavItem(Destination.Wallet, "Wallet", Icons.Default.AccountBalance),
    NavItem(Destination.CategoryRules, "Category rules", Icons.AutoMirrored.Filled.Rule),
    NavItem(Destination.TaxonomyRules, "Taxonomy rules", Icons.Default.Payments),
    NavItem(Destination.Settings, "Settings", Icons.Default.Settings)
)

/** True for a screen pushed onto the back stack rather than reached from the bottom bar or side
 *  panel - it gets a back arrow in the top bar instead of the hamburger menu. */
private fun isPushedDetail(destination: Destination): Boolean =
    destination is Destination.AddTransaction || destination is Destination.CategoryDetail ||
        destination is Destination.StatementValidator

private fun titleFor(destination: Destination): String = when (destination) {
    Destination.Home -> "Home"
    Destination.Messages -> "Messages"
    Destination.Trends -> "Trends"
    Destination.OwedToYou -> "Owed to you"
    Destination.MonthlyBudget -> "Monthly budget"
    Destination.Wallet -> "Wallet"
    Destination.Settings -> "Settings"
    Destination.CategoryRules -> "Category rules"
    Destination.TaxonomyRules -> "Taxonomy rules"
    Destination.StatementValidator -> "Validate a statement"
    is Destination.AddTransaction -> if (destination.transactionId != null) "Edit transaction" else "Add transaction"
    is Destination.CategoryDetail -> destination.categoryName
}

/**
 * Bottom bar for Home/Messages/Trends (daily use), side panel for Monthly budget/Category
 * rules/Taxonomy rules/Settings (used a few times a month). See 08-pages-and-navigation.md.
 * Category management (create, rename, reorder) lives on the Monthly budget page - there is no
 * separate Categories page.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation() {
    val backStack = remember { mutableStateListOf<Destination>(Destination.Home) }
    val current = backStack.last()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val navViewModel: AppNavigationViewModel = hiltViewModel()
    val newMessageCount by navViewModel.newMessageCount.collectAsStateWithLifecycle()
    // The badge clears the moment the Messages page is open, even though the underlying
    // new-flag doesn't clear until the user leaves it (that's what keeps the rows bold while
    // you're looking at them). See 04-messages-and-notifications.md.
    val showMessagesBadge = newMessageCount > 0 && current != Destination.Messages

    fun navigateToTopLevel(destination: Destination) {
        backStack.clear()
        backStack.add(destination)
    }

    fun navigateTo(destination: Destination) {
        backStack.add(destination)
        scope.launch { drawerState.close() }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        // The drawer's own edge-swipe-to-open gesture competes with swipe gestures on screen
        // content (e.g. accepting/rejecting a message row) - disabled since the hamburger
        // button already opens it explicitly.
        gesturesEnabled = false,
        drawerContent = {
            ModalDrawerSheet {
                sidePanelItems.forEach { item ->
                    NavigationDrawerItem(
                        label = { Text(item.label) },
                        icon = { Icon(item.icon, contentDescription = null) },
                        selected = current == item.destination,
                        onClick = { navigateTo(item.destination) }
                    )
                }
            }
        }
    ) {
        val pushedDetail = isPushedDetail(current)

        AppScaffold(
            title = titleFor(current),
            onMenuClick = {
                if (pushedDetail) backStack.removeLastOrNull() else scope.launch { drawerState.open() }
            },
            useBackArrow = pushedDetail,
            actions = {
                // Monthly budget has no per-instance arguments, so this bare hiltViewModel()
                // call resolves to the same ViewModel instance the screen itself gets - there's
                // no per-NavEntry ViewModelStore scoping wired up for Navigation 3 here (see the
                // same note on AddTransactionScreen), so both calls share the single Activity-
                // scoped instance.
                if (current == Destination.MonthlyBudget) {
                    val monthlyBudgetViewModel: MonthlyBudgetViewModel = hiltViewModel()
                    IconButton(onClick = monthlyBudgetViewModel::onAddClicked) {
                        Icon(Icons.Default.Add, contentDescription = "New category")
                    }
                }
                // Save lives in the header rather than a button row at the bottom of the form -
                // see AddTransactionContent.kt. Keyed the same way the screen's own ViewModel
                // call is (Destination.AddTransaction.viewModelKey()) so both resolve the same
                // cached instance.
                if (current is Destination.AddTransaction) {
                    val addTransactionViewModel: AddTransactionViewModel = hiltViewModel(key = current.viewModelKey())
                    val addTransactionState by addTransactionViewModel.uiState.collectAsStateWithLifecycle()
                    val keyboardController = LocalSoftwareKeyboardController.current
                    val focusManager = LocalFocusManager.current
                    TextButton(
                        onClick = {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                            addTransactionViewModel.onSave()
                        },
                        enabled = !addTransactionState.isSaving
                    ) {
                        Text(if (addTransactionState.isEditMode) "Save changes" else "Save")
                    }
                }
            },
            bottomBar = {
                NavigationBar {
                    bottomBarItems.forEach { item ->
                        NavigationBarItem(
                            selected = current == item.destination,
                            onClick = { navigateToTopLevel(item.destination) },
                            icon = {
                                if (item.destination == Destination.Messages && showMessagesBadge) {
                                    BadgedBox(badge = { Badge() }) {
                                        Icon(item.icon, contentDescription = null)
                                    }
                                } else {
                                    Icon(item.icon, contentDescription = null)
                                }
                            },
                            label = { Text(item.label) }
                        )
                    }
                }
            }
        ) { contentModifier ->
            NavDisplay(
                backStack = backStack,
                onBack = { backStack.removeLastOrNull() },
                entryProvider = { destination ->
                    when (destination) {
                        Destination.Home -> NavEntry(destination) {
                            HomeScreen(
                                onNavigateToAddTransaction = { backStack.add(Destination.AddTransaction()) },
                                onNavigateToMessages = { navigateToTopLevel(Destination.Messages) },
                                onNavigateToCategoryDetail = { categoryId, categoryName, monthKey ->
                                    backStack.add(Destination.CategoryDetail(categoryId, categoryName, monthKey))
                                },
                                modifier = contentModifier
                            )
                        }
                        Destination.Messages -> NavEntry(destination) {
                            MessagesScreen(
                                onNavigateToAddTransaction = { messageId ->
                                    backStack.add(Destination.AddTransaction(messageId = messageId))
                                },
                                modifier = contentModifier
                            )
                        }
                        Destination.Trends -> NavEntry(destination) { TrendsScreen(contentModifier) }
                        Destination.OwedToYou -> NavEntry(destination) {
                            OwedToYouScreen(
                                onNavigateToEditTransaction = { transactionId ->
                                    backStack.add(Destination.AddTransaction(transactionId = transactionId))
                                },
                                modifier = contentModifier
                            )
                        }
                        Destination.MonthlyBudget -> NavEntry(destination) { MonthlyBudgetScreen(contentModifier) }
                        Destination.Wallet -> NavEntry(destination) {
                            WalletScreen(
                                onNavigateToCategoryDetail = { categoryId, categoryName, monthKey ->
                                    backStack.add(Destination.CategoryDetail(categoryId, categoryName, monthKey))
                                },
                                modifier = contentModifier
                            )
                        }
                        Destination.Settings -> NavEntry(destination) {
                            SettingsScreen(
                                onValidateStatementClick = { backStack.add(Destination.StatementValidator) },
                                modifier = contentModifier
                            )
                        }
                        Destination.CategoryRules -> NavEntry(destination) { CategoryRulesScreen(contentModifier) }
                        Destination.TaxonomyRules -> NavEntry(destination) { TaxonomyRulesScreen(contentModifier) }
                        Destination.StatementValidator -> NavEntry(destination) {
                            StatementValidatorScreen(
                                onNavigateToAddTransaction = { amountPaise, title, dateEpochDay, bannerText ->
                                    backStack.add(
                                        Destination.AddTransaction(
                                            prefillAmountPaise = amountPaise,
                                            prefillTitle = title,
                                            prefillDateEpochDay = dateEpochDay,
                                            prefillBannerText = bannerText
                                        )
                                    )
                                },
                                modifier = contentModifier
                            )
                        }
                        is Destination.AddTransaction -> NavEntry(destination) {
                            AddTransactionScreen(
                                messageId = destination.messageId,
                                transactionId = destination.transactionId,
                                onDone = { backStack.removeLastOrNull() },
                                prefillAmountPaise = destination.prefillAmountPaise,
                                prefillTitle = destination.prefillTitle,
                                prefillDateEpochDay = destination.prefillDateEpochDay,
                                prefillBannerText = destination.prefillBannerText,
                                modifier = contentModifier
                            )
                        }
                        is Destination.CategoryDetail -> NavEntry(destination) {
                            CategoryDetailScreen(
                                categoryId = destination.categoryId,
                                monthKey = destination.monthKey,
                                onNavigateToEditTransaction = { transactionId ->
                                    backStack.add(Destination.AddTransaction(transactionId = transactionId))
                                },
                                modifier = contentModifier
                            )
                        }
                    }
                }
            )
        }
    }
}
