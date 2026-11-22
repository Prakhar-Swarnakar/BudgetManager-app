package com.budgetmanager.app.feature.messages

import com.budgetmanager.app.core.model.MessageDirection
import com.budgetmanager.app.core.model.MessageStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class MessageDayGroupsTest {

    private val zone = ZoneId.of("Asia/Kolkata")
    private val today = LocalDate.of(2026, 11, 21)

    private fun row(id: Long, date: LocalDate, hour: Int = 12, spendPaise: Long = 0) = MessageRowUi(
        id = id, sender = "BANK", merchantOrBody = "Shop", amountText = null, paymentMethod = null,
        receivedAt = date.atTime(hour, 0).atZone(zone).toInstant(), status = MessageStatus.ACCEPTED,
        isNew = false, direction = MessageDirection.DEBIT, categoryEmoji = null, categoryName = null,
        taxonomyLabel = null, spendPaise = spendPaise
    )

    @Test
    fun `days are titled Today, Yesterday, then weekday and date`() {
        val groups = groupByDay(
            listOf(
                row(1, today), row(2, today.minusDays(1)), row(3, LocalDate.of(2026, 11, 19)),
                row(4, LocalDate.of(2026, 11, 16))
            ),
            today, zone
        )
        assertEquals(listOf("Today", "Yesterday", "Thu 19 Nov", "Mon 16 Nov"), groups.map { it.title })
    }

    @Test
    fun `rows of one day share a group and keep their order`() {
        val groups = groupByDay(listOf(row(1, today, hour = 15), row(2, today, hour = 9)), today, zone)
        assertEquals(1, groups.size)
        assertEquals(listOf(1L, 2L), groups.single().rows.map { it.id })
    }

    @Test
    fun `days with no messages get no group`() {
        val groups = groupByDay(listOf(row(1, today), row(2, today.minusDays(5))), today, zone)
        assertEquals(2, groups.size)
    }

    @Test
    fun `a past month has dates only, never Today or Yesterday`() {
        val oct = LocalDate.of(2026, 10, 30)
        val groups = groupByDay(listOf(row(1, oct), row(2, oct.minusDays(1))), today, zone)
        assertEquals(listOf("Fri 30 Oct", "Thu 29 Oct"), groups.map { it.title })
    }

    @Test
    fun `a day's money out adds up your share of each spend`() {
        val groups = groupByDay(
            listOf(row(1, today, spendPaise = 110_000), row(2, today, spendPaise = 23_800), row(3, today)),
            today, zone
        )
        assertEquals(133_800L, groups.single().outPaise)
    }

    @Test
    fun `no rows means no groups`() {
        assertTrue(groupByDay(emptyList(), today, zone).isEmpty())
    }
}
