package com.example.budgetspike

/**
 * Very rough "does this look like a bank spend SMS?" check for the Phase 1 test.
 * It is deliberately simple. Real parsing (Phase 2) will be built from your real messages.
 */
object SpendDetector {

    // "Rs 500", "Rs.500.00", "INR 1,250", "₹99"
    private val amountRegex = Regex(
        """(?:\brs\.?|\binr|₹)\s*([0-9][0-9,]*(?:\.[0-9]{1,2})?)""",
        RegexOption.IGNORE_CASE
    )

    private val debitRegex = Regex(
        """\b(debited|spent|withdrawn|purchase|paid)\b""",
        RegexOption.IGNORE_CASE
    )

    private val debitedRegex = Regex("""\bdebited\b""", RegexOption.IGNORE_CASE)
    private val otpRegex = Regex("""\botp\b|one[- ]time password""", RegexOption.IGNORE_CASE)

    /** The first amount found in the text, such as "1,250.00", or null. */
    fun findAmount(body: String): String? =
        amountRegex.find(body)?.groupValues?.get(1)

    fun isSpendLike(body: String): Boolean {
        if (findAmount(body) == null) return false
        if (!debitRegex.containsMatchIn(body)) return false
        // An OTP message that only mentions a purchase is not a spend.
        // Real debit alerts often say "do not share OTP", so "debited" wins.
        if (otpRegex.containsMatchIn(body) && !debitedRegex.containsMatchIn(body)) return false
        return true
    }
}
