package com.budgetmanager.app.sms

import com.budgetmanager.app.core.model.Money

/** One recognised SMS shape: a regex plus how to read amount/merchant/payment method out of its
 *  capture groups. Tried in order by SmsParser; the first match wins. Add a new bank's format
 *  as its own rule with its own test cases here - never touch another bank's rule
 *  (13-development-best-practices.md, "adding a bank should never touch other banks' rules"). */
data class BankRule(
    val name: String,
    val pattern: Regex,
    val extract: (MatchResult) -> ParsedSpend
)

object BankRules {

    private val dateDDMonYY = """\d{1,2}-\w{3}-\d{2,4}"""
    private val dateDDMMYY = """\d{2}-\d{2}-\d{2,4}"""

    val all: List<BankRule> = listOf(
        // "ICICI Bank Acct XX756 debited for Rs 282.00 on 18-Sep-26; MANAN MANCHANDA credited. UPI:..."
        BankRule(
            name = "ICICI UPI debit to person/merchant",
            pattern = Regex(
                """ICICI Bank (?:Acct|Savings Account|SAVINGS Account) XX\d+ debited for Rs\s*([\d,]+(?:\.\d{1,2})?)\s+on\s+$dateDDMonYY;\s*(.+?)\s+credited\.?\s*UPI""",
                RegexOption.IGNORE_CASE
            ),
            extract = { match ->
                ParsedSpend(
                    amount = Money.parseSmsAmount(match.groupValues[1]),
                    merchant = match.groupValues[2].trim(),
                    paymentMethod = "UPI"
                )
            }
        ),
        // "Rs 6357.00 debited from ICICI Bank Savings Account XX756 on 21-Sep-26 towards
        //  Policybazaar In for Subscription for AutoPay Retrieval Ref No...."
        BankRule(
            name = "ICICI debit towards autopay",
            pattern = Regex(
                """Rs\s*([\d,]+(?:\.\d{1,2})?)\s+debited from ICICI Bank.*?towards\s+(.+?)\s+for\s+""",
                RegexOption.IGNORE_CASE
            ),
            extract = { match ->
                ParsedSpend(
                    amount = Money.parseSmsAmount(match.groupValues[1]),
                    merchant = match.groupValues[2].trim(),
                    paymentMethod = null
                )
            }
        ),
        // "Rs.775.00 spent on your SBI Credit Card ending with 5590 at LULUVALUEMARTC3 on
        //  18-09-26 via UPI (Ref No. 662717134848)."
        BankRule(
            name = "SBI Credit Card spend",
            pattern = Regex(
                """Rs\.?\s*([\d,]+(?:\.\d{1,2})?)\s+spent on your SBI Credit Card.*?\bat\s+(.+?)\s+on\s+$dateDDMMYY(?:\s+via\s+(\w+))?""",
                RegexOption.IGNORE_CASE
            ),
            extract = { match ->
                ParsedSpend(
                    amount = Money.parseSmsAmount(match.groupValues[1]),
                    merchant = match.groupValues[2].trim(),
                    paymentMethod = match.groupValues[3].takeIf { it.isNotBlank() }
                )
            }
        )
    )
}
