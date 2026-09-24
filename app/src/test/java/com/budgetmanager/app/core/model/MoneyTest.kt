package com.budgetmanager.app.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoneyTest {

    @Test
    fun `formats whole rupees with Indian grouping`() {
        assertEquals("₹1,25,000", Money(12_500_000).formatted())
        assertEquals("₹12,345", Money(1_234_500).formatted())
        assertEquals("₹100", Money(10_000).formatted())
        assertEquals("₹0", Money.Zero.formatted())
    }

    @Test
    fun `formats negative amounts with a leading minus before the rupee sign`() {
        assertEquals("-₹2,000", Money(-200_000).formatted())
    }

    @Test
    fun `shows paise only when non-zero`() {
        assertEquals("₹250.50", Money(25_050).formatted())
        assertEquals("₹250", Money(25_000).formatted())
    }

    @Test
    fun `parses a plain rupee amount`() {
        assertEquals(Money(50_000), Money.parseRupeeInput("500"))
    }

    @Test
    fun `parses an amount with paise, including a single decimal digit`() {
        assertEquals(Money(125_050), Money.parseRupeeInput("1250.50"))
        assertEquals(Money(125_050), Money.parseRupeeInput("1250.5"))
    }

    @Test
    fun `rejects empty input`() {
        assertNull(Money.parseRupeeInput(""))
        assertNull(Money.parseRupeeInput("   "))
    }

    @Test
    fun `rejects zero`() {
        assertNull(Money.parseRupeeInput("0"))
        assertNull(Money.parseRupeeInput("0.00"))
    }

    @Test
    fun `rejects negative input`() {
        assertNull(Money.parseRupeeInput("-500"))
    }

    @Test
    fun `rejects non-numeric or malformed input`() {
        assertNull(Money.parseRupeeInput("abc"))
        assertNull(Money.parseRupeeInput("12,34"))
        assertNull(Money.parseRupeeInput("12.345"))
        assertNull(Money.parseRupeeInput("12."))
    }

    @Test
    fun `parses SMS amounts that include comma grouping`() {
        assertEquals(Money(125_000), Money.parseSmsAmount("1,250.00"))
        assertEquals(Money(25_000), Money.parseSmsAmount("250"))
    }
}
