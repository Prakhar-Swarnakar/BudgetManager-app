# Backlog

Items agreed to postpone, or proposed and not yet decided. None of these are in v1. Grouped by
theme; the Status column keeps the postpone/proposed distinction that used to be two separate
sections.

## SMS & spend detection

| Item | Status | Notes |
|---|---|---|
| Review the SMS parser | Proposed | M2b (2026-09-25) covers ICICI and SBI precisely from one week of real SMS; every other bank falls back to a generic pattern (amount is usually right, merchant/payment method less so). Revisit once more real SMS has come in: add precise `BankRules` for any bank that's landing wrong, re-check category-suggestion accuracy against what the parser actually extracts, and re-check the generic fallback against whatever it's missing. Also revisit whether `MigrationTestHelper` works again on a later Room release (see R23 in [09-risks-and-phases.md](09-risks-and-phases.md)) |
| Duplicate detection | Built 2026-10-01, pending your on-device test | Flags two *different* real messages for one real payment (for example a bank alert and a UPI app confirmation both arriving for the same purchase) - same amount, different sender, within 10 minutes (`DetectPossibleDuplicates`). A warning badge on the row, and the matching message named in the detail sheet; never auto-rejects anything. Distinct from the exact-SMS-counted-twice bug fixed the same day (see `DedupeKey.kt`) - that was the same physical message processed twice; this is two genuinely different messages describing the same real-world transaction |
| Payment method taxonomy | Proposed | Turn `paymentMethod` from loose regex-extracted text into a fixed, filterable set: UPI, Credit Card, Debit Card, **Credit Card via UPI** (a RuPay credit card linked to GPay/PhonePe etc. - increasingly common, and the hardest to detect since the SMS often just says "UPI" even though a credit card funded it), Bank Transfer (NEFT/IMPS/RTGS), Wallet, Cash, Other. Lets you later see a Trends-style breakdown by payment method ("how much went on credit card this month"). Needs bank-by-bank detection rules similar to `BankRules`, especially for the Credit Card via UPI case |
| Learning from choices | Built 2026-10-01, pending your on-device test | Saving an accepted message's transaction remembers its merchant as a keyword mapped to whichever category was actually chosen, even when it overrides the suggestion - so the same merchant suggests correctly next time. Merchant text under 3 characters is never learned (too likely to misfire against an unrelated future message) |
| Editable keyword rules | Built 2026-10-02, pending your on-device test | A list/add/edit/delete page under Settings → "Category rules", grouped by category (each category is a section with its words as removable chips, plus a "+ add" chip - including categories with no rules yet, so an empty one is still visible and easy to seed). Chose this over a flat alphabetical list after mocking up both - answers "what triggers Food?" at a glance, which was the more natural question. Tapping a chip reassigns its category; the keyword itself can't be edited in place (it's the rule's primary key) |

## Split with friends & credits

| Item | Status | Notes |
|---|---|---|
| Split with friends | Proposed | You front an expense (dinner, groceries) that's really shared. Adding friends and a split (equal or custom amounts) to a transaction keeps the **full amount** as what your bank actually shows, but only **your share** counts toward your budget - the rest shows as owed back to you on a new "Owed to you" page (one row per friend per transaction, with a settle action), so your budget isn't inflated by money you're only holding, and you have a reminder to collect it. Settling a friend's share just closes that receivable - it doesn't touch the category again, since the split already excluded that portion when it was created. The reverse case (paying a friend back for a shared expense *they* fronted) needs no special handling - it's just a normal transaction from you, categorised like anything else; maybe worth an optional "reimbursement" tag for your own memory, nothing more. Ties into Credit handling below: an incoming credit that matches an outstanding "owed to you" amount should auto-suggest itself as a settlement for that friend/transaction. Needs new data - a Friend entity and a split/settlement table - and new UI: the split step on Add/Edit transaction, and the "Owed to you" page itself. The biggest addition since v1 shipped; wants its own design pass (participant entity, settle flow, how/whether it shows up in Trends) before scheduling |
| Credit handling | Proposed | When a credit SMS arrives, auto-guess its type the same way debits are classified, then wait for the user to accept/reject the guess - never silent: **Refund** (return/cancellation wording) reduces that category's spend, since the money was never really kept spent; **Settlement** (amount matches an outstanding "Owed to you" entry - see Split with friends above) closes that receivable without re-touching the category, since the split already excluded that portion from budget when it was created; **Income** (salary-looking sender/wording) is tracked separately and never touches any category; **Ignored** is today's v1 default (cashback, anything not worth tracking). Needs the same kind of SMS classification work as debit messages (SpendClassifier/SmsParser), plus the amount-matching logic shared with Split with friends |
| Credit card / bank statement import | Proposed, to discuss further | A **validator**, not a primary import path: import a credit card or bank statement (PDF or CSV) and compare it against what SMS detection actually caught, flagging any gap between the two - a correctness check on the app's own detection accuracy, not a second way to add transactions. Whether it should also auto-create the missing transactions it finds (vs. flag-only, for you to add manually) and which statement formats to support are both still open - revisit and scope properly before building |

## Trends

| Item | Status | Notes |
|---|---|---|
| Like-for-like comparison | Proposed | Compare this month so far with the same days of last month, instead of the full previous month |
| Category history in Trends | Proposed | Tap a category to see its own month-by-month history |
| Grouping small slices | Proposed | If there are many categories, combine the smallest slices in the This month chart into "Others". Confirmed worth doing during M10 on-device testing (2026-09-25): with 10+ budgeted categories and an uneven spread (one ~30% category, several under 5%), the small slices' emoji labels crowd together near the ring's start/end seam. Not a bug - each label is at its mathematically correct midpoint - just a readability issue once a real category list gets long and uneven |

## Messages & notifications

| Item | Status | Notes |
|---|---|---|
| Notification action buttons | Agreed to postpone | For example Confirm or Change directly from the notification. v1 uses a simple "new spends detected" notification only |
| SMS burst grouping | Agreed to postpone | Combine many messages that arrive together into one notification |
| Daily summary reminder | Proposed | One gentle reminder such as "3 messages need review" |
| Archive old messages | Proposed | Move accepted and rejected messages out of the main list after some time. In v1 they stay, and the filter chips manage the list |

## Backup and restore

| Item | Status | Notes |
|---|---|---|
| Merge on restore | Proposed | Let Import merge a backup into existing data instead of replacing it |
| Backup reminder | Proposed | A gentle nudge if the last export was more than about 30 days ago |

## Categories & transactions

| Item | Status | Notes |
|---|---|---|
| Archive categories | Proposed | Built once (M5, 2026-09-25: swipe left, a collapsed Archived section) and then removed from v1 the same day, at the user's request, along with the separate Categories page it lived on. Every category now stays visible and reorderable on Monthly budget forever. Could come back later, scoped to a real need rather than by default |
| All transactions list | Proposed | A single page of every transaction across categories, with search. In v1, transactions are reached through their category's detail page |

## Later ideas

- Recurring bills and subscriptions
- Optional rollover per category
- Cloud backup or a web view
- Savings goals
