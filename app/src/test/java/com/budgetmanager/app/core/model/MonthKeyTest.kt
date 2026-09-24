package com.budgetmanager.app.core.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.ZoneOffset

class MonthKeyTest {

    private val zone = ZoneOffset.UTC

    @Test
    fun `formats as yyyy-MM`() {
        assertEquals("2026-09", MonthKey.of(2026, 9).value)
        assertEquals("2026-01", MonthKey.of(2026, 1).toString())
    }

    @Test
    fun `31 December belongs to December, 1 January belongs to January`() {
        val dec31 = Instant.parse("2025-12-31T23:59:00Z")
        val jan1 = Instant.parse("2026-01-01T00:01:00Z")
        assertEquals(MonthKey.of(2025, 12), MonthKey.from(dec31, zone))
        assertEquals(MonthKey.of(2026, 1), MonthKey.from(jan1, zone))
    }

    @Test
    fun `next month after December rolls into January of the next year`() {
        assertEquals(MonthKey.of(2026, 1), MonthKey.of(2025, 12).next())
    }

    @Test
    fun `previous month before January rolls back to December of the previous year`() {
        assertEquals(MonthKey.of(2025, 12), MonthKey.of(2026, 1).previous())
    }

    @Test
    fun `handles a leap February correctly`() {
        val leapDay = Instant.parse("2028-02-29T12:00:00Z") // 2028 is a leap year
        assertEquals(MonthKey.of(2028, 2), MonthKey.from(leapDay, zone))
        assertEquals(MonthKey.of(2028, 3), MonthKey.of(2028, 2).next())
    }

    @Test
    fun `11-59pm on the last day of the month still belongs to that month`() {
        val lastMinuteOfApril = Instant.parse("2026-04-30T23:59:00Z")
        assertEquals(MonthKey.of(2026, 4), MonthKey.from(lastMinuteOfApril, zone))
    }
}
