package com.budgetmanager.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mail
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
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import com.budgetmanager.app.core.designsystem.components.AppScaffold
import com.budgetmanager.app.feature.budget.MonthlyBudgetScreen
import com.budgetmanager.app.feature.budget.MonthlyBudgetViewModel
import com.budgetmanager.app.feature.categories.CategoriesScreen
import com.budgetmanager.app.feature.categories.CategoriesViewModel
import com.budgetmanager.app.feature.home.HomeScreen
import com.budgetmanager.app.feature.messages.MessagesScreen
import com.budgetmanager.app.feature.settings.SettingsScreen
import com.budgetmanager.app.feature.transaction.AddTransactionScreen
import com.budgetmanager.app.feature.trends.TrendsScreen
import kotlinx.coroutines.launch

private data class NavItem(val destination: Destination, val label: String, val icon: ImageVector)

private val bottomBarItems = listOf(
    NavItem(Destination.Home, "Home", Icons.Default.Home),
    NavItem(Destination.Messages, "Messages", Icons.Default.Mail),
    NavItem(Destination.Trends, "Trends", Icons.Default.ShowChart)
)

private val sidePanelItems = listOf(
    NavItem(Destination.MonthlyBudget, "Monthly budget", Icons.Default.AccountBalanceWallet),
    NavItem(Destination.Categories, "Categories", Icons.Default.Category),
    NavItem(Destination.Settings, "Settings", Icons.Default.Settings)
)

private fun titleFor(destination: Destination): String = when (destination) {
    Destination.Home -> "Home"
    Destination.Messages -> "Messages"
    Destination.Trends -> "Trends"
    Destination.MonthlyBudget -> "Monthly budget"
    Destination.Categories -> "Categories"
    Destination.Settings -> "Settings"
    is Destination.AddTransaction -> if (destination.transactionId != null) "Edit transaction" else "Add transaction"
}

/**
 * Bottom bar for Home/Messages/Trends (daily use), side panel for Monthly budget/Categories/
 * Settings (used a few times a month). See 08-pages-and-navigation.md.
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
        AppScaffold(
            title = titleFor(current),
            onMenuClick = { scope.launch { drawerState.open() } },
            actions = {
                // Neither destination has per-instance arguments, so these bare hiltViewModel()
                // calls resolve to the same ViewModel instances the screens themselves get -
                // there's no per-NavEntry ViewModelStore scoping wired up for Navigation 3 here
                // (see the same note on AddTransactionScreen), so both calls share the single
                // Activity-scoped instance.
                when (current) {
                    Destination.Categories -> {
                        val categoriesViewModel: CategoriesViewModel = hiltViewModel()
                        IconButton(onClick = categoriesViewModel::onAddClicked) {
                            Icon(Icons.Default.Add, contentDescription = "New category")
                        }
                    }
                    Destination.MonthlyBudget -> {
                        val monthlyBudgetViewModel: MonthlyBudgetViewModel = hiltViewModel()
                        IconButton(onClick = monthlyBudgetViewModel::onAddClicked) {
                            Icon(Icons.Default.Add, contentDescription = "New category")
                        }
                    }
                    else -> Unit
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
                        Destination.Home -> NavEntry(destination) { HomeScreen(contentModifier) }
                        Destination.Messages -> NavEntry(destination) {
                            MessagesScreen(
                                onNavigateToAddTransaction = { messageId ->
                                    backStack.add(Destination.AddTransaction(messageId = messageId))
                                },
                                modifier = contentModifier
                            )
                        }
                        Destination.Trends -> NavEntry(destination) { TrendsScreen(contentModifier) }
                        Destination.MonthlyBudget -> NavEntry(destination) { MonthlyBudgetScreen(contentModifier) }
                        Destination.Categories -> NavEntry(destination) { CategoriesScreen(contentModifier) }
                        Destination.Settings -> NavEntry(destination) { SettingsScreen(contentModifier) }
                        is Destination.AddTransaction -> NavEntry(destination) {
                            AddTransactionScreen(
                                messageId = destination.messageId,
                                transactionId = destination.transactionId,
                                onDone = { backStack.removeLastOrNull() },
                                modifier = contentModifier
                            )
                        }
                    }
                }
            )
        }
    }
}
