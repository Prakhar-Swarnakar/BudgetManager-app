package com.budgetmanager.app.sms

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.budgetmanager.app.MainActivity
import com.budgetmanager.app.core.model.AlertType
import com.budgetmanager.app.domain.BudgetAlertNotifier
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Shows one notification, "N new spends detected", updated as the count changes. No action
 *  buttons in v1 (backlog). See 04-messages-and-notifications.md. */
@Singleton
class Notifier @Inject constructor(
    @ApplicationContext private val context: Context
) : BudgetAlertNotifier {
    @SuppressLint("MissingPermission") // Checked via areNotificationsEnabled() first.
    fun showNewSpends(count: Int) {
        if (count <= 0) return
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return
        ensureChannel()

        val openApp = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val title = if (count == 1) "1 new spend detected" else "$count new spends detected"
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText("Tap to review")
            .setAutoCancel(true)
            .setContentIntent(openApp)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        manager.notify(NOTIFICATION_ID, notification)
    }

    fun cancel() {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }

    /** One category/type pair fires at most once per month (EvaluateBudgetAlerts enforces
     *  that); a stable id per pair here just stops two different categories' alerts from
     *  overwriting each other in the notification shade. */
    @SuppressLint("MissingPermission") // Checked via areNotificationsEnabled() first.
    override fun showBudgetAlert(categoryId: Long, categoryEmoji: String, categoryName: String, type: AlertType) {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return
        ensureBudgetAlertChannel()

        val title = when (type) {
            AlertType.EIGHTY_PERCENT -> "$categoryEmoji $categoryName is at 80% of its budget"
            AlertType.OVER_BUDGET -> "$categoryEmoji $categoryName is over budget"
        }
        val openApp = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification = NotificationCompat.Builder(context, BUDGET_ALERT_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText("Tap to see the details")
            .setAutoCancel(true)
            .setContentIntent(openApp)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        manager.notify(budgetAlertNotificationId(categoryId, type), notification)
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Spend alerts", NotificationManager.IMPORTANCE_DEFAULT)
                    .apply { description = "Tells you when a new spend SMS arrives" }
            )
        }
    }

    private fun ensureBudgetAlertChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(BUDGET_ALERT_CHANNEL_ID) == null) {
            nm.createNotificationChannel(
                NotificationChannel(BUDGET_ALERT_CHANNEL_ID, "Budget alerts", NotificationManager.IMPORTANCE_DEFAULT)
                    .apply { description = "Tells you when a category reaches 80% or goes over budget" }
            )
        }
    }

    private fun budgetAlertNotificationId(categoryId: Long, type: AlertType): Int =
        BUDGET_ALERT_ID_BASE + (categoryId.toInt() * 2) + type.ordinal

    private companion object {
        const val CHANNEL_ID = "spend_alerts"
        const val NOTIFICATION_ID = 1
        const val BUDGET_ALERT_CHANNEL_ID = "budget_alerts"
        const val BUDGET_ALERT_ID_BASE = 1_000
    }
}
