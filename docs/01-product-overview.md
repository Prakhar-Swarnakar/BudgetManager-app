# Product Overview

## Vision

A small, private budgeting app that answers one question quickly: **how much is left in each category this month?** It should take very little effort to keep up to date, because bank SMS are picked up automatically and only need a quick review.

## Who it is for

One user (the owner), on their own Android phone, with accounts at several different banks.

## Principles

- **Low effort:** logging a transaction should take a few taps, and SMS should do most of the work.
- **Honest numbers:** overspending shows as a negative number rather than being hidden or capped.
- **User in control:** the app suggests a category from keywords, but nothing is filed without the user accepting it.
- **Private:** all data stays on the phone.
- **Simple first:** anything that adds complexity without being essential goes to the backlog.

## Decision log

| Topic | Decision | Status |
|---|---|---|
| Platform | Native Android app (needed to read SMS and show notifications) | Decided |
| Users | Single user | Decided |
| Data storage | On the phone only, with manual export and import as a backup | Decided |
| Budget period | Calendar month (1st to last day) | Decided |
| Month rollover | None. Each month resets and starts from its own budget | Decided |
| Overspending | Allowed. Remaining amount can go below zero (for example -2000) | Decided |
| Categories | Each has a name and an icon | Decided |
| Category icon | An emoji, entered with the phone's own keyboard | Decided |
| Adding transactions | Entered manually, or created from a bank SMS after the user reviews it | Decided |
| SMS categorisation | Keyword-based suggestion that the user can change or confirm | Decided |
| SMS review | Done on a dedicated Messages page, not through notification buttons | Decided |
| Notification | A simple "new spends detected" notification. Action buttons are in the backlog | Decided |
| Budget alerts | Notify at 80% of a category budget and when it is overspent | Decided |
| Navigation | Bottom bar for Home, Messages, and Trends. Side panel for Monthly budget and Settings | Decided - the separate Categories page (built in M5) was removed 2026-09-25 and folded into Monthly budget, see below |
| Backup and restore | Lives inside Settings. It is not a separate page or side panel item | Decided |
| Monthly budget page | Lists every category with its amount for the month. A category with no amount shows ₹0. Tapping a category renames it, changes its icon, and edits its amount, all in one sheet - category management lives here, not on a separate page | Decided, revised 2026-09-25 |
| Monthly budget: + button | Adds a new category (icon, name, and its budget for the month). It does not change the budget of an existing category | Decided |
| Categories: create, rename, reorder, archive | Create/rename/reorder all live on the Monthly budget page now. Archiving was removed from v1 entirely 2026-09-25 - every category stays visible and reorderable forever | Decided, revised 2026-09-25 (see [06-backlog.md](06-backlog.md)) |
| Trends tabs | This month, Previous month, and Historic. No 3, 6, or 12 month switch on the page | Decided |
| Trends: This month | A pie chart of the month's budget allocation, showing how much of each slice is used | Decided |
| Trends: Historic | A bar graph of the budget across the last 6 months | Decided |
| Trends range | 6 months by default, with an option to change it in Settings | Assumed |
| Trends: Previous month | Shows budget used per month over 6 months, plus this month so far against last month by category | Assumed |
| App framework | Native Android in Kotlin with Jetpack Compose, built in Android Studio. Flutter was considered and dropped | Decided |
| Test phone | Pixel 7a running Android 17 | Decided |
| Architecture | MVVM with unidirectional data flow, in UI, domain, and data layers. One Gradle module organised by feature (see 12-tech-architecture.md) | Decided - built and proven across M0 to M4 |
| Tech stack | Hilt for dependency injection, Room for the database, DataStore for settings, Navigation 3, coroutines and Flow, custom Canvas charts | Decided, except the Canvas charts which are not built until M10 |
| Money | Stored as whole paise in a `Long`, formatted in one place with Indian grouping | Decided |
| Development rules | Practices in 13-development-best-practices.md apply to all code | Assumed |
| Starter categories | Rent, Groceries, Food & Dining, Transport, Bills & Utilities, Entertainment, Shopping, Health, Other, created on first launch with ₹0 budgets | Decided - seeded in M1, confirmed present on a real first launch (row 11 in 11-phase-1-results.md) |
| Editing transactions | Done on the Category detail page, opened from a Home card. Deleting an SMS-based transaction returns the message to Not assigned | Assumed |
| Monthly budget saving | No Save button. Each amount is saved when its sheet is saved | Assumed |
| Open questions | All previously open questions were settled with defaults on 24 Sep 2026 (see 07-open-questions.md) | Assumed |
| Implementation | Built in vertical milestones M0 to M12, each running on the phone (see 14-implementation-plan.md) | Decided - M0 to M6 done (M5, Categories, was later removed and folded into M6) |
| Build approach | Four phases. Phase 1 tests SMS reading, notifications, and installing on the phone before anything else is built | Decided |
| Currency | Indian rupee (₹) | Assumed |
| Distribution | Installed directly on the owner's phone, not through the Play Store. Android may block or add steps to this, so it is tested first (see 09-risks-and-phases.md) | Assumed |
| New month budget | Starts as a copy of the previous month's budgets, which can be edited | Assumed |
