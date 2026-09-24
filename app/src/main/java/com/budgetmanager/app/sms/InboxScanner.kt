package com.budgetmanager.app.sms

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Telephony
import androidx.core.content.ContextCompat
import com.budgetmanager.app.core.model.MessageStatus
import com.budgetmanager.app.core.model.SmsMessage
import com.budgetmanager.app.data.datastore.SettingsDataStore
import com.budgetmanager.app.data.repository.MessageRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Catches up on messages missed while the app was closed - the receiver killed mid-work, or a
 * message that arrived before SMS permission was granted. Runs on app open. Relies on
 * MessageRepository.ingest's dedupeKey check to skip anything the live receiver already caught
 * (R10, R11); DedupeKey.build is shared with SmsReceiver so the same SMS always matches.
 */
@Singleton
class InboxScanner @Inject constructor(
    @ApplicationContext private val context: Context,
    private val messageRepository: MessageRepository,
    private val settings: SettingsDataStore,
    private val notifier: Notifier
) {
    suspend fun scan() {
        if (!hasSmsPermission()) return

        val since = settings.getLastProcessedSmsAt()
        var latestSeen = since
        var insertedCount = 0

        try {
            context.contentResolver.query(
                Telephony.Sms.Inbox.CONTENT_URI,
                arrayOf(Telephony.Sms.ADDRESS, Telephony.Sms.DATE, Telephony.Sms.BODY),
                "${Telephony.Sms.DATE} > ?",
                arrayOf(since.toString()),
                "${Telephony.Sms.DATE} ASC"
            )?.use { cursor ->
                val addressIndex = cursor.getColumnIndex(Telephony.Sms.ADDRESS)
                val dateIndex = cursor.getColumnIndex(Telephony.Sms.DATE)
                val bodyIndex = cursor.getColumnIndex(Telephony.Sms.BODY)
                while (cursor.moveToNext()) {
                    val date = cursor.getLong(dateIndex)
                    val body = cursor.getString(bodyIndex) ?: ""
                    val sender = cursor.getString(addressIndex) ?: "unknown"
                    latestSeen = maxOf(latestSeen, date)

                    if (!SpendClassifier.isSpendLike(body)) continue

                    val message = SmsMessage(
                        id = 0,
                        sender = sender,
                        body = body,
                        receivedAt = Instant.ofEpochMilli(date),
                        smsProviderId = null,
                        dedupeKey = DedupeKey.build(sender, date, body),
                        parsedAmount = null,
                        merchant = null,
                        suggestedCategoryId = null,
                        status = MessageStatus.NOT_ASSIGNED,
                        isNew = true
                    )
                    if (messageRepository.ingest(message) != null) insertedCount++
                }
            }
        } catch (e: Exception) {
            // Never crash on catch-up; the live receiver remains the primary path.
        }

        if (latestSeen > since) settings.setLastProcessedSmsAt(latestSeen)

        if (insertedCount > 0) {
            notifier.showNewSpends(messageRepository.observeNewCount().first())
        }
    }

    private fun hasSmsPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) ==
            PackageManager.PERMISSION_GRANTED
}
