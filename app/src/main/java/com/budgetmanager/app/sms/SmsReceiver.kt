package com.budgetmanager.app.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.budgetmanager.app.core.model.MessageStatus
import com.budgetmanager.app.core.model.SmsMessage
import com.budgetmanager.app.data.repository.MessageRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Instant

/**
 * Runs when Android delivers an incoming SMS. Kept tiny and safe: classify, save, notify -
 * nothing else. Never throws; a crash here would hide the very thing the app exists to catch.
 *
 * Does not use @AndroidEntryPoint: Hilt's field injection for BroadcastReceiver relies on
 * calling super.onReceive(), but BroadcastReceiver.onReceive() is abstract in the Android SDK
 * and can never be called directly (a real Kotlin compile error, not a workaround-able one -
 * see https://github.com/google/dagger/issues/1918). A manual @EntryPoint sidesteps this
 * entirely, as the architecture doc anticipated ("If this proves fiddly, use a Hilt entry point
 * instead" - 12-tech-architecture.md section 6).
 */
class SmsReceiver : BroadcastReceiver() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Dependencies {
        fun messageRepository(): MessageRepository
        fun notifier(): Notifier
        fun categorySuggester(): CategorySuggester
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val dependencies = EntryPointAccessors.fromApplication(
            context.applicationContext,
            Dependencies::class.java
        )

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                handle(intent, dependencies.messageRepository(), dependencies.notifier(), dependencies.categorySuggester())
            } catch (e: Exception) {
                // Never let a failure here crash the receiver or hide the SMS.
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun handle(
        intent: Intent,
        messageRepository: MessageRepository,
        notifier: Notifier,
        categorySuggester: CategorySuggester
    ) {
        val parts = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (parts.isNullOrEmpty()) return

        val sender = parts[0].originatingAddress ?: "unknown"
        val body = parts.joinToString("") { it.messageBody ?: "" }
        val smsTimestamp = parts[0].timestampMillis

        if (!SpendClassifier.isSpendLike(body)) return

        // Falls back to blank/null fields if nothing matched - the raw body is always kept, so
        // nothing is lost even when a bank's SMS format isn't recognised (see SmsParser).
        val parsed = SmsParser.parse(body)

        val message = SmsMessage(
            id = 0,
            sender = sender,
            body = body,
            receivedAt = Instant.ofEpochMilli(smsTimestamp),
            smsProviderId = null,
            dedupeKey = DedupeKey.build(sender, smsTimestamp, body),
            parsedAmount = parsed.amount,
            merchant = parsed.merchant,
            paymentMethod = parsed.paymentMethod,
            suggestedCategoryId = categorySuggester.suggest(parsed.merchant ?: body),
            status = MessageStatus.NOT_ASSIGNED,
            isNew = true
        )

        messageRepository.ingest(message) ?: return // duplicate, skip (R11)
        val newCount = messageRepository.observeNewCount().first()
        notifier.showNewSpends(newCount)
    }
}
