package com.budgetmanager.app.sms

import com.budgetmanager.app.core.model.Money

data class ParsedSpend(
    val amount: Money?,
    val merchant: String?,
    val paymentMethod: String?
)

/**
 * Extracts amount, merchant, and payment method from a spend-like SMS (SpendClassifier has
 * already decided it's worth keeping). Tries known bank formats first (BankRules, verified
 * against a week of this project's own real SMS); when none match, falls back to generic
 * patterns common across many banks' wording, so an unrecognised bank still gets a best-effort
 * read instead of nothing. Written without a full sample collection across every bank the user
 * has (14-implementation-plan.md M2b originally planned to wait for that) - the generic fallback
 * is the trade-off for starting sooner, and should get more specific BankRules over time as
 * mismatches turn up.
 */
object SmsParser {

    private val genericAmountRegex = Regex(
        """(?:\brs\.?|\binr\b|₹)\s*([\d,]+(?:\.\d{1,2})?)""",
        RegexOption.IGNORE_CASE
    )

    // POS/UPI-merchant style: "... at MERCHANT on 18-09-26 ..."
    private val genericMerchantAtRegex = Regex(
        """\bat\s+([A-Za-z0-9][A-Za-z0-9 &.'-]{1,40}?)\s+on\b""",
        RegexOption.IGNORE_CASE
    )

    // Person-to-person/merchant UPI style: "; NAME credited"
    private val genericUpiRecipientRegex = Regex(
        """;\s*([A-Za-z][A-Za-z0-9 .'-]{1,40}?)\s+credited\b""",
        RegexOption.IGNORE_CASE
    )

    private val genericPaymentMethodRegex = Regex(
        """\bvia\s+(UPI|NEFT|IMPS|RTGS|POS|Debit Card|Credit Card)\b""",
        RegexOption.IGNORE_CASE
    )

    fun parse(body: String): ParsedSpend {
        for (rule in BankRules.all) {
            val match = rule.pattern.find(body) ?: continue
            return rule.extract(match)
        }
        return ParsedSpend(
            amount = genericAmountRegex.find(body)?.groupValues?.get(1)?.let { Money.parseSmsAmount(it) },
            merchant = genericMerchantAtRegex.find(body)?.groupValues?.get(1)?.trim()
                ?: genericUpiRecipientRegex.find(body)?.groupValues?.get(1)?.trim(),
            paymentMethod = genericPaymentMethodRegex.find(body)?.groupValues?.get(1)
        )
    }
}
