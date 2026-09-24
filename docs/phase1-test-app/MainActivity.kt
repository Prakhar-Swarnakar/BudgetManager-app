package com.example.budgetspike

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.provider.Telephony
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Phase 1 test screen. Shows what the phone allows, and what the SMS receiver has caught.
 * Everything is refreshed each time the app comes to the front (onResume).
 */
class MainActivity : ComponentActivity() {

    private var tick by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    TestScreen(tick)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        tick++
    }
}

private data class InboxRow(val sender: String, val time: Long, val text: String, val spendLike: Boolean)

private fun has(context: Context, permission: String): Boolean =
    ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

private fun installerName(context: Context): String {
    return try {
        if (Build.VERSION.SDK_INT >= 30) {
            val info = context.packageManager.getInstallSourceInfo(context.packageName)
            info.installingPackageName ?: "none (installed by USB, ADB or a file)"
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getInstallerPackageName(context.packageName)
                ?: "none (installed by USB, ADB or a file)"
        }
    } catch (e: Exception) {
        "unknown"
    }
}

private fun readInbox(context: Context, limit: Int = 20): List<InboxRow> {
    if (!has(context, Manifest.permission.READ_SMS)) return emptyList()
    val rows = mutableListOf<InboxRow>()
    try {
        context.contentResolver.query(
            Telephony.Sms.Inbox.CONTENT_URI,
            arrayOf(Telephony.Sms.ADDRESS, Telephony.Sms.DATE, Telephony.Sms.BODY),
            null,
            null,
            "date DESC"
        )?.use { c ->
            val iAddr = c.getColumnIndex(Telephony.Sms.ADDRESS)
            val iDate = c.getColumnIndex(Telephony.Sms.DATE)
            val iBody = c.getColumnIndex(Telephony.Sms.BODY)
            while (c.moveToNext() && rows.size < limit) {
                val body = c.getString(iBody) ?: ""
                val spend = SpendDetector.isSpendLike(body)
                rows.add(
                    InboxRow(
                        sender = c.getString(iAddr) ?: "unknown",
                        time = c.getLong(iDate),
                        // Privacy: only show text for spend-like messages.
                        text = if (spend) body else "(not shown: not spend-like)",
                        spendLike = spend
                    )
                )
            }
        }
    } catch (e: Exception) {
        // Permission missing or provider unavailable: show nothing.
    }
    return rows
}

private fun fmtTime(ms: Long): String =
    SimpleDateFormat("dd MMM HH:mm:ss", Locale.getDefault()).format(Date(ms))

@Composable
private fun Heading(text: String) {
    Spacer(Modifier.height(20.dp))
    Text(text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(6.dp))
}

@Composable
private fun TestScreen(tick: Int) {
    val context = LocalContext.current
    var localTick by remember { mutableIntStateOf(0) }
    val key = tick + localTick

    val smsGranted = remember(key) {
        has(context, Manifest.permission.RECEIVE_SMS) && has(context, Manifest.permission.READ_SMS)
    }
    val notificationsOn = remember(key) { NotificationManagerCompat.from(context).areNotificationsEnabled() }
    val entries = remember(key) { SpendLog.load(context) }
    val inbox = remember(key) { readInbox(context) }

    // After the lists above were read (so NEW markers show once), mark everything as seen
    // and clear the notification.
    LaunchedEffect(key) {
        SpendLog.markAllSeen(context)
        Notifier.cancel(context)
    }

    val smsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { localTick++ }
    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { localTick++ }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Budget Phase 1 test", fontSize = 24.sp, fontWeight = FontWeight.Bold)

        Heading("This phone")
        Text("${Build.MANUFACTURER} ${Build.MODEL}")
        Text("Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        Text("Installed by: ${installerName(context)}")

        Heading("Permissions")
        Text("SMS (receive + read): " + if (smsGranted) "GRANTED" else "not granted")
        Text("Notifications: " + if (notificationsOn) "ON" else "OFF or not allowed")
        Spacer(Modifier.height(8.dp))

        Button(
            onClick = {
                smsLauncher.launch(
                    arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS)
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("1. Ask for SMS permission") }

        OutlinedButton(
            onClick = {
                context.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        .setData(Uri.fromParts("package", context.packageName, null))
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Open App info (for Allow restricted settings)") }

        Button(
            onClick = {
                if (Build.VERSION.SDK_INT >= 33) {
                    notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    localTick++
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("2. Ask for notification permission") }

        OutlinedButton(
            onClick = {
                context.startActivity(
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Open notification settings") }

        Heading("Notification test")
        Button(
            onClick = { Notifier.showSpends(context, 3) },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Show a test notification") }
        Text(
            "Leave the app (press Home) after tapping. The notification is cleared when you come back.",
            fontSize = 12.sp
        )

        Heading("Caught by the receiver (${entries.size})")
        Text(
            "Delay = the app's clock when it caught the SMS minus the SMS's own time stamp.",
            fontSize = 12.sp
        )
        if (entries.isEmpty()) {
            Text("Nothing yet. Send an SMS to this phone.")
        }
        entries.forEach { e ->
            Spacer(Modifier.height(8.dp))
            val delay = (e.receivedAt - e.smsTimestamp) / 1000
            Text(
                (if (!e.seen) "NEW  " else "") + e.sender +
                    "  |  " + fmtTime(e.receivedAt),
                fontWeight = FontWeight.Bold
            )
            Text(
                (if (e.spendLike) "Spend-like" else "Not spend-like") +
                    (e.amount?.let { "  |  amount $it" } ?: "") +
                    "  |  delay ${delay}s",
                fontSize = 13.sp
            )
            Text(e.text, fontSize = 13.sp)
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = { SpendLog.clear(context); localTick++ },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Clear this list") }

        Heading("Latest in your SMS inbox")
        if (!smsGranted) {
            Text("Grant SMS permission to see this list.")
        } else if (inbox.isEmpty()) {
            Text("No messages found.")
        }
        inbox.forEach { r ->
            Spacer(Modifier.height(8.dp))
            Text(r.sender + "  |  " + fmtTime(r.time), fontWeight = FontWeight.Bold)
            Text(
                (if (r.spendLike) "Spend-like" else "Not spend-like") +
                    "  |  " + r.text,
                fontSize = 13.sp
            )
        }
        Spacer(Modifier.height(40.dp))
    }
}
