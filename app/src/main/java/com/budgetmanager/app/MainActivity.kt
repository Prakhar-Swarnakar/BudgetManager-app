package com.budgetmanager.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.budgetmanager.app.core.designsystem.components.PermissionGate
import com.budgetmanager.app.domain.SeedKeywordTaxonomy
import com.budgetmanager.app.navigation.AppNavigation
import com.budgetmanager.app.sms.InboxScanner
import com.budgetmanager.app.ui.theme.BudgetManagerTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var inboxScanner: InboxScanner

    @Inject
    lateinit var seedKeywordTaxonomy: SeedKeywordTaxonomy

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BudgetManagerTheme {
                PermissionGate {
                    AppNavigation()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Catches up on anything missed while closed. No-ops safely if SMS permission isn't
        // granted yet (e.g. still on the PermissionGate screen).
        lifecycleScope.launch { inboxScanner.scan() }
        // One-time keyword taxonomy backfill - self-guards after the first real run, separate
        // launch so a failure/slowness here never blocks the SMS catch-up above.
        lifecycleScope.launch { seedKeywordTaxonomy() }
    }
}
