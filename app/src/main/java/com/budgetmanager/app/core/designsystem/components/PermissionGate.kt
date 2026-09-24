package com.budgetmanager.app.core.designsystem.components

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

/**
 * Blocks the rest of the app behind SMS and notification permissions, since the app can't do
 * its job without them. Explains the "Allow restricted settings" step Phase 1 found is needed
 * for apps installed outside the Play Store (R2). See 03-user-flows.md flow 1.
 */
@Composable
fun PermissionGate(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val context = LocalContext.current
    var refreshKey by remember { mutableIntStateOf(0) }

    val smsGranted = remember(refreshKey) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) ==
            PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) ==
            PackageManager.PERMISSION_GRANTED
    }
    val notificationsGranted = remember(refreshKey) {
        NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    if (smsGranted && notificationsGranted) {
        content()
        return
    }

    val smsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { refreshKey++ }
    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { refreshKey++ }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Budget Manager needs two permissions", fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Text(
            "It reads bank SMS to catch your spends automatically, and shows a notification " +
                "when a new one arrives. Nothing leaves your phone."
        )
        Spacer(Modifier.height(24.dp))

        if (!smsGranted) {
            Text("SMS permission: not granted", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Button(onClick = {
                smsLauncher.launch(arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS))
            }) { Text("Allow SMS access") }
            Spacer(Modifier.height(8.dp))
            Text(
                "If nothing happens when you tap this, Android is blocking it silently " +
                    "because the app wasn't installed from the Play Store. Open App info below, " +
                    "tap the ⋮ menu, choose \"Allow restricted settings\", then come back and try again."
            )
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = {
                context.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        .setData(Uri.fromParts("package", context.packageName, null))
                )
            }) { Text("Open App info") }
            Spacer(Modifier.height(24.dp))
        }

        if (!notificationsGranted) {
            Text("Notifications: not granted", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Button(onClick = {
                if (Build.VERSION.SDK_INT >= 33) {
                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    refreshKey++
                }
            }) { Text("Allow notifications") }
        }
    }
}
