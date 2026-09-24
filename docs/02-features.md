# Features (v1)

Anything not listed here is in [06-backlog.md](06-backlog.md).

## F1. Categories

- The app starts with a set of starter categories so setup is quick: Rent, Groceries, Food & Dining, Transport, Bills & Utilities, Entertainment, Shopping, Health, and Other. Each can be renamed, re-iconed, or archived.
- Create, rename, and archive categories.
- Each category has a **name** and an **icon**. The icon is an emoji, typed or picked using the phone's own keyboard. The app does not have its own emoji picker.
- Archived categories disappear from pickers but keep their history.
- A category can be created on the Categories page, or with the + button on the Monthly budget page. A category created on the Categories page shows ₹0 on the Monthly budget page until an amount is set.

## F2. Monthly budget

- Set an amount per category for each calendar month.
- The Monthly budget page lists **every** category with its amount for the month. A category with no amount shows **₹0**.
- **Tap a category** to change its amount. A small sheet opens with an amount field, and the same sheet is used for adding a new category.
- The **+ button** adds a **new category** (icon, name, and this month's budget). It never increases the budget of an existing category.
- The budget can be edited at any time during the month.
- A new month starts as a copy of the previous month's budgets (assumed; see [07-open-questions.md](07-open-questions.md)).
- Nothing rolls over between months.

## F3. Manual transactions

- Add a transaction with amount, category, date, and an optional note.
- Edit or delete a transaction later, from the **Category detail** page (tap a category card on Home). It lists that category's transactions for the month. Tap one to edit it, or swipe left to delete it.
- The same Add Transaction page is used when accepting an SMS, with the fields pre-filled, and when editing.
- Deleting a transaction that came from an SMS returns that message to **Not assigned**.

## F4. SMS reading

- The app reads incoming bank SMS on the phone.
- It keeps only messages that look like a debit (money spent) and extracts the amount, merchant, and date.
- Each one is stored as a message on the Messages page.
- Works across many banks, so parsing has to cope with different formats.

## F5. Messages page

- A page listing every SMS-derived message the app received.
- The page's icon shows a small red circle when there are new messages.
- Each message is coloured by its status: white (not assigned), green (accepted), red (rejected).
- Swipe to accept or reject, or tap to open the message.
- Full detail in [04-messages-and-notifications.md](04-messages-and-notifications.md).

## F6. Notification for new messages

- When new spend messages arrive, the app shows a simple notification such as "3 new spends detected".
- No action buttons in v1.

## F7. Category suggestion

- When a message is accepted, the Add Transaction page opens with a category suggested from keywords in the merchant or SMS text.
- The user can change it before saving.
- If no keyword matches, the category is left empty.
- The starter categories come with a built-in keyword list (for example Swiggy and Zomato map to Food & Dining). Archived categories are never suggested. Editing the keyword list is in the backlog.

## F8. Budget tracking

- For each category: **remaining = budget minus spent**.
- Remaining can be negative, and negative values are clearly marked.

## F9. Budget alerts

- Notify when a category reaches 80% of its budget.
- Notify when a category goes over its budget.

## F10. Home overview

- Shows the current month with remaining budget per category, in the order set on the Categories page.
- Tapping a category card opens its Category detail page.
- A line shows how many messages are waiting for review (the ones still Not assigned).

## F11. Trends

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

## F12. Settings

- Shows the status of the SMS, notification, and battery permissions.
- Switches for the alerts: new spends detected, 80% of a budget used, and over budget.
- Trends range: the number of months shown in the charts.
- **Backup and restore:** export all data to a file and import it back, so a lost or replaced phone does not lose history. This is a section inside Settings, not a separate page.
