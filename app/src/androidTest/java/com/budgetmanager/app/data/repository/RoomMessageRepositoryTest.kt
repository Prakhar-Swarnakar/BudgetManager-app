package com.budgetmanager.app.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.budgetmanager.app.core.model.MessageStatus
import com.budgetmanager.app.core.model.Money
import com.budgetmanager.app.core.model.SmsMessage
import com.budgetmanager.app.data.database.AppDatabase
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

/**
 * Confirms sms_message.payment_method (added for M2b's real SMS parser) round-trips through
 * Room correctly - the schema bump this needed (MIGRATION_1_2) is a single, standard
 * `ALTER TABLE ... ADD COLUMN`, verified by hand against the exported v1/v2 schemas rather than
 * with Room's MigrationTestHelper: that tool hit a genuine upstream bug in this project's
 * dependency state (Room 2.8.5's room-testing artifact ships precompiled kotlinx-serialization
 * $$serializer classes that throw AbstractMethodError against the kotlinx-serialization-core
 * version Room itself pins - not something fixable from this app's build file, since Room's own
 * module metadata strictly constrains that version). Revisit if a later Room release fixes it.
 */
@RunWith(AndroidJUnit4::class)
class RoomMessageRepositoryTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: MessageRepository

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).build()
        repository = RoomMessageRepository(database.smsMessageDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun paymentMethod_roundTripsThroughRoom() = runTest {
        val message = SmsMessage(
            id = 0, sender = "BANK", body = "Rs 100 debited", receivedAt = Instant.now(),
            smsProviderId = null, dedupeKey = "k1", parsedAmount = Money.ofRupees(100),
            merchant = "Test Merchant", paymentMethod = "UPI", suggestedCategoryId = null,
            status = MessageStatus.NOT_ASSIGNED, isNew = true
        )

        val id = repository.ingest(message)!!
        val stored = repository.getById(id)!!

        assertEquals("UPI", stored.paymentMethod)
    }

    @Test
    fun paymentMethod_isNullWhenNotSet_matchingTheMigratedDefault() = runTest {
        val message = SmsMessage(
            id = 0, sender = "BANK", body = "Rs 100 debited", receivedAt = Instant.now(),
            smsProviderId = null, dedupeKey = "k2", parsedAmount = Money.ofRupees(100),
            merchant = "Test Merchant", suggestedCategoryId = null,
            status = MessageStatus.NOT_ASSIGNED, isNew = true
        )

        val id = repository.ingest(message)!!
        val stored = repository.getById(id)!!

        assertNull(stored.paymentMethod)
    }
}
