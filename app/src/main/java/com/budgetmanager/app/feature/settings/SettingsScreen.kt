package com.budgetmanager.app.feature.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    onOpenCategoryRules: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Permission/battery status is OS state, not something the ViewModel owns - refreshed
    // whenever this screen resumes, since that's when the user is most likely coming back from
    // fixing one of them in system Settings.
    var refreshKey by remember { mutableIntStateOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refreshKey++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val permissions = remember(refreshKey) { readPermissionStatus(context) }

    val smsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { refreshKey++ }
    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { refreshKey++ }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            try {
                val json = viewModel.buildExportJson()
                context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
                    ?: throw IOException("openOutputStream returned null")
                viewModel.onExportCompleted()
            } catch (e: IOException) {
                viewModel.onExportFailed()
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            try {
                val rawJson = context.contentResolver.openInputStream(uri)
                    ?.bufferedReader()?.use { it.readText() }
                    ?: throw IOException("openInputStream returned null")
                viewModel.onImportFileSelected(rawJson)
            } catch (e: IOException) {
                viewModel.onImportReadFailed()
            }
        }
    }

    SettingsContent(
        state = state,
        permissions = permissions,
        onFixSms = {
            smsLauncher.launch(arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS))
        },
        onFixNotifications = {
            if (Build.VERSION.SDK_INT >= 33) {
                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                context.startActivity(
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                )
            }
        },
        onFixBattery = {
            context.startActivity(
                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                    .setData(Uri.parse("package:${context.packageName}"))
            )
        },
        onOpenAppInfo = {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    .setData(Uri.fromParts("package", context.packageName, null))
            )
        },
        onNewSpendsAlertsToggled = viewModel::onNewSpendsAlertsToggled,
        onEightyPercentAlertsToggled = viewModel::onEightyPercentAlertsToggled,
        onOverBudgetAlertsToggled = viewModel::onOverBudgetAlertsToggled,
        onTrendsMonthsShownChanged = viewModel::onTrendsMonthsShownChanged,
        onExportClick = { exportLauncher.launch(defaultBackupFileName()) },
        onImportClick = { importLauncher.launch(arrayOf("application/json")) },
        onConfirmImport = viewModel::onConfirmImport,
        onCancelImport = viewModel::onCancelImport,
        onDismissImportError = viewModel::onDismissImportError,
        onDismissImportSuccess = viewModel::onDismissImportSuccess,
        onDismissExportError = viewModel::onDismissExportError,
        onOpenCategoryRules = onOpenCategoryRules,
        modifier = modifier
    )
}

private fun defaultBackupFileName(): String {
    val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(System.currentTimeMillis())
    return "budget-manager-backup-$date.json"
}

private fun readPermissionStatus(context: Context): PermissionStatusUi {
    val smsGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) ==
        android.content.pm.PackageManager.PERMISSION_GRANTED &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) ==
        android.content.pm.PackageManager.PERMISSION_GRANTED
    val notificationsGranted = NotificationManagerCompat.from(context).areNotificationsEnabled()
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    val batteryUnrestricted = powerManager.isIgnoringBatteryOptimizations(context.packageName)
    return PermissionStatusUi(smsGranted, notificationsGranted, batteryUnrestricted)
}
