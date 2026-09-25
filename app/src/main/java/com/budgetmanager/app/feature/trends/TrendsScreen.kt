package com.budgetmanager.app.feature.trends

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.budgetmanager.app.core.designsystem.components.ScreenPlaceholder

private val tabTitles = listOf("This month", "Previous month", "Historic")

@Composable
fun TrendsScreen(
    modifier: Modifier = Modifier,
    viewModel: TrendsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    Column(modifier = modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) }
                )
            }
        }
        when (selectedTab) {
            // Previous month and Historic are built in M11 (14-implementation-plan.md) - this
            // month's donut and by-category list is M10's scope.
            0 -> TrendsThisMonthContent(state, modifier = Modifier.weight(1f))
            1 -> ScreenPlaceholder("Previous month", modifier = Modifier.weight(1f))
            else -> ScreenPlaceholder("Historic", modifier = Modifier.weight(1f))
        }
    }
}
