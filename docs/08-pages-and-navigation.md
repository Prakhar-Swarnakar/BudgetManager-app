# Pages and Navigation

Rendered mockups are in the [ui-mockups](ui-mockups) folder. `00-overview.png` shows all screens on one sheet.

## Navigation structure *(decided)*

Navigation is split by how often each area is used.

**Bottom bar** (daily use):

- Home
- Messages (with a small red circle when there are new messages)
- Trends

**Side panel** (opened from the menu icon, used a few times a month):

- Monthly budget
- Categories
- Settings (which includes Backup and restore)

A round **Add** button on Home starts a manual transaction.

## Pages

| Page | Purpose | Mockup file |
|---|---|---|
| Home | Remaining budget per category for the current month | `01-home.png` |
| Category detail | One category's transactions for a month. Tap to edit, swipe left to delete. Opened by tapping a card on Home | `01b-category-detail.png` |
| Messages | List of SMS-derived messages with status colours and swipe actions | `02-messages.png` |
| Message detail | The full SMS, the suggested category, and Accept and Reject buttons | `03-message-detail.png` |
| Add transaction | Manual entry, or a pre-filled form when accepting a message | `04-add-transaction.png` |
| Trends: This month | Pie chart of the month's budget allocation and how much is used | `05-trends.png` |
| Trends: Previous month | Budget used over 6 months, and this month so far against last month by category | `05b-trends-previous-month.png` |
| Trends: Historic | Budget and spent bars for the last 6 months, with two summary tiles | `05c-trends-historic.png` |
| Side panel | Menu with Monthly budget, Categories, and Settings | `06-side-panel.png` |
| Monthly budget | Every category with its amount for a month (₹0 if none), with a + button to add a new category | `07-monthly-budget.png` |
| Budget: new category (+) | Sheet for creating a new category with its icon, name, and this month's amount | `07b-add-budget-line.png` |
| Budget: edit amount | The same sheet, opened by tapping a category, to change its amount | `07c-edit-budget-amount.png` |
| Categories | Reorder and archive categories, with a + button to add one | `08-categories.png` |
| New category | Sheet with an icon box and a name field, using the phone's emoji keyboard | `09-add-category.png` |
| Settings | Permissions, alerts, trends range, and Backup and restore | `10-settings.png` |

## Page details

### Home

- Month selector at the top.
- A summary card with spent, budget, amount left, and a progress bar.
- A line showing how many messages are waiting for review (still Not assigned).
- A card per category, in the order set on the Categories page, with its emoji, name, amount used against budget, amount left, and a progress bar. The bar is blue below 80%, amber from 80%, and red when over budget. An overspent category shows a negative amount such as -₹2,000. A category with a ₹0 budget shows "Not budgeted".
- Tapping a card opens the Category detail page.

### Category detail

- A back arrow, the category name, and the month selector.
- A summary card with the emoji, spent against budget, the amount left (negative if over), and a progress bar.
- A list of that month's transactions, newest first. Each row shows the merchant or note, date and time, amount, and a small "SMS" or "Manual" tag.
- Tap a row to edit it on the Add transaction page. Swipe left to delete, with an Undo bar. Deleting a transaction that came from an SMS returns the message to Not assigned.

### Messages

- Filter chips: All, Not assigned, Accepted, Rejected, each with a count.
- One row per message with the merchant, date and time, and amount. New messages are in bold with a small blue dot.
- Rows are white, green, or red by status. Accepted rows also show the category. Each status has an icon and a label, so colour is never the only signal.
- On a Not assigned row, swiping right reveals a green Accept strip and swiping left reveals a red Reject strip. On an Accepted or Rejected row, the one live direction (back to Not assigned) reveals a neutral grey "undo" strip instead, and the other direction shows nothing - the strip always matches what the swipe will really do. Confirmed and built in M3/M4; full rules in [04-messages-and-notifications.md](04-messages-and-notifications.md#gestures).

### Add transaction

- A banner with the original SMS text when opened from a message.
- A large amount field.
- A grid of category chips, with the suggested one highlighted and labelled "Suggested".
- Date and note fields, and Cancel and Save buttons, built in M4.

### Trends

- Three tabs: This month, Previous month, Historic.
- This month: a donut chart with an emoji on each slice, a centre percentage, a legend, and a category list.
- Previous month: a bar chart of budget used per month with a 100% line, and a comparison list by category.
- Historic: grouped bars of budget and spent, average spent per month, and months over budget.
- Details are in [02-features.md](02-features.md) and [05-budget-rules.md](05-budget-rules.md).

### Monthly budget

- Month selector, and a note when the amounts were copied from the previous month.
- A row for every category with its amount. A category with no amount shows ₹0 in a muted style.
- Tapping a row opens a small sheet with the category and an amount field. Save updates the amount and the month's total.
- A + button in the top bar opens the same sheet in "new category" mode, with an icon box, a name field, and the amount for the month. The icon comes from the phone's emoji keyboard. It never adds to an existing category's amount.
- A total for the month at the bottom. There is no Save button: each amount is saved when its sheet is saved.

### Categories

- A list with a drag handle, emoji, and name for each category. Swiping left reveals Archive.
- A collapsed Archived section at the bottom.
- A + button in the top bar opens the New category sheet. A category created here has no budget, so it shows ₹0 on the Monthly budget page until an amount is set.

### New category

- An icon box and a name field.
- Tapping the icon box brings up the phone's own keyboard, where the user picks an emoji. The app has no emoji picker of its own.

### Settings

- **Permissions:** SMS, notifications, and battery usage, each with its current status.
- **Alerts:** switches for new spends detected, 80% of a budget used, and over budget.
- **Trends:** months shown in charts (6 by default).
- **Backup and restore:** Export, Import, and the date of the last export.

## Shared components

- Month selector with previous and next arrows.
- Category card with progress bar and status text.
- Status colours and icons (see [05-budget-rules.md](05-budget-rules.md)).
- Filter chips and a three-way tab control.
- Bottom sheets for message detail, New category, and the budget amount sheet (one component used to edit an amount or add a new category).
