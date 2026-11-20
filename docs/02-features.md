# Features (v1)

Anything not listed here is in [06-backlog.md](06-backlog.md).

## F1. Categories and Monthly budget

Category management (create, rename, change icon, reorder) and setting each one's amount for the month live on one page - there is no separate Categories page. Removed 2026-09-25: archiving categories. Every category stays visible and reorderable forever; there is no "hide it but keep its history" state for categories (transactions themselves are never deleted just for this).

- The app starts with a set of starter categories so setup is quick: Rent, Groceries, Food & Dining, Transport, Bills & Utilities, Entertainment, Shopping, Health, and Other. Each can be renamed or re-iconed at any time.
- Each category has a **name** and an **icon**. The icon is an emoji, typed or picked using the phone's own keyboard. The app does not have its own emoji picker.
- Categories can be reordered by dragging - that order is what Home uses for its cards.
- Set an amount per category for each calendar month. The Monthly budget page lists **every** category with its amount for the month. A category with no amount shows **₹0**.
- **Tap a category** to rename it, change its icon, or change its amount, all in one sheet. Save updates the row and the month's total.
- The **+ button** adds a **new category** (icon, name, and this month's budget). It never changes the budget of an existing category.
- The budget can be edited at any time during the month.
- A new month starts as a copy of the previous month's budgets.
- Nothing rolls over between months.

## F2. Manual transactions

- Add a transaction with a title, amount, category, payment method (taxonomy), date, and an optional note. Order on the page (2026-10-04): Title, Amount, Category, Payment method, Split with friends, Date, Notes.
- **Title is saved** (built 2026-10-06): a transaction now keeps its own Title permanently (it previously vanished the moment you left the screen). The linked SMS message's own merchant is never touched by renaming it - Category detail and, once Accepted, the Messages row both show the saved title in preference to the message's merchant, falling back to the merchant (then the note, then a placeholder) for anything saved before this existed or left blank.
- Edit or delete a transaction later, from the **Category detail** page (tap a category card on Home). It lists that category's transactions for the month. Tap one to edit it, or swipe left to delete it.
- The same Add Transaction page is used when accepting an SMS, with the fields pre-filled, and when editing.
- Deleting a transaction that came from an SMS returns that message to **Not assigned**.
- Category is picked from a dropdown (built 2026-10-02, replacing a chip grid) - a single-line field that expands into a list on tap, showing the keyword-suggested category marked "Suggested".
- Optionally split with friends - see F6c below.

## F3. SMS reading

- The app reads incoming bank SMS on the phone.
- It keeps messages that look like a debit (money spent) and extracts the amount, merchant, and date.
- **(built 2026-10-03)** It also keeps messages that look like a credit (money coming in) - generic detection only, no sub-type classification yet (see F4's "Credit detection" item and `06-backlog.md`'s "Credit handling"). A credit message gets an amount where one can be read, but no merchant.
- Each one is stored as a message on the Messages page.
- Works across many banks, so parsing has to cope with different formats.

## F4. Messages page

- A page listing every SMS-derived message the app received, both debits and credits.
- The page's icon shows a small red circle when there are new messages.
- Each message is coloured by its status: white (not assigned), green (accepted), red (rejected).
- Swipe to accept or reject, or tap to open the message. Accepting a credit message is a direct status flip - no transaction is created, no category or budget is touched.
- **Direction arrow** (built 2026-10-03): every row shows a small arrow next to the amount - up and green for a credit, down and red for a debit.
- **Possible-duplicate warning** (built 2026-10-01): a Not assigned message is flagged when another Not assigned message has the same amount, a different sender, and arrived within 10 minutes of it - for example a bank's debit alert and a UPI app's own confirmation for the same payment. Shown as a small warning badge on the row and, in the detail sheet, which other message it might duplicate. This is separate from the exact-SMS-counted-twice bug fixed the same day (see `DedupeKey.kt`) - that was one physical message processed twice; this is two different messages that may describe the same real payment, and the user still decides which (if either) to accept.
- **Split by month** (built 2026-10-02): the page has its own month selector, scoped to the viewed month only, with "Fetch SMS" (backfills that month's SMS from the phone's inbox), "Run rule" (re-applies current keyword rules to that month's Not assigned messages only, never touching status or an already-decided message), and "Clear month" (removes every message in the viewed month after confirming - a linked transaction is kept, only unlinked) buttons underneath it.
- Full detail in [04-messages-and-notifications.md](04-messages-and-notifications.md).

## F5. Notification for new messages

- When new spend messages arrive, the app shows a simple notification such as "3 new spends detected".
- No action buttons in v1.

## F6. Category suggestion

- When a message is accepted, the Add Transaction page opens with a category suggested from keywords in the merchant or SMS text.
- The user can change it before saving.
- If no keyword matches, the category is left empty.
- The starter categories come with a built-in keyword list (for example Swiggy and Zomato map to Food & Dining).
- **Learning from choices** (built 2026-10-01, generalised 2026-10-02, decoupled from Title 2026-10-04): Add Transaction's **Title** field (pre-filled from the SMS when opened from a message, blank otherwise - renamed from "Merchant" 2026-10-04, same field) is a display label you're free to rename for your own readability - renaming it never changes what a rule learns (see F2's "Title is saved" below for what renaming it *does* change). The keyword actually learned into a rule lives in its own separate field next to the Category/Payment-method dropdown, revealed by the **"Add to rule"** checkbox - seeded once from the SMS's own parsed merchant (not from Title, so renaming Title never changes what the rule learns), independently editable afterward, and preserved if you uncheck and recheck the box. The checkbox itself is disabled whenever there's no real SMS behind the transaction - a manual entry has nothing to learn from, so it can never add a rule, checkbox or not. Checking it before Save upserts that keyword → the chosen category as a keyword rule, the same moment the transaction saves. A keyword under 3 characters blocks Save with an inline error rather than silently skipping - checking the box is a deliberate choice, so an empty or too-short keyword is treated as a mistake worth catching.
- **Editable keyword rules** (built 2026-10-01, grouped layout 2026-10-02, moved to the side panel 2026-10-02): a "Category rules" page in the side panel (previously a button inside Settings), grouped by category - each category is a section showing its words as removable chips plus a "+ add" chip, including a category with none yet. Tapping a chip reassigns its category; a rule's keyword can't be edited in place - it's the rule's identity, so changing it would silently leave the old rule behind; delete and re-add instead.

## F6b. Payment method taxonomy (built 2026-10-02)

A second classification alongside Category - **what** a transaction is for (Category) vs. **how** you paid for it (taxonomy) - tracked completely independently, each with its own keyword-rule engine, since the same merchant doesn't reliably map to one payment method the way it reliably maps to one category.

- A fixed set of 8 values: UPI, Credit Card, Debit Card, Credit Card via UPI (a credit card linked through a UPI app - the hardest to tell apart, since the SMS often just says "UPI"), Bank Transfer, Wallet, Cash, Other.
- Add Transaction has its own payment-method dropdown, pre-filled from a message's keyword suggestion the same way Category is, and its own "Add to rule" checkbox and keyword field - independent of Category's, so you can remember one without the other (2026-10-04: its keyword is seeded from the SMS merchant the same way Category's is, not from Title).
- A "Taxonomy rules" page in the side panel, laid out the same way as Category rules (grouped chips, a "+ add" per group), except every one of the 8 values always has a group - there's no creating or renaming a taxonomy value, only adding or removing the keywords that suggest it.
- Shown on the Messages row (built 2026-10-02): the same slot and style category uses, directly underneath it.

## F6c. Split with friends (built 2026-10-02)

Fronting a shared expense (dinner, groceries) that is really split several ways - the transaction keeps the **full amount** your bank actually shows, but only **your share** counts toward your category's budget.

- Add Transaction has a "Split with friends" switch. Expanding it lets you add one or more friends - typed by name, created inline the first time (no separate "add a friend" step) - each either an **equal share** (the remaining amount divided evenly among you and every other equal-share friend) or a **custom amount** you type directly. "Your share" updates live as you add friends or change amounts.
- Saving blocks with an error if the amounts entered leave your own share negative.
- A friend's share is excluded from the category's spend from the moment the split is saved - it never changes again when that friend later settles up. Settling is purely a receivable-tracking action, not a budget one.
- **Editing** a split transaction reloads every friend as a custom amount so the numbers you see are exactly what's saved; saving an edit **always replaces the whole split** (even down to none, if every friend is removed) - there's no partial update. If any friend had already settled, a warning says saving will reset everyone back to unsettled, since the old split doesn't carry forward.
- **Owed to you** (bottom-bar tab) - **redesigned 2026-10-13**: one row per **transaction** now, not per friend - a dinner split five ways is one row listing all five friends as tags, not five identical-looking rows. Status is **Pending** or **Reminded** (a simple "I've nudged about this on GPay/Splitwise" marker - transaction-level, not per friend, since one request commonly covers the whole group expense; never touches the owed amount or any budget figure). Swipe toggles between the two. Tap a row to open the underlying transaction for editing. A "Pending" total (the sum of every listed friend's still-unsettled share) sits above the list. **Two view modes** (same day): **By transaction** (the row shape above) and **By friend** - one collapsible section per friend (name, transaction count, their own pending total), collapsed by default; expanding one shows that friend's transactions with their own tag floated first in each row (so it reads naturally regardless of which view you're in). **Filters** (same day): status (All/Pending/Reminded) and friend, both as chip rows with counts, independent of each other - selecting one doesn't change the other's counts. **Settling is deferred** - the per-friend Settle action from the original build is paused while Owed to You's row model changed out from under it (a single row no longer maps to a single friend's settle state); the data and `settle()` capability are untouched and ready to be reconnected, planned to live on the Add Transaction edit screen instead of a row action here. See `06-backlog.md` for the fuller design conversation.
- **Shown on the Messages and Category detail rows** (built 2026-10-06): a split transaction's row shows **your share** as the main amount, with the original full amount right after it, struck through - the same two numbers either page always had access to (`Transaction.amount`/`owedTotal`), just not previously surfaced on the row itself. A non-split transaction's row is completely unchanged.

## F7. Budget tracking

- For each category: **remaining = budget minus spent**.
- Remaining can be negative, and negative values are clearly marked.

## F8. Budget alerts

- Notify when a category reaches 80% of its budget.
- Notify when a category goes over its budget.

## F9. Home overview

- Shows the current month with remaining budget per category, in the order set on the Monthly budget page.
- Tapping a category card opens its Category detail page.
- A line shows how many messages are waiting for review (the ones still Not assigned).

## F10. Trends

The Trends page has three tabs: **This month**, **Previous month**, and **Historic**. There is no 3, 6, or 12 month switch on the page.

**This month**

- A pie (donut) chart of the month's budget allocation. Each slice is one category, sized by its share of the total budget and labelled with its emoji.
- Inside each slice, the coloured part shows how much of that allocation is already used. Blue means under 80%, amber means 80% or more, and red means over budget. The remaining part is grey.
- The centre shows the overall percentage of the budget used.
- A list underneath gives each category's allocation, amount used, and status.

**Previous month** *(contents assumed)*

- A chart of budget used over the last 6 months, where each bar is that month's spending as a percentage of that month's own budget, with a 100% line.
- A list comparing this month so far with last month, by category, with an arrow and the difference in rupees.

**Historic**

- A bar graph of the last 6 months, with a budget bar and a spent bar for each month.
- Two summary tiles: average spent per month, and how many months went over budget.

**Range:** the charts show 6 months by default. The number of months can be changed in Settings (assumed options: 3, 6, or 12).

## F11. Settings

- Shows the status of the SMS, notification, and battery permissions.
- Switches for the alerts: new spends detected, 80% of a budget used, and over budget.
- Trends range: the number of months shown in the charts.
- **Backup and restore:** export all data to a file and import it back, so a lost or replaced phone does not lose history. This is a section inside Settings, not a separate page.

## F12. Statement validator (built 2026-10-09, CSV; PDF/Excel/passwords added 2026-10-12)

A second opinion on what the SMS pipeline caught - a validator, not a second way to add transactions. Nothing is ever saved automatically. Reached from a "Data" section on Settings.

- Pick a bank or card statement file: **CSV, Excel (.xls or .xlsx), or PDF**. Read entirely on the phone, never uploaded anywhere.
- A password-protected PDF asks for its password in a dialog before reading it; a wrong password keeps the dialog open for another try rather than bouncing back to the picker.
- A summary card: coverage % (statement lines matched), the statement's own debit total vs. the in-app Accepted debit total, and the difference.
- Five report buckets, each shown only when it has rows: **Needs review** (matched, still Not assigned), **Missing from our data** (no match at all - the one bucket with a real "Add" action, pre-filling Add Transaction), **Rejected but on statement**, **Amount differs**, and **In our data but not on statement** (an Accepted message nothing on the statement backs up - a possible false positive).
