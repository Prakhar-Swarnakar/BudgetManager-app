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
 * Reads the SMS Provider directly. The receiver killed mid-work, or a message that arrived
 * before SMS permission was granted, are both covered by [scan]. Relies on
 * MessageRepository.ingest's dedupeKey check to skip anything the live receiver already caught
 * (R10, R11); DedupeKey.build is shared with SmsReceiver so the same SMS always matches.
 */
@Singleton
class DefaultInboxScanner @Inject constructor(
    @ApplicationContext private val context: Context,
    private val messageRepository: MessageRepository,
    private val settings: SettingsDataStore,
    private val notifier: Notifier
) : InboxScanner {

    override suspend fun scan() {
        if (!hasSmsPermission()) return

        val lastProcessed = settings.getLastProcessedSmsAt()
        if (lastProcessed == null) {
            // First ever run: nothing has been "missed" yet, since nothing was ever caught.
            // Start the marker at now rather than scanning the phone's whole SMS history -
            // bulk historical import is explicitly out of scope for v1 (see 06-backlog.md).
            settings.setLastProcessedSmsAt(System.currentTimeMillis())
            return
        }

        val latestSeen = scanAndIngest(lastProcessed)
        if (latestSeen != null && latestSeen > lastProcessed) settings.setLastProcessedSmsAt(latestSeen)
    }

    override suspend fun scanFrom(sinceMillis: Long) {
        if (!hasSmsPermission()) return
        scanAndIngest(sinceMillis)
    }

    /** Returns the newest message date seen (even non-spend ones), or null if the query failed. */
    private suspend fun scanAndIngest(since: Long): Long? {
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
            return null
        }

        if (insertedCount > 0) {
            notifier.showNewSpends(messageRepository.observeNewCount().first())
        }
        return latestSeen
    }

    private fun hasSmsPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) ==
            PackageManager.PERMISSION_GRANTED
}
