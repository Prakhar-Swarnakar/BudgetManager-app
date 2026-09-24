package com.budgetmanager.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import com.budgetmanager.app.ui.theme.BudgetManagerTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * M0 checkpoint: confirms Hilt + the theme compile and run. Replaced with the real
 * navigation shell (AppNavigation) in the next batch.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BudgetManagerTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Text(
                        "Budget Manager – M0 foundation",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}
