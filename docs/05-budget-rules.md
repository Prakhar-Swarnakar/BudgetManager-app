# Budget Rules

## Budget period

- A budget period is a **calendar month**, from the 1st to the last day.
- A transaction belongs to the month of its date, not the month it was entered.

## Setting budgets

- Each category has its own budget amount for each month.
- Budgets can be edited at any time during the month.
- A new month starts with last month's amounts copied in as a starting point *(assumed)*.
- The Monthly budget page lists **every** category. A category with no amount allotted for the month shows **₹0**.
- Tapping a category edits its amount for that month, using the same sheet that is used to add a category.
- The **+ button** on the Monthly budget page creates a **new category** with its icon, name, and amount for that month. It never increases the budget of an existing category.
- A category created on the Categories page has no budget yet, so it shows ₹0 in every month until an amount is set.
- The month's total budget is the sum of its category budgets. There is no separate income or overall cap.

## Calculations

- **Spent** = the sum of that category's transactions in the month.
- **Remaining** = budget minus spent.
- **Percent used** = spent divided by budget.

Remaining can be negative. For example, a Food budget of ₹5,000 with ₹7,000 spent shows **-₹2,000**. Nothing caps or hides the negative value.

## A category with a ₹0 budget that has spending

- Its remaining amount is shown as a negative number (for example -₹1,200), with the label **Not budgeted** instead of a percentage.
- It counts as over budget: red status, and one "over budget" alert for that category and month.
- The 80% alert does not apply, because there is no percentage.
- In the This month chart it has no slice, because the slice size comes from the budget. It still appears in the list underneath with its spending.
- The month's totals include its spending.

## No rollover

- Unspent money does not move to the next month.
- Overspending does not reduce the next month's budget.
- Each month is judged only against its own budget.

## Messages that are not accepted

- A message that is not assigned or is rejected does **not** count towards any category.
- Only a saved transaction affects the numbers.

## Alerts

| Alert | When it fires |
|---|---|
| 80% reached | Spending in a category reaches 80% of its budget |
| Overspent | Spending in a category goes over 100% of its budget |

Assumptions to confirm:

- Alerts are checked whenever a transaction is saved, whether it was entered manually or accepted from a message.
- Each alert is sent **once per category per month**.
- If one transaction jumps from below 80% straight to over 100%, only the overspent alert is sent.
- Editing or deleting a transaction does not send new alerts.
- A per-month alert record is kept for each category, so an alert that has been sent is not sent again even if spending goes down and up again.

## Status colours

The same thresholds are used on Home and in the This month chart:

| Percent used | Colour |
|---|---|
| Below 80% | Blue |
| 80% up to and including 100% | Amber |
| Over 100% | Red |

Colour is always paired with an icon or a word such as "Over budget", so it never carries the meaning alone.

## Trends calculations

- Only saved transactions are counted. Unassigned and rejected messages never appear in trends.
- Each month is compared with **its own** budget, not the current month's budget.
- **Budget used** for a month is spent divided by that month's budget. Over 100% means over budget.
- A month that is still in progress is shown in a lighter colour and is left out of "average spent per month" and "months over budget".
- The **This month** chart sizes each slice by the category's budget as a share of the month's total budget. The coloured part of a slice is that category's percent used.
- The range is 6 months by default and can be changed in Settings *(assumed)*.
- "This month so far" is compared with the full previous month. A like-for-like comparison over the same days is in the backlog.
