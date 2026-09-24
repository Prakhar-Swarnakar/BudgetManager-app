package com.example.budgetspike

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony

/**
 * Runs when Android delivers an incoming SMS to the app.
 * Kept tiny on purpose: log the message, and show the notification if it looks like a spend.
 */
class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        try {
            val parts = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            if (parts == null || parts.isEmpty()) return

            // A long SMS arrives in several parts. Join them.
            val sender = parts[0].originatingAddress ?: "unknown"
            val body = parts.joinToString("") { it.messageBody ?: "" }
            val smsTime = parts[0].timestampMillis
            val now = System.currentTimeMillis()

            val spend = SpendDetector.isSpendLike(body)
            SpendLog.add(
                context,
                LogEntry(
                    sender = sender,
                    receivedAt = now,
                    smsTimestamp = smsTime,
                    spendLike = spend,
                    amount = if (spend) SpendDetector.findAmount(body) else null,
                    // Privacy: keep the text only when it looks like a spend.
                    text = if (spend) body else "(text not kept: not spend-like)",
                    seen = false
                )
            )

            if (spend) {
                Notifier.showSpends(context, SpendLog.unseenSpendCount(context))
            }
        } catch (e: Exception) {
            // Never crash from the receiver. A crash here would hide the very thing we are testing.
        }
    }
}
