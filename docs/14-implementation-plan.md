# Implementation Plan

This is the single plan for building the real app. It pulls together the decisions in [01](01-product-overview.md), the features and flows in [02](02-features.md) and [03](03-user-flows.md), the rules in [04](04-messages-and-notifications.md) and [05](05-budget-rules.md), the pages in [08](08-pages-and-navigation.md), the risks in [09](09-risks-and-phases.md), the architecture in [12](12-tech-architecture.md), and the rules in [13](13-development-best-practices.md). Where this document and an older one disagree, this one and the newer decisions in [07](07-open-questions.md) win.

**Status on 25 September 2026:** Phase 1 run and mostly confirmed. M0 to M3 done. M4 (Add transaction and the accept flow) built and mostly confirmed on-device; one item (the Cancel button) has a fix shipped but not yet confirmed - see the progress tracker and the "still open" list in [07-open-questions.md](07-open-questions.md). M5 (Categories) was built, confirmed on-device, and then removed the same day - its rename and reorder capabilities were folded into M6, and archiving was dropped from v1 entirely; there is no separate Categories page. M6 (Monthly budget), including the fold-in, confirmed on-device. M7 (Home and Category detail) built, not yet tested on-device. M8 (Alerts and Settings) is next.

## 1. What we are building

A private Android app, for one person, that answers "how much is left in each category this month?". You set a budget per category for each calendar month. Bank SMS are picked up on the phone and appear on a Messages page, where you swipe to accept or reject them. Accepted ones become transactions, which reduce the category's remaining amount, even below zero. You get an alert at 80% and when over budget. Trends show budget use over time. All data stays on the phone, with a manual backup file.

Built with Kotlin, Jetpack Compose, MVVM in three layers, Hilt, Room, DataStore, and coroutines. Details are in [12-tech-architecture.md](12-tech-architecture.md).

## 2. How we build together

**Roles.** I write the code and the tests. You build and run it in Android Studio on your Pixel, because my environment cannot compile Android code, and I cannot reach the Android tools on your computer either. Your computer's shell tool is currently unavailable (a Windows update from 8 September stops Claude's workspace from reaching your files). That does not block us. File transfer still works.

**Getting code into your project.** Two ways, and the first is better:

1. **Connect your project folder to this session** in the Claude desktop app (the folder you create in step M0, for example `C:\Dev\BudgetManagerApp`). Then I write files straight into it, and you only press Sync and Run.
2. **Copy files yourself** from the delivery I send after each batch.

**Rhythm for each milestone.**

1. I tell you what the milestone will change and list the files.
2. I write the code and tests in small batches, at most about eight files at a time, so a build error is easy to trace.
3. You build, run the unit tests, and try the milestone's "Done when" checks on your phone.
4. If the build fails, paste the **first** error from Android Studio's Build panel. I fix it.
5. When the checks pass, you commit to Git with the milestone tag (for example `m3-messages-page`), and I tick the milestone in the tracker below and update any doc that changed.

**Reducing build errors without a compiler.** I use only stable, documented APIs. Library versions come from Android Studio's own new-project template and each library's official release page, never from memory. Each batch is small and self-contained. Tests that can run on your laptop come first, so a wrong assumption shows up quickly.

## 3. What changed from the earlier plan

I changed the earlier four-phase plan in [09](09-risks-and-phases.md) so that you get a working app sooner and take fewer risks. The four phases remain, but each is now cut into vertical milestones, and every milestone ends with something you can run on the phone.

| Change | Why |
|---|---|
| Each milestone is a thin vertical slice (data, logic, screen) rather than a layer | You can test on the phone at every step, and errors stay local |
| The SMS work is split: receive and store raw text first, parse later | Receiving needs no sample messages, so it can start before you collect samples |
| The permission screen moves into the SMS milestone (M2) | You cannot receive SMS without it, and it reuses what Phase 1 teaches |
| Backup and restore comes before Trends, at the end of Phase 3 | Real data should be protected before you rely on the app. You can start using it daily after M9 |
| Trends is last | It is the least urgent and the most drawing work |
| A Category detail page is added | Without it, there was no way to edit or delete a transaction |
| Starter categories and keywords are seeded | First launch takes a minute instead of an hour, and suggestions work on day one |
| WorkManager is not used in v1 | The receiver plus a catch-up scan cover the need, and it means one fewer library |
| Custom-drawn charts, no chart library | The donut with a "used" part is not a stock chart |
| Parser, money, and month logic are written and tested first | These are the places where a silent bug gives you wrong numbers |

## 4. Roadmap

| ID | Milestone | Phase | Size | Needs from you |
|---|---|---|---|---|
| M0 | Project foundation | 2 | S | Phase 1 results, application ID confirmed |
| M1 | Domain core and database | 2 | M | Nothing |
| M2 | SMS pipeline | 2 | L | Sample SMS, for the parser step only |
| M3 | Messages page | 2 | M | Nothing |
| M4 | Add transaction and the accept flow | 2 | M | Nothing |
| M5 | ~~Categories~~ Removed 2026-09-25 - folded into M6 (see the progress tracker) | 3 | S | Nothing |
| M6 | Monthly budget | 3 | M | Nothing |
| M7 | Home and Category detail | 3 | M | Nothing |
| M8 | Alerts and Settings | 3 | M | Nothing |
| M9 | Backup and restore | 3 | M | Nothing |
| M10 | Trends: This month | 4 | M | Nothing |
| M11 | Trends: Previous month, Historic, and range setting | 4 | M | Nothing |
| M12 | Hardening and release | 4 | M | A week of real use |

Sizes are relative effort, not time: S is a few files, M is a screen with logic and tests, L has real complexity and needs iteration on real data.

### Progress tracker

| ID | Status | Date | Notes |
|---|---|---|---|
| Phase 1 test app | Run (Parts A-F). Results not yet written up in full | 2026-09-24 | R1 confirmed: USB/ADB install works with no Play Protect warning; copying the APK to the phone and installing from Downloads was blocked outright. F4 (update with same key) confirmed data survives an in-place update. "App swiped away" background row confirmed working (also caught a genuine Bedtime/DND-suppresses-notification finding, unrelated to the app itself). Reboot, force-stop, screen-off-an-hour, second SIM rows still to run - useful before M2b, no longer blocking since M2a/M2c are already built and proven |
| M0 | Done | 2026-09-24 | Project foundation: Hilt+KSP+Room toolchain proven, brand theme, Navigation 3 shell (bottom bar + side panel), 6 placeholder screens |
| M1 | Done | 2026-09-24 | Domain core (Money/MonthKey/BudgetStatus), Room schema v1 with seeded starter categories/keywords, repository layer with atomic accept/delete. 17 unit tests + 5 instrumented tests passing |
| M2 | 2a and 2c done, 2025-09-25. 2b (per-bank parser) blocked on sample SMS | 2026-09-25 | Real SmsReceiver (via a manual Hilt @EntryPoint, not @AndroidEntryPoint - BroadcastReceiver.onReceive() is abstract in the Android SDK and can never call super, a genuine Dagger limitation, not a workaround choice), PermissionGate, Notifier, InboxScanner catch-up scan. Verified end-to-end against a real ICICI Bank SMS, not just test strings: classified, stored, notified, checked directly in the database. Fixed a first-run bug before it could bulk-import the phone's SMS history (out of scope per 06-backlog.md) |
| M3 | Done, 2026-09-25 | 2026-09-25 | Messages page: list, filter chips, swipe gestures, detail sheet, red tab badge. MessagesViewModel tested against a fake repository (filter counts, reject+undo, new-flag clearing). Fixed three real on-device UX bugs: filter chips clipped off-screen (needed horizontal scroll), swipe left/right leaving a stuck full-colour background (fixed by vetoing the dismiss instead of manually resetting it), and swiping sometimes opening the side panel (ModalNavigationDrawer's own edge-swipe gesture competing with the row's - disabled since the hamburger button already opens it). Added a temporary, BuildConfig.DEBUG-gated test-only accept state machine (swipe marks Accepted directly instead of navigating to Add Transaction) and two debug tools ("Import today's SMS", "Add test message") to make manual testing fast before M4 exists - all clearly marked for replacement when M4 lands |
| M4 | Built and mostly confirmed; one item pending retest | 2026-09-25 | Real AddTransactionScreen wired up (replacing M3's debug accept state machine), pre-fill from a message (amount, suggested category, SMS banner), manual add, edit mode, Cancel button, atomic saveFromMessage. Fixed a real on-device crash (`UNIQUE constraint failed: transactions.source_message_id`, traced via `adb logcat`) caused by the debug "reset to Not assigned" tool desyncing a message from its transaction - fixed by removing that debug tool and making saveFromMessage idempotent (update-in-place if a transaction already exists for that message), verified with a new instrumented test against real Room. Finalised the swipe state machine to a symmetric rule per the user's explicit correction: only Not assigned can become Accepted or Rejected; Accepted/Rejected only revert to Not assigned (see [04-messages-and-notifications.md](04-messages-and-notifications.md#gestures), row 7 in [07-open-questions.md](07-open-questions.md)). Made the swipe reveal strip match the real per-status action instead of always showing accept/reject colours. Remaining: confirm the Add Transaction Cancel button (reported needing 3 taps; a keyboard-dismiss fix shipped in `d82d4a8` without a confirmed root cause) - tracked in the "still open" list in [07-open-questions.md](07-open-questions.md) |
| M5 | Done, confirmed on-device 2026-09-25, then removed the same day - see M6 | 2026-09-25 | Categories page: a plain-Column list (drag handle to reorder, swipe left to archive), a collapsed Archived section that expands and restores on tap, and a bottom sheet shared between "new category" and "rename/change icon" (EmojiIconBox + name). The + button lived in the top bar via a new `actions` slot on AppScaffold. Reordering was local-only until drag end, then persisted in one batch write (CategoryRepository.reorder, CategoryDao.updateAll). Single-emoji validation (EmojiValidation, pure Kotlin, grapheme-cluster based) had tests for skin tones, ZWJ families, and flags. Fixed two real on-device bugs from first testing: dragging got stuck mid-gesture (the per-row list had no `key()`, so Compose matched rows by screen position rather than by category - a mid-drag reorder could cancel the drag's own gesture coroutine; fixed by keying each row on its id) and not every category could be archived (the list had no scroll modifier, so rows past the 9 starter categories were off-screen and unreachable; fixed by scoping `verticalScroll` to the populated-list branch only, since `EmptyState`'s `fillMaxSize()` would crash under an unbounded scrollable parent). Retested and confirmed working, then **removed the same day**: the user judged the page redundant once M6 (Monthly budget) covered the same "see and create categories" ground, and asked to fold rename and reorder into Monthly budget and drop archiving entirely. `EmojiValidation`, `MonthSelector`, `AppScaffold`'s `actions` slot, and `CategoryRepository.reorder`/`update` all carried over into M6; `feature/categories/` and `CategoryRepository.setArchived` were deleted outright as dead code once nothing could reach them |
| M6 | Done, confirmed on-device 2026-09-25 (original build, then again after the category-management fold-in from the removed M5) | 2026-09-25 | Monthly budget page: MonthSelector (previous/next, no future restriction here - unlike Home's planned one, setting next month's budget ahead of time is normal), a row per active category with a drag handle (muted ₹0 when unset), a total at the bottom, and one bottom sheet for both "new category" and "edit this category" - editing now covers rename, change icon, and change amount together, since category management folded in from the removed M5. The + button reuses the `actions` slot on AppScaffold that M5 introduced. The + never changes an existing category's amount. New domain use case CopyBudgetFromPreviousMonth: a month with no budget rows yet copies the previous month's in as a starting point (self-gating - a month with any rows, copied or user-edited, won't copy again), with a "Copied from last month" note shown when it happens. Reordering reuses M5's drag mechanics (local state, one write on drag end, rows keyed by id) minus the swipe gesture, since there's no more archive action to share the row with. Unit tests cover the domain use case directly and the ViewModel (zero rows, editing, rename, reorder, the + flow, totals, copy-forward, no-copy-when-nothing-to-copy) |
| M7 | Built, not yet tested on-device | 2026-09-25 | Home: month selector (past viewable, future disabled - the restriction Monthly budget's doesn't have), a summary card (spent of budget, amount left, progress bar), a "N messages to review" line, a card per active category (own progress bar and status word, same order as Monthly budget's list), and a round Add FAB for a manual transaction. Category detail: month selector, a summary card, and that category's transactions (newest first), tap to edit, swipe left to delete with Undo - deleting reverts a linked message to Not assigned, and Undo recreates the transaction exactly, re-accepting its source message via saveFromMessage if it had one, rather than a separate restore code path. New shared pieces: TransactionRepository.observeSpentByCategoryForMonth (one grouped query for all of Home's cards), BudgetProgressBar/BudgetStatusLabel (core/designsystem, ready for Trends), and AppScaffold's `useBackArrow` (pushed screens - Add transaction, Category detail - now get a real back arrow instead of the hamburger that opened the side panel mid-flow, a small inconsistency this also fixed for Add transaction). Unit tests cover both ViewModels: per-card status/remaining, totals, review count, next-month-disabled-at-present, delete+undo for both manual and SMS-linked transactions, month navigation |
| M8 to M12 | Not started | | |

## 5. Milestones in detail

Each milestone lists what gets built, what is tested, and the check you do on your phone.

### M0. Project foundation

**Build**
- A new Android Studio project from the **Empty Activity** template. Name `BudgetManager`, package `com.budgetmanager.app`, Kotlin, minimum SDK 26, in a folder **outside OneDrive**. Run `git init` and add the `.gitignore` from [13](13-development-best-practices.md).
- Version catalog entries and plugins for Hilt, Room, KSP, DataStore, and Navigation (section 6).
- `BudgetApp` (starts Hilt) and `MainActivity` (single Activity).
- Theme: colours from the mockups (blue `#2A78D6` accent, amber `#EDA100` warning, red `#E34948` over budget, green for accepted), text styles, light and dark. `StatusColors` in one file.
- `AppScaffold`, the bottom bar (Home, Messages, Trends), the side panel (Monthly budget, Categories, Settings), and empty placeholder screens.
- `allowBackup` set to `false` and no INTERNET permission (decision 29 in [07](07-open-questions.md)).

**Tests:** the app builds, and a launch smoke test passes.

**Done when:** it installs over USB, the three tabs and the side panel open the placeholders, and dark mode does not crash.

### M1. Domain core and database

**Build**
- Pure Kotlin in `core/model`: `Money` (whole paise; parse and format with Indian grouping, negatives such as -₹2,000), `MonthKey`, `BudgetStatus`, and budget maths (spent, remaining, percent used, the ₹0-budget rule from [05](05-budget-rules.md)).
- Room database version 1 with exported schema: `category`, `monthly_budget`, `transactions`, `sms_message`, `alert_log`, `keyword_rule`.
- A seed callback that inserts the nine starter categories and their keywords.
- Repository interfaces, Room versions, fakes for tests, and Hilt modules.
- `saveFromMessage` and delete-transaction, each in one database transaction.

**Tests:** money formatting (₹1,25,000, -₹2,000, ₹0), amount parsing, month boundaries (31 Dec, leap February, 23:59 on the last day), status thresholds (79%, 80%, 100%, 101%, ₹0 budget), and database tests for the unique keys and for "deleting a transaction returns its message to Not assigned".

**Done when:** all unit tests pass on your laptop, the database tests pass on your phone, and the starter categories exist after a fresh install.

### M2. SMS pipeline (three steps)

**2a. Receive and store (no samples needed)**
- `PermissionGate`: asks for SMS and notifications, explains "Allow restricted settings" using what Phase 1 showed, and links to App info.
- `SmsReceiver` (`goAsync`, never throws, saves raw text if anything fails), the manifest entry, and `MessageRepository.ingest` with the unique `dedupeKey`.
- `Notifier`: one "N new spends detected" notification, updated as the count changes.
- A simple classifier (debit word plus rupee amount, OTP excluded), so only spend-like messages are saved.

**2b. Parser (needs the samples)**
- `SmsParser` and `BankRules`, one small block per bank, plus the sample files as test data.
- `CategorySuggester` using the seeded keywords.

**2c. Catch-up**
- `InboxScanner`, which runs when the app opens, reads the SMS inbox for anything newer than the last processed message (kept in DataStore), and relies on `dedupeKey` to avoid duplicates.

**Tests:** every sample message has an expected amount, merchant, and result. Negative cases include OTPs, promotions, balance alerts, credits, failed or declined transactions, and refunds. Also dedupe, the suggester, and the catch-up scan.

**Done when:** a real bank SMS arriving while the app is closed creates a stored row and one notification. The parser gets most of your samples right (the target is set with the data, for example 9 in 10), and anything it cannot parse is saved with its raw text and blank fields.

### M3. Messages page

**Build:** `MessagesScreen`, `MessagesContent`, `MessagesViewModel`; `FilterChipRow`, `SwipeRow`, `StatusBadge`, `MessageRow`, `EmptyState`, `UndoSnackbar`; the message detail sheet with Accept and Reject; the red circle on the tab; the new-flag rules from [04](04-messages-and-notifications.md). Swipe right goes to Add transaction (a stub until M4). Swipe left rejects with Undo.

**Tests:** ViewModel tests with a fake repository (filter counts, reject and undo, new-flag clearing).

**Done when:** rows show white, green, and red correctly, the filters and counts are right, the gestures work, and the red circle appears and clears as described.

### M4. Add transaction and the accept flow

**Build:** `AddTransactionScreen` and its ViewModel; `AmountField`, `CategoryChipGrid`, a date picker, and a note. Pre-fill from a message, including the suggested category and blank fields when the parser could not read them. Manual add. Edit mode (used later from Category detail). Saving from a message uses the atomic `saveFromMessage`.

**Tests:** input validation (empty, zero, non-numeric), pre-fill, blank-amount messages, and the atomic accept.

**Done when (the Phase 2 gate):** you accept a real SMS, the transaction is saved, and the message turns green. Backing out leaves it white. A manual transaction saves as well.

### M5. ~~Categories~~ Removed 2026-09-25

Originally built as its own page - drag to reorder, swipe to archive, an archived section, and a New category sheet with `EmojiIconBox` and a name, accepting a single emoji only. Confirmed working on-device, then removed the same day: once M6 (Monthly budget) existed, the user judged a separate Categories page redundant with it, and asked to fold create/rename/reorder into Monthly budget's own list and drop archiving from v1 entirely. See M6 below and the progress tracker for what actually shipped.

### M6. Monthly budget (includes category management)

**Build:** the Monthly budget page with `MonthSelector` and the total bar; one sheet used to create a category with its amount (the + button) and to rename/change-icon/re-amount an existing one (tap a row) - category management lives here, not on a separate page; `EmojiIconBox` and single-emoji validation; drag to reorder, carried over from the removed M5; `CopyBudgetFromPreviousMonth`; the first-launch flow (starter categories at ₹0, then this page).

**Tests:** single-emoji validation, copy-forward (including when the previous month has no budget), order persistence, totals, and the ViewModel.

**Done when:** every category is listed with ₹0 where nothing is set, tapping a row renames/re-icons/edits its amount, dragging reorders the list, + creates a new category and never changes an existing amount, and a new month starts as a copy.

### M7. Home and Category detail

**Build:** Home with `SummaryCard`, `CategoryCard`, `BudgetProgressBar`, the review line (Not assigned count), month selector (past months viewable, future disabled), and the Add button. The Category detail page with `TransactionRow`, edit through M4's screen, and swipe-to-delete with Undo, which returns the linked message to Not assigned.

**Tests:** Home state (spent, remaining, status, "Not budgeted"), delete reverts the message.

**Done when:** the numbers match a month you calculate by hand, overspending shows as a negative amount in red, and you can edit and delete transactions.

### M8. Alerts and Settings

**Build:** `EvaluateBudgetAlerts` (80%, over budget, once per category per month, the ₹0 rule, none on edit or delete); notification channels for alerts; the Settings screen with permission status (SMS, notifications, battery optimisation) and links to fix each, plus the three alert switches stored in DataStore.

**Tests:** a table of alert cases (79% to 81%, 79% straight to 101%, repeated crossing, ₹0 budget, deletion then re-adding).

**Done when:** crossing 80% and going over each produce exactly one notification for that month, and the switches turn them off.

### M9. Backup and restore

**Build:** a JSON backup model with `formatVersion`; export with Android's file picker; import with validation, a summary, and confirmation; the last export date.

**Tests:** an export followed by an import gives identical data. A malformed file, a missing field, and a newer format version are all rejected without touching data.

**Done when:** you export, clear the app's storage in Android's App info page, import, and everything is back. **From here you can start using the app every day.** Never uninstall the app, because that deletes its data.

### M10. Trends: This month

**Build:** the three-tab `TrendsScreen`; `DonutChart` drawn on Canvas, with slices sized by budget, the used part coloured by status, emoji labels, and the overall percentage in the centre; the allocation list underneath, including categories with a ₹0 budget that have spending.

**Tests:** the series calculations.

**Done when:** it matches the mockup and the figures agree with Home.

### M11. Trends: Previous month, Historic, and range setting

**Build:** `BarChart` (grouped bars and a 100% line); the budget-used-per-month chart and the by-category comparison; the Historic chart with its two summary tiles (an in-progress month is excluded from the average and from months over budget); the "Months shown in charts" setting (3, 6, or 12; default 6).

**Tests:** each month is compared with its own budget, the in-progress month is excluded, and range handling.

**Done when:** all three tabs match the mockups and the range setting changes them.

### M12. Hardening and release

**Build:** the release build with R8 and your signing key from Phase 1; a repeatable install and update routine (`adb install -r`, with a backup export first); permission onboarding polish; duplicate-payment flagging if you want it; fixes from real use.

**Tests:** the release build is tested on the phone, not only debug.

**Done when:** you have used it for a week with no workarounds.

## 6. Libraries and setup

Versions are not listed on purpose. They come from Android Studio's template and each library's release page when M0 starts.

| Purpose | Library |
|---|---|
| Compose | Compose BOM, Material 3, UI tooling preview, activity-compose, lifecycle runtime and ViewModel for Compose |
| Navigation | Navigation 3 if stable, otherwise Navigation Compose |
| Dependency injection | Hilt, Hilt compiler (via KSP), Hilt navigation for Compose |
| Database | Room runtime and compiler (via KSP), Room testing. Room 2.x |
| Settings | DataStore Preferences |
| Backup | kotlinx.serialization JSON |
| Tests | JUnit, kotlinx coroutines test, Turbine (optional), AndroidX test, Compose UI test |

Plugins: Android application, Kotlin Android, Kotlin Compose compiler, KSP, Hilt, and Kotlin serialization.

Manifest: `RECEIVE_SMS`, `READ_SMS`, `POST_NOTIFICATIONS`, the `SmsReceiver` entry (as in the Phase 1 test app), `allowBackup="false"`, and **no** INTERNET permission.

## 7. Where each risk is handled

| Risks | Handled in |
|---|---|
| R1 to R9, R17, R18 | Phase 1 (the test app) |
| R10 to R15 | M2 |
| R19, R20 | M2 and M3 |
| R23, R25, R26 | M1 |
| R24 | M1 and M7 |
| R28 | M3 |
| R27, R30 | M6 (built as M5, folded in when that page was removed) |
| R21 | M8 |
| R22 | M9 |
| R29 | M10 |
| R16 (duplicate payments), R6, R7 | M12 |

## 8. If time gets short

Cut in this order, from the top:

1. The trends range setting (M11). It stays at 6 months.
2. Duplicate-payment flagging (M12).
3. The Previous month tab (M11), keeping Historic.
4. Drag-to-reorder on Monthly budget's category list (M6). Categories then keep creation order.
5. Onboarding polish (M12).

**Never cut:** the parser's sample tests, database migrations and their tests, backup and restore, and the atomic accept. These protect your data and your numbers.

## 9. Collecting the sample SMS

The parser (M2b) is the biggest risk to the product, and it can only be as good as the samples it is tested against.

1. Open your messages app and find spend messages from **each bank you use**. Aim for 30 to 50 messages in total, about 5 spends per bank: UPI payments, card purchases, ATM withdrawals, NEFT or IMPS debits, and autopay.
2. Also collect messages that are **not** spends, about 3 per bank: credits, OTPs, balance alerts, promotions, declined or failed transactions, and refunds. They teach the parser what to ignore.
3. Copy each one into the template `sms-samples-template.txt` in this folder. **Before saving, remove personal details:** replace card and account digits with `XXXX`, remove names and phone numbers, and remove balances if you prefer. Keep the amounts, merchants, dates, and wording exactly as they are.
4. For each one, note what you expect: spend or not, the amount, and the merchant.
5. Keep the file private. Do not put it in a public repository.

You can send the file to me when it is ready. M0 and M1 do not need it, so we can start while you collect.

## 10. What happens next

1. **You:** run Phase 1 ([10-phase-1-guide.md](10-phase-1-guide.md)) and fill in [11-phase-1-results.md](11-phase-1-results.md). Tell me anything surprising.
2. **You, in parallel:** collect the sample SMS.
3. ~~**You:** confirm the application ID~~ Confirmed: `com.budgetmanager.app`.
4. **Me, when the Phase 1 results are in:** adjust the M2 permission screen to what you saw, check whether Navigation 3 and Room 3.0 are stable, and start M0.
