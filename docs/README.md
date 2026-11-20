# Budget Manager

A simple personal app for managing a monthly budget. Set a budget per category at the start of the month, log what you spend (manually or from bank SMS), and see what is left in each category. Overspending is allowed and shown as a negative number.

- **Platform:** native Android app, single user
- **Data:** stored on the phone only
- **Phase:** Phase 1 (SMS, notification, and install testing) run and mostly confirmed working. M0 through M11 done (project foundation through Trends - This month, Previous month, Historic, and the range setting). M12 (Hardening and release) is next. Several post-v1 rounds since (2026-10-01/02): a real SMS-dedupe fix, possible-duplicate warnings, keyword-rule learning generalised into an explicit "Add to rule" checkbox, Messages split by month with Fetch SMS/Run rule/Clear month, a full payment-method taxonomy (a second classification alongside Category, with its own keyword-rule engine, Add Transaction field, and side-panel page) built via a real schema migration, **Split with friends** (a split step on Add Transaction, a new "Owed to you" bottom-bar tab, and a real schema migration v3→v4, installed and verified on-device), and **generic credit detection** (2026-10-03: a credit SMS gets a message row the same way a debit one does, no sub-type classification yet, a direct accept/reject toggle with no transaction/category/budget touched, and a direction arrow on every row - schema migration v4→v5, installed and verified on-device), and an **Add Transaction redesign** (2026-10-04: reordered fields, "Merchant" renamed "Title" and decoupled from the category/taxonomy rule-learning keyword - which now seeds from the SMS's real merchant instead, surviving a Title rename; manual transactions can no longer add a rule at all; a new shared `RuleLearningSection` component), a split-amount display fix on Messages and Category detail (2026-10-06: a split transaction's row shows your share, with the full amount struck through), and **a real, persisted Transaction title** (2026-10-06: schema migration v5→v6, hand-diffed, rehearsed against a real database copy, then installed and re-verified on the real device - zero data loss confirmed - the linked message's own merchant stays untouched, both rows now show the saved title instead), **richer Messages filters** (2026-10-07: Direction, Category, and Taxonomy now stack with the existing status chips as independent, combinable filters, no schema change - revised same day on feedback that a separate Direction row made the page feel too tall, so status stays the only filter row outside and all three live behind one filter icon inline on that row), **Home shows Income and Saved for the month** (2026-10-07, no schema change: a simple slice of the still-backlogged typed Credit handling system - Income sums every Accepted credit message this month, Saved is Income minus Spent, every accepted credit counts for now since there's no way yet to tell real income apart from a friend settlement or a refund), **the inclusion gate no longer requires an amount** (2026-10-07, no schema change: found by investigating a real report of missing September investments - `SpendClassifier`/`CreditClassifier` were silently dropping genuine transaction SMS that never state an amount inline; fixed by removing the amount requirement while keeping the keyword requirement exactly as strict, plus a related `will be credited` guard added to match the debit side's existing one), **a statement validator, CSV path** (2026-10-09, no schema change: compares an imported bank/card CSV statement against what SMS detection caught - coverage %, a spend cross-check, and five report buckets, the one actionable one pre-filling Add Transaction for a statement line with no matching message at all; a validator, never a second way to add transactions, and nothing is persisted between sessions), **PDF, Excel, and password support for the validator** (2026-10-12, no schema change: reads PDF - including password-protected PDFs - and both `.xls`/`.xlsx` Excel formats, not just CSV; verified on-device against two real statements, cross-checked against an independent manual analysis with exact agreement; found and fixed a real header-detection bug along the way), and **Owed to You's row redesign** (2026-10-13, real schema migration v6→v7: one row per transaction now instead of one per friend, with every friend shown as a tag; a new transaction-level Pending/Reminded swipe status replaces the old per-friend Settle button for now - settling is paused, not removed, planned to move to Add Transaction's edit screen later; a new shared `ActivityRow` component extracted from `MessageRow`'s existing layout, which was rebuilt on it with no behavioural change). **Installed and verified on-device the same day**, which also surfaced and fixed a real `SwipeRow` bug - a stale-closure issue that silently broke a row's reverse swipe after its forward swipe had fired once, likely the same root cause as a long-open "swiping Rejected doesn't revert" report, and **Owed to You's view-by-friend mode and filters** (same day, no schema change: a By transaction/By friend tab toggle, By friend grouping into collapsible per-friend sections with that friend's own tag floated first, plus independent status/friend filter chip rows - all verified on-device) - see the progress tracker in `14-implementation-plan.md` for the full detail. The typed Refund/Settlement/Income sub-system is still backlog - see `06-backlog.md`.
- **Built with:** Kotlin and Jetpack Compose, in Android Studio
- **Code:** private repo at [BudgetManager-app](https://github.com/Prakhar-Swarnakar/BudgetManager-app)
- **Last updated:** 2026-10-13

## Documents

| File | What it covers |
|---|---|
| [01-product-overview.md](01-product-overview.md) | Vision, principles, and the log of decisions made so far |
| [02-features.md](02-features.md) | The v1 feature list |
| [03-user-flows.md](03-user-flows.md) | Step-by-step flows for each main journey |
| [04-messages-and-notifications.md](04-messages-and-notifications.md) | SMS capture, the Messages page, statuses, and the notification |
| [05-budget-rules.md](05-budget-rules.md) | How budgets, remaining amounts, alerts, and trend figures are calculated |
| [06-backlog.md](06-backlog.md) | Ideas deliberately postponed |
| [07-open-questions.md](07-open-questions.md) | Assumptions to confirm and questions still open |
| [08-pages-and-navigation.md](08-pages-and-navigation.md) | Navigation structure and what each page contains |
| [09-risks-and-phases.md](09-risks-and-phases.md) | What could go wrong, ranked by importance, and the four build phases |
| [10-phase-1-guide.md](10-phase-1-guide.md) | Step-by-step guide to build and test the Phase 1 app in Android Studio |
| [11-phase-1-results.md](11-phase-1-results.md) | Blank results sheet to fill in while testing |
| [phase1-test-app](phase1-test-app) | Kotlin source files for the Phase 1 test app |
| [12-tech-architecture.md](12-tech-architecture.md) | Technology choices, architecture, folder structure, reusable components, and data model for the real app |
| [13-development-best-practices.md](13-development-best-practices.md) | Coding, testing, data, privacy, and release rules |
| [14-implementation-plan.md](14-implementation-plan.md) | **Start here to build.** The milestone plan M0 to M12, setup, working method, and next steps |
| [sms-samples-template.txt](sms-samples-template.txt) | Template for collecting real bank SMS for the parser tests |

## UI mockups

Rendered screens are in the [ui-mockups](ui-mockups) folder. `00-overview.html` (open it in a browser) shows every current screen on one page, grouped by daily-use/side-panel/sheets - rebuilt 2026-10-07 from the real Compose source and checked against real on-device screenshots, replacing the earlier per-page PNGs. See [08-pages-and-navigation.md](08-pages-and-navigation.md) for what each page covers.

## Status labels used in these docs

- **Decided:** you confirmed it.
- **Assumed:** a reasonable default I filled in. Please confirm or correct.
- **Backlog:** agreed to postpone; not in v1.
