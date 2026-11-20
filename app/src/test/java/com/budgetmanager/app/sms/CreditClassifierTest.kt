package com.budgetmanager.app.sms

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CreditClassifierTest {

    @Test
    fun `a plain credit alert is credit-like`() {
        val body = "Rs 5,000.00 credited to a/c XX1234 on 24-09-26. Avl bal Rs 12,345.00."
        assertTrue(CreditClassifier.isCreditLike(body))
    }

    @Test
    fun `a credit-card reversal is credit-like`() {
        // Reused from SpendClassifierTest, where this same body correctly fails isSpendLike -
        // under this feature's "no sub-typing yet" decision, a reversal is still money coming in.
        val body = "Rs.152.99 has been credited to your credit card ending with 5590 as reversal of UPI trxn. dated 23-09-26."
        assertTrue(CreditClassifier.isCreditLike(body))
    }

    @Test
    fun `a debit SMS whose payee clause says credited is credit-like in isolation - the caller must check debit-likeness first`() {
        // This is the exact false-positive risk flagged in 06-backlog.md. CreditClassifier alone
        // does NOT prevent the misfile - SmsReceiver/DefaultInboxScanner's if/else-if ordering
        // (SpendClassifier.isSpendLike checked first) is what actually prevents it. This test
        // documents that isCreditLike is not, by itself, debit-aware.
        val body = "ICICI Bank Acct XX756 debited for Rs 282.00 on 18-Sep-26; TEST PERSON credited. UPI:662745660770. Call 18002662 for dispute."
        assertTrue(SpendClassifier.isSpendLike(body))
        assertTrue(CreditClassifier.isCreditLike(body))
    }

    @Test
    fun `an OTP is not credit-like`() {
        assertFalse(CreditClassifier.isCreditLike("Your OTP is 123456. Do not share it with anyone."))
    }

    @Test
    fun `a message with an amount but no credit word is not credit-like`() {
        assertFalse(CreditClassifier.isCreditLike("Recharge of INR 99.00 is successful for your mobile."))
    }

    @Test
    fun `a message with neither an amount nor a credit word is not credit-like`() {
        assertFalse(CreditClassifier.isCreditLike("Your account statement is ready to view."))
    }

    @Test
    fun `a credit word with no amount at all is still credit-like`() {
        // Mirrors SpendClassifier's same real-world gap - a confirmation that money arrived
        // without ever stating the amount inline should still be kept, not silently dropped.
        val body = "Good news - your refund request has been received and is being processed by our team."
        assertTrue(CreditClassifier.isCreditLike(body))
    }

    @Test
    fun `a - will be credited - pre-notice is not credit-like`() {
        // Mirrors SpendClassifier's "will be debited" guard - a pre-notice describing money that
        // hasn't arrived yet is not a completed credit, same reasoning as the debit side.
        val body = "Rs 2500.00 will be credited to your account XX1234 on 15-Oct-26 as per your request."
        assertFalse(CreditClassifier.isCreditLike(body))
    }
}
