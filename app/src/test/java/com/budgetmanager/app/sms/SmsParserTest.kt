package com.budgetmanager.app.sms

import com.budgetmanager.app.core.model.Money
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Rules verified against a week of this project's own real SMS (2026-09-25), with account
 * numbers already bank-masked in the original text and personal names replaced by placeholders
 * before being committed here (13-development-best-practices.md - real names never go in the
 * repository, even a private one).
 */
class SmsParserTest {

    @Test
    fun `ICICI UPI debit to a person is parsed`() {
        val body = "ICICI Bank Acct XX756 debited for Rs 282.00 on 18-Sep-26; TEST PERSON credited. UPI:662745660770. Call 18002662 for dispute. SMS BLOCK 756 to 9215676766."
        val parsed = SmsParser.parse(body)

        assertEquals(Money.ofRupees(282), parsed.amount)
        assertEquals("TEST PERSON", parsed.merchant)
        assertEquals("UPI", parsed.paymentMethod)
    }

    @Test
    fun `ICICI UPI debit to a merchant with a lowercase handle is parsed`() {
        val body = "ICICI Bank Acct XX756 debited for Rs 74.00 on 18-Sep-26; testvendor123  credited. UPI:626188320190. Call 18002662 for dispute."
        val parsed = SmsParser.parse(body)

        assertEquals(Money.ofRupees(74), parsed.amount)
        assertEquals("testvendor123", parsed.merchant)
        assertEquals("UPI", parsed.paymentMethod)
    }

    @Test
    fun `ICICI autopay debit is parsed with the vendor as merchant`() {
        val body = "Rs 6357.00 debited from ICICI Bank Savings Account XX756 on 21-Sep-26 towards Test Vendor for Subscription for AutoPay Retrieval Ref No.626402879112"
        val parsed = SmsParser.parse(body)

        assertEquals(Money.ofRupees(6357), parsed.amount)
        assertEquals("Test Vendor", parsed.merchant)
    }

    @Test
    fun `SBI Credit Card spend is parsed with amount, merchant, and via-UPI`() {
        val body = "Rs.775.00 spent on your SBI Credit Card ending with 5590 at BLINKIT on 18-09-26 via UPI (Ref No. 662717134848). Trxn. not done by you? Report at https://sbicard.com/Dispute"
        val parsed = SmsParser.parse(body)

        assertEquals(Money.ofRupees(775), parsed.amount)
        assertEquals("BLINKIT", parsed.merchant)
        assertEquals("UPI", parsed.paymentMethod)
    }

    @Test
    fun `SBI Credit Card spend with decimal paise is parsed`() {
        val body = "Rs.152.99 spent on your SBI Credit Card ending with 5590 at ZOMATO on 23-09-26 via UPI (Ref No. 626680789553)."
        val parsed = SmsParser.parse(body)

        assertEquals(Money(15299), parsed.amount)
        assertEquals("ZOMATO", parsed.merchant)
    }

    @Test
    fun `an unrecognised bank format still gets a best-effort amount and merchant`() {
        val body = "Rs 250 debited from your account at TEST STORE on 24-09-26. Avl bal Rs 5000."
        val parsed = SmsParser.parse(body)

        assertEquals(Money.ofRupees(250), parsed.amount)
        assertEquals("TEST STORE", parsed.merchant)
    }

    @Test
    fun `an unrecognised UPI-style format falls back to the recipient after the semicolon`() {
        val body = "Your account XX999 is debited INR 400.00; TEST RECIPIENT credited via a partner bank."
        val parsed = SmsParser.parse(body)

        assertEquals(Money.ofRupees(400), parsed.amount)
        assertEquals("TEST RECIPIENT", parsed.merchant)
    }

    @Test
    fun `nothing recognisable at all still returns a null merchant, never a crash`() {
        val parsed = SmsParser.parse("Rs 99 debited for something unusual with no known shape.")

        assertEquals(Money.ofRupees(99), parsed.amount)
        assertNull(parsed.merchant)
    }
}
