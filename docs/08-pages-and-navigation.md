# Pages and Navigation

Rendered mockups are in the [ui-mockups](ui-mockups) folder. `00-overview.html` (open it in a browser) shows every current screen on one page - rebuilt 2026-10-07 directly from the Compose source and checked against real on-device screenshots, replacing the earlier per-page PNGs, which had gone stale after the redesign rounds since. The per-page "Mockup file" column below now points into that one file's matching section rather than a separate image each.

## Navigation structure *(decided)*

Navigation is split by how often each area is used.

**Bottom bar** (daily use):

- Home
- Messages (with a small red circle when there are new messages)
- Trends
- Owed to you (added post-v1, 2026-10-02 - a bottom-bar tab rather than a side-panel page, your explicit call over the recommendation to put it in the side panel)

**Side panel** (opened from the menu icon, used a few times a month):

- Wallet (added 2026-11-17 - cash on hand; see its section below)
- Monthly budget (category management - create, rename, reorder - lives here too; there is no separate Categories page)
- Category rules (added post-v1, 2026-10-02 - moved here from a button inside Settings)
- Taxonomy rules (added post-v1, 2026-10-02 - same layout as Category rules, for the separate payment-method keyword rules)
- Settings (which includes Backup and restore)

A round **Add** button on Home starts a manual transaction.

## Pages

All mockups below live in the single [00-overview.html](ui-mockups/00-overview.html), grouped into three rows: "Daily use" (bottom bar), "Side panel", and "Sheets" - built 2026-10-07, so it predates **Validate a statement** (built 2026-10-09) below; that page has no mockup yet.

| Page | Purpose |
|---|---|
| Home | Remaining budget per category for the current month |
| Category detail | One category's transactions for a month. Tap to edit, swipe left to delete. Opened by tapping a card on Home |
| Messages | List of SMS-derived messages with status colours and swipe actions |
| Message detail | The full SMS, the suggested category, and Accept and Reject buttons |
| Add transaction | Manual entry, or a pre-filled form when accepting a message |
| Trends: This month | Pie chart of the month's budget allocation and how much is used |
| Trends: Previous month | Budget used over 6 months, and this month so far against last month by category |
| Trends: Historic | Budget and spent bars for the last 6 months, with two summary tiles |
| Owed to you | One row per transaction, listing every friend on it, with a Pending/Reminded swipe status and a running pending total - added post-v1, 2026-10-02, row redesigned 2026-10-13 |
| Wallet | Cash on hand, with a ledger of cash top-ups and spends paid with Cash - added 2026-11-17 |
| Side panel | Menu with Monthly budget, Wallet, Category rules, Taxonomy rules, and Settings |
| Monthly budget | Every category with its amount for a month (₹0 if none), drag to reorder, a + button to add a new category. Category management (rename, change icon) lives here too - there is no separate Categories page |
| Budget: new category (+) | Sheet for creating a new category with its icon, name, and this month's amount |
| Budget: edit category | The same sheet, opened by tapping a category, to rename it, change its icon, or change its amount |
| Category rules | Keyword-to-category rules, grouped by category - added post-v1, 2026-10-02, moved here from Settings |
| Taxonomy rules | Keyword-to-payment-method rules, same grouped layout as Category rules but for the fixed 8-value taxonomy - added post-v1, 2026-10-02 |
| Settings | Permissions, alerts, trends range, Backup and restore, and Data (Validate a statement) |
| Validate a statement | Pick a CSV statement, compare it against what SMS detection caught, review the gaps - a validator, not a second way to add transactions. Opened from Settings' new Data section, built 2026-10-09 |

## Page details

### Home

- Month selector at the top.
- A summary card with spent, budget, amount left, and a progress bar. **Income and Saved** (built 2026-10-07): below the progress bar, Income is the sum of every Accepted credit message this month, and Saved is Income minus Spent - can go negative, shown with the usual "-" sign rather than a different label. Every accepted credit counts as income for now - there's no typed Refund/Settlement/Income distinction yet (so a friend settling up or a refund would also count); see `06-backlog.md`, "Credit handling" for the fuller system this could grow into. No budget concept applies to Income, purely informational.
- **Owed to you** (built 2026-11-17): a third line under Income/Saved - friends' still-unsettled shares on transactions dated this month. Informational; never changes Saved or any budget figure.
- A line showing how many messages are waiting for review (still Not assigned).
- A card per category, in the order set on the Monthly budget page, with its emoji, name, the amount **spent** (right, swapped with "left" on 2026-11-17) and "N left" under the name, and a progress bar. The bar is blue below 80%, amber from 80%, and red when over budget. An overspent category shows a negative amount such as -₹2,000. A category with a ₹0 budget shows "Not budgeted".
- Tapping a card opens the Category detail page.

### Category detail

- A back arrow, the category name, and the month selector.
- A summary card with the emoji, spent against budget, the amount left (negative if over), and a progress bar.
- A list of that month's transactions, newest first. Each row shows a title, date and time, amount, and a small "SMS" or "Manual" tag. **Title** (built 2026-10-06, schema v5→v6): shows `Transaction.title` - what Add Transaction's Title field actually saves - falling back to the linked SMS message's own merchant (never touched by renaming the title), then the note, then "No note," for anything saved before this column existed or left blank either way. **Time** (fixed 2026-10-06): `Transaction.occurredAt` is always midnight (Add Transaction's date field has no time component) - the row shows the linked message's own untouched `receivedAt` instead whenever one exists, so the real time still shows regardless of title. **Split with friends** (built 2026-10-06): a split transaction's amount shows **your share**, with the full amount struck through right after it - unaffected for every non-split transaction.
- Tap a row to edit it on the Add transaction page, which now also shows the original SMS banner for a message-linked transaction, the same as the accept-a-message flow (**fixed 2026-10-06** - it previously only appeared when accepting, not when editing afterward). Swipe left to delete - a confirmation dialog asks first, no Undo here (revised 2026-09-25). Deleting a transaction that came from an SMS returns the message to Not assigned.

### Messages

- Status filter chips: All, Not assigned, Accepted, Rejected, each with a count.
- **Richer filters** (built 2026-10-07, revised same day): a filter icon pinned at the start of the status chip row itself (no second row) - badged when Direction, Category, or Taxonomy is narrowed past its default - opens a sheet with all three: Direction (All/Outgoing/Incoming), Category, and Taxonomy (the latter two each: Any, None - nothing assigned or suggested at all, or a specific value). All four dimensions stack (AND) - e.g. "Not assigned + Incoming" at once - rather than replacing each other. Each dimension's counts are independent of whatever else is currently selected.
- One row per message with the merchant, date and time, and amount. New messages are in bold with a small blue dot. **Title** (built 2026-10-06): once Accepted, the row shows the linked transaction's saved title in place of the message's own merchant whenever one was saved - the message's merchant is never overwritten by this, so a Not assigned or Rejected row (which never has a transaction) always shows exactly what the SMS said. **Split with friends** (built 2026-10-06): once Accepted into a split transaction, the amount shown is **your share**, with the original full amount right after it, struck through - unaffected for every non-split message.
- Rows are white, green, or red by status. Accepted rows also show the category. Each status has an icon and a label, so colour is never the only signal.
- On a Not assigned row, swiping right reveals a green Accept strip and swiping left reveals a red Reject strip. On an Accepted or Rejected row, the one live direction (back to Not assigned) reveals a neutral grey "undo" strip instead, and the other direction shows nothing - the strip always matches what the swipe will really do. Confirmed and built in M3/M4; full rules in [04-messages-and-notifications.md](04-messages-and-notifications.md#gestures).

### Add transaction

Order (2026-10-04): SMS banner, Title, Amount, Category, Payment method, Split with friends, Date, Notes.

- A banner with the original SMS text when opened from a message, **and also when editing a transaction that came from one** (fixed 2026-10-06 - previously only shown on the initial accept, not when reopening to edit afterward).
- **Title** (renamed from "Merchant," 2026-10-04): pre-filled from the SMS when accepting a message, blank otherwise - free to rename for readability, since it no longer feeds the category/taxonomy keyword rules (see below).
- A large amount field.
- Category and Payment method each use the shared rule-learning section (`RuleLearningSection`, `core/designsystem/components/`): a dropdown (Category replaced its chip grid with one 2026-10-02; see [02-features.md](02-features.md)) followed by an "Add to rule" checkbox - disabled when there's no real SMS behind the transaction - and, while checked, a **Keyword** field seeded from the SMS's own merchant (not from Title).
- A "Split with friends" switch (added post-v1, 2026-10-02): expands into a list of friends, each equal-share or a custom amount, with a live "Your share" total. See [02-features.md](02-features.md), F6c.
- Date and note fields, and Cancel and Save buttons, built in M4.

### Owed to you

- A "Pending" total card at the top (sum of every unsettled friend's share across every listed transaction).
- Two tabs (added 2026-10-13): **By transaction** - one row per transaction, title, date and time, every friend on that transaction as tags (truncated with a "+N" overflow), a **Pending/Reminded** status badge, the total amount owed across all of them, and category/taxonomy. **By friend** - one collapsible section per friend (name, transaction count, their own pending total), collapsed by default; expanding shows that friend's rows with their own tag floated first.
- Status and friend filter chip rows (added 2026-10-13), both independent (selecting one doesn't narrow the other's counts).
- Swipe toggles Pending ↔ Reminded (a plain "I've nudged about this" marker - never changes the amount). Tap a row to open the underlying transaction for editing.
- Settling is paused for now - the per-friend Settle button from the original build doesn't map cleanly onto a row that can represent several friends at once, so it's not in this screen currently; the data and capability are untouched, planned to move to Add Transaction's edit screen instead.

### Trends

- Three tabs: This month, Previous month, Historic.
- This month: a donut chart with an emoji on each slice, a centre percentage, a legend, and a category list.
- Previous month: a bar chart of budget used per month with a 100% line, and a comparison list by category.
- Historic: grouped bars of budget and spent, average spent per month, and months over budget.
- Details are in [02-features.md](02-features.md) and [05-budget-rules.md](05-budget-rules.md).

### Monthly budget

Category management (create, rename, change icon, reorder) lives here - there is no separate Categories page (removed 2026-09-25, along with archiving categories).

- Month selector, and a note when the amounts were copied from the previous month.
- A row for every category, each with a drag handle, emoji, name, and its amount. A category with no amount shows ₹0 in a muted style. Dragging a row by its handle reorders the list - that order is also what Home uses for its cards.
- Tapping a row (not the handle) opens a sheet with an icon box, a name field, and an amount field, so renaming, changing the icon, and changing the amount are all one action. Save updates the row and the month's total.
- A + button in the top bar opens the same sheet in "new category" mode. The icon comes from the phone's emoji keyboard; the app has no emoji picker of its own. It never changes an existing category's amount.
- A total for the month at the bottom. There is no Save button: each amount is saved when its sheet is saved.

### Wallet

Added 2026-11-17, following `ui-mockups/01-proposed-cash-tracking.html`.

- A "Cash on hand" card: every top-up minus every cash spend (a transaction whose payment method is Cash, at its full amount). Never stored, always derived. Red with a short explanation when negative - it means a top-up wasn't logged or something was tagged Cash by mistake.
- One ledger, newest first: top-ups (green, with a delete icon and confirmation) and cash spends (category emoji and name; tap opens that category's detail page).
- **+** opens an "Add cash" sheet: amount, date, optional note. Cash spends are added through Add Transaction as before, with Payment method set to Cash.

### Settings

- **Permissions:** SMS, notifications, and battery usage, each with its current status.
- **Alerts:** switches for new spends detected, 80% of a budget used, and over budget.
- **Trends:** months shown in charts (6 by default).
- **Backup and restore:** Export, Import, and the date of the last export.
- **Data** (built 2026-10-09): a single "Validate a statement" button, opening the page below.

### Validate a statement

- A short explainer: a second opinion on what SMS detection caught, not a second way to add transactions; nothing here saves on its own. Accepts CSV, Excel (.xls/.xlsx), or PDF (including password-protected PDFs, via a password dialog) - see 06-backlog.md.
- "Choose file" opens the same system file picker Backup and restore's Export/Import already use - the file is read entirely on the phone, never uploaded anywhere.
- Once read, a report replaces the picker: a summary card (coverage %, the statement's debit total vs. the in-app debit total for the same period, the difference), then up to five grouped sections - **Needs review** (caught, just not reviewed yet), **Missing from our data** (no matching message at all - each row has an **Add** button, opening Add Transaction pre-filled with the statement line's amount, description, and date), **Rejected, but on statement** (worth a second look either way), **Amount differs** (matched by date and direction, but off by more than a few rupees), and **In our data, not on statement** (an Accepted message nothing on the statement backs up - a possible false positive). A bucket with nothing in it isn't shown.
- "Validate another file" at the bottom starts over. Closing the page (back arrow) discards the comparison entirely - nothing is kept between sessions.

## Shared components

- Month selector with previous and next arrows.
- Category card with progress bar and status text.
- Status colours and icons (see [05-budget-rules.md](05-budget-rules.md)).
- Filter chips and a three-way tab control.
- Bottom sheets for message detail and the budget category sheet (one component used to create a new category or edit an existing one's icon, name, and amount).
