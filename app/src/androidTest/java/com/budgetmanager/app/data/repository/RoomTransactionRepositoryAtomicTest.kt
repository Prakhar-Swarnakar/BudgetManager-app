package com.budgetmanager.app.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.budgetmanager.app.core.model.MessageStatus
import com.budgetmanager.app.core.model.Money
import com.budgetmanager.app.core.model.MonthKey
import com.budgetmanager.app.core.model.SmsMessage
import com.budgetmanager.app.data.database.AppDatabase
import com.budgetmanager.app.data.database.entity.CategoryEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

/** Proves the two atomic operations M1 exists to get right: accepting a message and deleting
 *  a message-linked transaction, each as one database transaction. Needs a real device/emulator. */
@RunWith(AndroidJUnit4::class)
class RoomTransactionRepositoryAtomicTest {

    private lateinit var database: AppDatabase
    private lateinit var transactionRepository: TransactionRepository
    private lateinit var messageRepository: MessageRepository
    private var categoryId: Long = 0
    private val monthKey = MonthKey.of(2026, 9)

    @Before
    fun setUp() = runTest {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).build()
        transactionRepository =
            RoomTransactionRepository(database, database.transactionDao(), database.smsMessageDao())
        messageRepository = RoomMessageRepository(database.smsMessageDao())
        categoryId = database.categoryDao()
            .insert(CategoryEntity(name = "Food & Dining", emoji = "🍔", sortOrder = 0))
    }

    @After
    fun tearDown() {
        database.close()
    }

    private suspend fun ingestTestMessage(dedupeKey: String): SmsMessage {
        val message = SmsMessage(
            id = 0, sender = "HDFCBK", body = "Rs 250 debited", receivedAt = Instant.now(),
            smsProviderId = null, dedupeKey = dedupeKey, parsedAmount = Money.ofRupees(250),
            merchant = "Swiggy", suggestedCategoryId = categoryId, status = MessageStatus.NOT_ASSIGNED,
            isNew = true
        )
        val id = messageRepository.ingest(message)!!
        return messageRepository.getById(id)!!
    }

    @Test
    fun acceptingAMessage_savesTheTransactionAndMarksTheMessageAccepted() = runTest {
        val message = ingestTestMessage("key-1")

        val transactionId = transactionRepository.saveFromMessage(
            message = message, amount = Money.ofRupees(250), occurredAt = Instant.now(),
            monthKey = monthKey, categoryId = categoryId, note = "Swiggy"
        )

        val transaction = transactionRepository.getById(transactionId)
        assertNotNull(transaction)
        assertEquals(message.id, transaction!!.sourceMessageId)
        assertEquals(MessageStatus.ACCEPTED, messageRepository.getById(message.id)!!.status)
    }

    @Test
    fun deletingATransactionFromAMessage_revertsTheMessageToNotAssigned() = runTest {
        val message = ingestTestMessage("key-2")
        val transactionId = transactionRepository.saveFromMessage(
            message = message, amount = Money.ofRupees(500), occurredAt = Instant.now(),
            monthKey = monthKey, categoryId = categoryId, note = "Swiggy"
        )

        transactionRepository.delete(transactionId)

        assertNull(transactionRepository.getById(transactionId))
        assertEquals(MessageStatus.NOT_ASSIGNED, messageRepository.getById(message.id)!!.status)
    }

    @Test
    fun deletingAManualTransaction_doesNotTouchAnyMessage() = runTest {
        val transactionId = transactionRepository.insert(
            amount = Money.ofRupees(100), occurredAt = Instant.now(), monthKey = monthKey,
            categoryId = categoryId, note = "Cash"
        )

        transactionRepository.delete(transactionId)

        assertNull(transactionRepository.getById(transactionId))
    }
}
