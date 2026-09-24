package com.budgetmanager.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.budgetmanager.app.core.designsystem.components.PermissionGate
import com.budgetmanager.app.navigation.AppNavigation
import com.budgetmanager.app.ui.theme.BudgetManagerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
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
}
