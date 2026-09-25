# User Flows

Flows marked *(assumed)* contain a step I filled in that needs your confirmation. See [07-open-questions.md](07-open-questions.md).

## 1. First launch and setup

1. The user opens the app for the first time.
2. The app asks for permission to read SMS and to send notifications.
3. The app creates the starter categories (Rent, Groceries, Food & Dining, Transport, Bills & Utilities, Entertainment, Shopping, Health, Other), each with a ₹0 budget for the current month.
4. The app opens the Monthly budget page so the user can tap each category and set an amount, rename or add categories if they want, then continue.
5. The user lands on the Home screen.

## 2. Set up a month's budget

Category management (create, rename, change icon, reorder) lives on this same page - there is no separate Categories page (removed 2026-09-25, along with archiving; folded into here).

1. The user opens the side panel and taps **Monthly budget**. Opening a month with nothing set yet copies last month's amounts in as a starting point, with a note saying so.
2. The page lists every category with its amount for the month. A category with no amount shows **₹0**.
3. To change an amount, rename a category, or change its icon, the user **taps the category**. One sheet covers all three, and the user saves. The month's total updates.
4. To reorder categories, the user drags a row by its handle. The new order also becomes Home's card order.
5. To add a new category, the user taps the **+ button**. The sheet asks for an icon, a name, and this month's amount. The new category is created and added to the list. The + button never changes the budget of an existing category.
6. There is no separate Save button for the page as a whole. Each amount is saved as soon as its sheet is saved. Spending for the month starts at zero, and nothing carries over from last month.

## 3. Add a transaction manually

1. The user taps Add.
2. They enter an amount, choose a category, and confirm the date. A note is optional.
3. They save.
4. The category's remaining amount updates, and a budget alert fires if a threshold is crossed (see flow 8).

## 4. An SMS arrives

1. A bank SMS arrives on the phone.
2. The app checks whether it is a debit message. If not, it is ignored.
3. If it is, the app extracts the amount, merchant, and date, and saves it as a message with status **not assigned**.
4. The Messages page icon shows a small red circle.
5. The app shows a simple notification, for example "3 new spends detected".
6. Tapping the notification opens the app *(assumed to land on the Messages page directly; as built in M2, it opens `MainActivity` generally and lands on Home - the Messages deep link, R20 in [09-risks-and-phases.md](09-risks-and-phases.md), is not wired up yet, see [07-open-questions.md](07-open-questions.md))*.

## 5. Review messages

1. The user opens the Messages page and sees the list of received messages, newest first.
2. Each row is coloured by status: white (not assigned), green (accepted), red (rejected).
3. On a **Not assigned** row, the user can:
   - **Swipe right** to accept
   - **Swipe left** to reject
   - **Tap** the message to open it and read the full SMS
4. On an **Accepted** or **Rejected** row, the one live swipe direction reverts it to Not assigned instead (see flows 6 and 7). Built and confirmed on-device in M4; full gesture table in [04-messages-and-notifications.md](04-messages-and-notifications.md#gestures).

## 6. Accept a message

1. The user accepts a message by swiping.
2. The Add Transaction page opens with the details pre-filled: amount, date, merchant as the note, and a suggested category if a keyword matched.
3. The user edits anything they want and saves.
4. The message turns green (accepted) and the transaction is added to the category. Both happen together in one atomic database write, confirmed in M4.
5. If the user leaves without saving, the message stays white (not assigned).

## 7. Reject a message

1. The user swipes to reject.
2. The message turns red (rejected) and no transaction is created.
3. It stays in the list so it is still visible.
4. A short **Undo** bar appears for a few seconds after the swipe.
5. A rejected message can be swiped right to return it to Not assigned - from there it can be accepted like any other Not assigned message. It cannot go straight from Rejected to Accepted (revised 2026-09-25 during M4; see row 7 in [07-open-questions.md](07-open-questions.md)).

## 8. Budget alerts

1. Whenever a transaction is saved, the app recalculates that category.
2. If spending has just reached **80%** of the budget, the app sends a notification.
3. If spending has just gone **over 100%** of the budget, the app sends a notification.
4. Each alert is sent once per category per month *(assumed)*.

## 9. Daily check

1. The user opens the app and sees the Home screen.
2. They see remaining budget per category for the current month, with overspent categories clearly marked.
3. The Messages icon shows a red circle if there is anything new to review.

## 10. Review trends

1. The user taps **Trends** in the bottom bar. It opens on the **This month** tab.
2. **This month:** the user sees a pie chart of the month's budget allocation. Each slice shows how much of its allocation is used, and the list below gives the figures per category.
3. **Previous month:** the user sees budget used over the last 6 months, and this month so far compared with last month by category *(contents assumed)*.
4. **Historic:** the user sees a budget bar and a spent bar for each of the last 6 months, with the average spent per month and the number of months over budget.

## 11. Back up and restore

1. The user opens the side panel, taps **Settings**, and scrolls to **Backup & restore**.
2. **Export** saves all data to a file. The page shows the date of the last export.
3. On a new or reset phone, the user taps **Import** and picks that file.
4. Import replaces the data on the phone after a summary and a confirmation *(assumed; see open questions)*.

## 12. Edit or delete a transaction

1. On Home, the user taps a category card. The Category detail page opens with that category's transactions for the month.
2. **Tap** a transaction to edit it on the Add Transaction page. Saving updates the totals.
3. **Swipe left** to delete it. An Undo bar appears for a few seconds.
4. If the transaction came from an SMS, deleting it returns that message to **Not assigned** so it can be reviewed again.
5. Editing or deleting does not send budget alerts.

## 13. Change the trends range *(assumed)*

1. The user opens the side panel and taps **Settings**.
2. In the Trends section, they tap **Months shown in charts** and choose a number of months. The default is 6.
3. The Previous month and Historic tabs use the new range.
