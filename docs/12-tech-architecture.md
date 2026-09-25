# Technical Architecture

This document describes how the real app (Phases 2 to 4) will be built: the technology, the architecture, the folder structure, and how components are reused. The Phase 1 test app is throwaway and does not follow this structure. Coding rules are in [13-development-best-practices.md](13-development-best-practices.md).

Every choice below started out marked **Assumed**, as my recommendations as architect. Phase 2 (M0 to M4) is now built on them, so the status column reflects what has actually been proven on-device, not just proposed.

## 1. Decisions at a glance

| Area | Choice | Why | Status |
|---|---|---|---|
| Language | Kotlin | Google's primary language for Android, with the best tooling and the most current examples | Decided |
| UI | Jetpack Compose with Material 3 | Google's recommended UI toolkit. Little boilerplate, and reusable components are ordinary functions | Decided |
| Architecture | MVVM with unidirectional data flow, in three layers: UI, domain, data | Google's official recommendation. Keeps screens simple and logic testable | Decided - built and proven across M0 to M4 |
| Structure | One Gradle module, organised by feature | Small single-user app. Multiple modules would add build complexity for no benefit | Decided |
| Navigation | Navigation 3 (the back stack is a list you control) | Went stable 2026-09-23, days before M0. In use since M0 for the bottom bar, side panel, and Messages-to-Add-Transaction navigation, with no need for the Navigation Compose fallback | Decided (row 26 in [07-open-questions.md](07-open-questions.md)) |
| Dependency injection | Hilt | Google recommends it for apps with several screens and ViewModels. It removes hand-written wiring | Decided - with one confirmed limitation: a `BroadcastReceiver` cannot use `@AndroidEntryPoint` (see section 6 below) |
| Database | Room (version 2.x, with KSP) | The standard on-device database layer with compile-time checked queries and schema versioning | Decided |
| Settings storage | DataStore (Preferences) | Replaces SharedPreferences. Safe for asynchronous use | Decided - holds the SMS catch-up marker since M2c |
| Async work | Kotlin coroutines and Flow | The standard way to pass data between layers | Decided |
| Charts | Custom drawing with Compose Canvas, no chart library | The donut with a "used" part inside each slice is not a stock chart (R29). Bars are simple to draw | Assumed - not built until M10 |
| Dates | `java.time` | Available on API 26 and above, which is our minimum | Decided |
| Money | Whole paise stored as `Long` | Avoids rounding errors (R25) | Decided |
| Backup file | JSON via kotlinx.serialization, written with Android's file picker | Readable, versioned, and needs no storage permission | Assumed - not built until M9 |
| Build | Gradle Kotlin DSL with a version catalog (`libs.versions.toml`) | One place for all library versions | Decided |
| Minimum Android version | API 26 (Android 8.0) | Covers nearly every current phone. Target is the latest API | Decided |

**About Room 3.0:** Google announced Room 3.0 in March 2026 as an alpha with a different package name and Kotlin-only code generation. Room 2.x moves to maintenance mode. Decided (row in [07-open-questions.md](07-open-questions.md)): staying on Room 2.x (2.8.5) through all of v1, no revisit planned. DAO functions are already written in the style Room 3.0 requires (`suspend` functions or `Flow`), so a later move would stay small if it's ever wanted.

## 2. Architecture

### Layers

```
┌──────────────────────────────────────────────┐
│ UI layer        Screens, components, ViewModels │
│                 shows state, sends user actions │
├──────────────────────────────────────────────┤
│ Domain layer    Pure Kotlin rules and use cases │
│                 (budget maths, SMS parsing, …)  │
├──────────────────────────────────────────────┤
│ Data layer      Repositories, Room, DataStore   │
│                 the only place that touches     │
│                 storage                         │
└──────────────────────────────────────────────┘
        Dependencies point downward only.
```

- **UI layer:** Compose screens plus one ViewModel per screen. A ViewModel exposes one `UiState` object (a `StateFlow`) and receives user actions as method calls.
- **Domain layer:** plain Kotlin with no Android imports. It holds the rules that must be right: how much is left in a category, which status colour applies, how an SMS is parsed, which category is suggested, and whether an alert should fire. Because it needs no Android, its tests run on the laptop in seconds. Use cases exist only where a rule spans several repositories or is reused by several screens. Simple reads go straight from the ViewModel to a repository.
- **Data layer:** repositories are the only way the rest of the app reads or writes data. Repository interfaces let tests use fakes. Room entities and DAOs are hidden behind them.

### Two kinds of models

- **Entities** are Room's table rows (`CategoryEntity`, and so on). They stay inside the data layer.
- **Domain models** (`Category`, `Transaction`) are what everything above the data layer sees. Repositories convert between the two.

The UI uses domain models directly, with small UI-only wrappers where a screen needs derived values (for example a category card with its percentage and status). A third model per layer would be over-engineering for this app.

### Data flow

State flows down and actions flow up, so there is one source of truth.

```
Room ──Flow──▶ Repository ──Flow──▶ ViewModel ──StateFlow──▶ Screen
                                        ▲                       │
                                        └────── user action ────┘
```

### Walk-through: accepting a spend from an SMS

1. **SMS arrives.** `SmsReceiver` (Android calls it) asks `SmsParser` for the amount and merchant, then saves a `SmsMessage` row through `MessageRepository`. It shows or updates the "N new spends detected" notification. The receiver does only this.
2. **Messages page opens.** `MessagesViewModel` collects `MessageRepository.observeMessages()` and turns it into `MessagesUiState`. `MessagesScreen` draws the rows.
3. **User swipes right.** The screen calls `viewModel.onSwipeStart(id)`. On a Not assigned message this opens the Add transaction screen for it; on an Accepted or Rejected message the swipe instead reverts it to Not assigned (built in M4 - see [04-messages-and-notifications.md](04-messages-and-notifications.md#gestures) for the full per-status rule).
4. **Add transaction opens.** `AddTransactionViewModel` loads the message and asks `CategorySuggester` for a category. The form is pre-filled.
5. **User taps Save.** `TransactionRepository.saveFromMessage(...)` inserts the transaction and marks the message **Accepted** in one Room database transaction, so either both happen or neither does. This is what makes "a message turns green only after the transaction is saved" true.
6. **Home updates by itself.** The Home ViewModel is already collecting a `Flow` from Room, so the new totals appear without any refresh code.
7. **Alerts.** After a save, `EvaluateBudgetAlerts` compares the category's spend with its budget, checks the `AlertLog` table, and shows an 80% or over-budget notification once per category per month.

## 3. Folder structure

The code lives in its own project folder, **outside OneDrive** (`C:\Users\prakh\AndroidStudioProjects\BudgetManager`), with its own Git repository. This documentation folder (`docs/`, this file's own folder) was merged into that same repository on 2026-09-25, via `git subtree`, so its original commit history came along with it - docs and code now share one repository and one history.

### Project root

```
BudgetManagerApp/
├── .gitignore
├── README.md
├── settings.gradle.kts
├── build.gradle.kts
├── gradle/
│   └── libs.versions.toml         all library versions in one place
└── app/
    ├── build.gradle.kts
    ├── proguard-rules.pro
    ├── schemas/                   Room's exported database schemas (committed)
    └── src/
        ├── main/                  the app
        ├── test/                  unit tests (run on the laptop)
        │   └── resources/sms/     sample SMS files, personal details removed
        └── androidTest/           tests that run on a phone or emulator
```

The signing key (`.jks`) and its password are **never** inside the project folder. See the practices doc.

### Inside `src/main`

```
src/main/
├── AndroidManifest.xml
├── res/                           strings, icons, themes
└── java/com/<your>/budgetmanager/
    ├── BudgetApp.kt               Application class (starts Hilt)
    ├── MainActivity.kt            the single Activity
    │
    ├── navigation/
    │   ├── AppNavigation.kt       bottom bar, side panel, back stack
    │   └── Destinations.kt        the list of screens and their arguments
    │
    ├── core/                      shared code with no feature knowledge
    │   ├── designsystem/
    │   │   ├── theme/             Color.kt, Type.kt, Theme.kt, StatusColors.kt
    │   │   └── components/        reusable UI pieces (section 4)
    │   ├── model/                 domain models and pure rules
    │   │   ├── Money.kt
    │   │   ├── Category.kt, Transaction.kt, SmsMessage.kt, …
    │   │   ├── BudgetStatus.kt
    │   │   └── MonthKey.kt
    │   ├── common/                formatters, clock, dispatcher providers
    │   └── notifications/         channels and notification builders
    │
    ├── data/
    │   ├── database/              AppDatabase, entities, DAOs, converters, migrations
    │   ├── datastore/             SettingsDataStore
    │   ├── backup/                JSON export and import
    │   ├── repository/            interfaces and their Room-backed versions
    │   └── di/                    Hilt modules that provide the above
    │
    ├── domain/                    use cases (only where they add value)
    │   ├── EvaluateBudgetAlerts.kt
    │   ├── CopyBudgetFromPreviousMonth.kt
    │   └── BuildTrendSeries.kt
    │
    ├── sms/
    │   ├── SmsReceiver.kt         tiny entry point called by Android
    │   ├── SmsParser.kt           amount, merchant, debit or credit
    │   ├── BankRules.kt           per-bank patterns, one small block per bank
    │   ├── CategorySuggester.kt   keyword rules
    │   └── InboxScanner.kt        catches up on messages missed while closed
    │
    └── feature/                   one folder per screen area
        ├── home/                  HomeScreen, HomeViewModel, HomeUiState
        ├── categorydetail/        one category's transactions for a month (edit, delete)
        ├── messages/              list, filters, message detail sheet
        ├── transaction/           add and edit transaction
        ├── trends/                three tabs, chart components
        ├── budget/                monthly budget list, drag to reorder, category sheet (new
        │                          category and rename/change-icon/amount are one sheet) -
        │                          category management lives here, no separate categories/ folder
        └── settings/              permissions, alerts, backup and restore
```

### Rules for the structure

- **`feature/*` may use `core`, `domain`, and `data` repositories. It may not use another feature.** Features share things by moving them into `core`.
- **`core` and `domain` never import from `feature` or `data`.** Domain models and rules must stay pure.
- **Only `data` knows Room.** No screen or ViewModel imports a DAO or an entity.
- **`sms` sits beside `feature` because it is not a screen.** It talks to repositories like any other caller.
- **One file per main class.** Small related types (such as a screen's `UiState` and its actions) may share the screen's file.

### Inside one feature

Every feature folder looks the same, which makes new screens quick to add:

```
feature/messages/
├── MessagesScreen.kt       stateful: gets the ViewModel, collects state
├── MessagesContent.kt      stateless: draws a MessagesUiState (used by previews and tests)
├── MessagesViewModel.kt
├── MessagesUiState.kt      the state, plus the user actions
└── components/             pieces used only by this feature (MessageRow, …)
```

### Naming

| Kind | Pattern | Example |
|---|---|---|
| Screen | `<Name>Screen` | `HomeScreen` |
| ViewModel | `<Name>ViewModel` | `HomeViewModel` |
| UI state | `<Name>UiState` | `HomeUiState` |
| Repository interface | `<Thing>Repository` | `TransactionRepository` |
| Repository implementation | `Room<Thing>Repository` | `RoomTransactionRepository` |
| Fake for tests | `Fake<Thing>Repository` | `FakeTransactionRepository` |
| Room table class | `<Thing>Entity` | `CategoryEntity` |
| DAO | `<Thing>Dao` | `CategoryDao` |
| Use case | A verb phrase | `EvaluateBudgetAlerts` |
| Stream of data | `observe<Thing>()` | `observeMessages()` |

## 4. Reusable components

A reusable component is an ordinary composable function in `core/designsystem/components`. The same rules apply to all of them.

### Rules

- **Stateless.** A component receives what to show and lambdas for what happens. It never holds a ViewModel or reads a repository.
- **Takes a `Modifier`** as its first optional parameter, so callers control size and spacing.
- **Uses theme tokens only.** Colours, spacing, and text styles come from the theme. Nothing is hard-coded, so a change happens in one place.
- **Has a `@Preview`** in light and dark themes, with sample data.
- **Uses slots for flexible content.** A card takes a `content: @Composable () -> Unit` rather than ten parameters.
- **Is extracted when needed twice.** Build it inside its feature first. When a second feature needs it, move it to `core`. This avoids guessing what will be shared.

### The component list

These come from the mockups. Where a component appears on several screens it is built once.

| Component | Used on | Notes |
|---|---|---|
| `BudgetProgressBar` | Home, Trends, Monthly budget | Colour comes from `BudgetStatus`: blue below 80%, amber from 80%, red over budget |
| `StatusColors` / `BudgetStatus` | Everywhere status appears | One function turns spent and budget into a status. Colours and icons for each status live in one file |
| `MoneyText` | Every amount | Formats paise as ₹ with Indian grouping (₹1,25,000) and negatives such as -₹2,000. The only place money is formatted |
| `CategoryCard` | Home, Trends | Emoji, name, used against budget, left, progress bar |
| `EmojiIconBox` | New category, category rows, chips | Shows one emoji. When editable it opens the system keyboard |
| `CategoryChip` and `CategoryChipGrid` | Add transaction, filters | Selectable, with an optional "Suggested" label |
| `MonthSelector` | Home, Monthly budget | Previous and next arrows |
| `SummaryCard` | Home, Trends | Spent, budget, left, and a progress bar |
| `StatusBadge` | Messages, Message detail | Icon and label, so colour is never the only signal |
| `FilterChipRow` | Messages | Chips with counts |
| `SegmentedTabs` | Trends | Three-way tab control |
| `SwipeRow` | Messages | Wraps Compose's swipe-to-dismiss with a configurable start and end `SwipeAction` (icon, colour, enabled) per row, so the reveal strip always matches what that swipe will really do. Varies by status: Accept/Reject on a Not assigned row, a neutral "undo" on Accepted/Rejected's one live direction. Was also used by Categories' swipe-to-archive until archiving was removed from v1 2026-09-25 |
| `AmountField` | Add transaction, budget sheet | A rupee field that keeps whole numbers and shows Indian grouping |
| `FormSheet` | New category, edit budget amount | One bottom sheet used for both "edit an amount" and "new category with amount" |
| `ConfirmDialog` | Backup restore, delete | Title, message, confirm, and cancel |
| `EmptyState` | Any list | Icon, message, and an optional action |
| `TransactionRow` | Category detail | Merchant or note, date, amount, and an "SMS" or "Manual" tag. Wrapped in `SwipeRow` for delete |
| `UndoSnackbar` | Messages (reject), Category detail (delete) | A short bar with an Undo action, driven by state rather than one-off events |
| `DonutChart` | Trends: This month | Drawn on Canvas. Each slice shows an emoji and a used part |
| `BarChart` | Trends: Previous month, Historic | Drawn on Canvas. Supports grouped bars and a 100% line |
| `AppScaffold` | Every screen | Top bar, optional bottom bar, snackbar host, and the standard padding |

### State holders

A screen's complexity goes into its ViewModel. A complex reusable component (such as the emoji box) may have a small plain-Kotlin state holder class beside it. It does **not** use a ViewModel, because ViewModels belong to screens.

## 5. Data model

All amounts are `Long` paise. Dates are stored as epoch milliseconds, with a month key (`2026-09`) stored beside them for fast monthly queries.

| Table | Key fields | Notes |
|---|---|---|
| `category` | id, name, emoji, sortOrder, archived | The `archived` column stays in the schema but is unused in v1 after archiving was removed 2026-09-25 (dropping it would need a migration, which wasn't worth it for a column that just always reads false) |
| `monthly_budget` | id, monthKey, categoryId, amountPaise | Unique on (monthKey, categoryId). A category with no row shows ₹0 |
| `transactions` | id, amountPaise, occurredAt, monthKey, categoryId, note, sourceMessageId (nullable) | `sourceMessageId` links back to the SMS when there is one. The table is named `transactions` because `transaction` is an SQL keyword |
| `sms_message` | id, sender, body, receivedAt, smsProviderId, dedupeKey, parsedAmountPaise (nullable), merchant (nullable), suggestedCategoryId, status, isNew | `status` is Not assigned, Accepted, or Rejected. `dedupeKey` is unique, which stops the same SMS being saved twice (R11). The amount and merchant are null when the text could not be parsed |
| `alert_log` | monthKey, categoryId, type | Unique on all three. Makes each alert fire once per category per month (R21) |
| `keyword_rule` | keyword, categoryId | Drives category suggestions. Seeded with defaults. The app has no screen to edit them in v1 (backlog) |

**Seeded data.** When the database is first created, a Room callback inserts the nine starter categories (Rent, Groceries, Food & Dining, Transport, Bills & Utilities, Entertainment, Shopping, Health, Other) and their default keywords. Starter budgets are ₹0.

**Deleting a transaction** that has a `sourceMessageId` sets that message back to Not assigned in the same database transaction.

Rules for the schema:

- `exportSchema = true`, and the exported schema files are committed. Every change gets a version bump and a written migration (R23).
- Money and month logic exist in one place each (`Money`, `MonthKey`), and both have tests.
- Only spend messages create `sms_message` rows. OTPs are never stored (R13, R14).

## 6. Handling SMS safely

Phase 1 is testing this. The real design follows the same rules:

- The **receiver stays tiny.** It parses, saves, and notifies. Parsing is a plain function of the text and takes milliseconds. The receiver wraps its work in `goAsync()` so Android keeps it alive while the database write finishes, and it always finishes, even on error.
- **It never throws.** A crash in the receiver would hide the very messages we want to catch. On any failure it saves the raw text as an unparsed message, so nothing is lost.
- **Parsing is isolated.** `BankRules.kt` holds one small block per bank, each with sample messages and tests. Adding a bank should never touch other banks' rules.
- **Catch-up on open.** `InboxScanner` reads the SMS inbox for anything newer than the last message processed, and the unique `dedupeKey` prevents double entries.
- **No SMS text in logs.** Log outcomes ("parsed 1 message"), not content.
- **Hilt in a receiver:** `@AndroidEntryPoint` cannot be used here - confirmed in M2a. It requires calling `super.onReceive()` first, but that method is abstract in the Android SDK's `BroadcastReceiver` and can never actually be called; this is a genuine, longstanding Dagger/Hilt limitation (google/dagger#1918), not a workaround-of-convenience. `SmsReceiver` instead stays a plain `BroadcastReceiver` with a manual `@EntryPoint interface` fetched via `EntryPointAccessors.fromApplication()`.
- **WorkManager is not used in v1.** The receiver plus the catch-up scan cover the need. Add it later only if Phase 2 testing shows the receiver being cut short.

## 7. Settings and backup

- **DataStore** holds settings: alert switches, months shown in charts, and the last export date.
- **Backup** writes one JSON file containing every table plus a `formatVersion`. Export and import use Android's file picker, so the app needs no storage permission. Import checks the version, shows a summary, and asks for confirmation before replacing data.

## 8. Testing approach

| What | Kind | Where | Tools |
|---|---|---|---|
| SMS parsing against real samples | Unit | `src/test` | JUnit, plus a text file per bank |
| Budget maths, status, month boundaries, Indian formatting | Unit | `src/test` | JUnit |
| Category suggestion | Unit | `src/test` | JUnit |
| ViewModels | Unit, with fake repositories | `src/test` | JUnit, coroutines test library |
| DAOs and migrations | Instrumented | `src/androidTest` | Room's in-memory database and migration testing |
| The main flows (accept a message, add a transaction) | UI | `src/androidTest` | Compose UI testing |
| Receiver on a real phone | Manual checklist | The Phase 1 method | Real SMS |

Unit tests cover the logic where a silent bug would give wrong money numbers. UI tests are few and cover only the important flows. Prefer fakes to mocks.

## 9. What gets built when

The milestone-by-milestone order, with tests and "done when" checks, is in [14-implementation-plan.md](14-implementation-plan.md). In outline:

| Phase | Architecture pieces |
|---|---|
| 2 (M0 to M4) | Project setup, Hilt, the whole Room schema with seeded categories and keywords, the domain rules (`Money`, `MonthKey`, budget maths), `sms/*`, DataStore for the catch-up marker, Messages and Add transaction features, and the shared components they need |
| 3 (M5 to M9) | Monthly budget (including category management - create, rename, reorder), Home, Category detail, alerts, Settings, and `backup/` |
| 4 (M10 to M12) | Trends, `DonutChart`, `BarChart`, `BuildTrendSeries`, and release hardening |

## 10. Technical decisions (defaults adopted)

Recorded in [07-open-questions.md](07-open-questions.md), rows 25 to 28.

1. **Application ID:** `com.budgetmanager.app`. It is the permanent name of the app on your phone and cannot be changed later without creating a different app. Change it before the project is created if you prefer another.
2. **Dependency injection:** Hilt.
3. **Navigation:** Navigation 3 if it is stable when the project is created, otherwise Navigation Compose.
4. **Git:** a private repository, in a project folder outside OneDrive. Sample SMS files never go into a public repository.

## Sources

- [Recommendations for Android architecture (Android Developers)](https://developer.android.com/topic/architecture/recommendations)
- [Room 3.0: Modernizing the Room (Android Developers Blog)](https://android-developers.googleblog.com/2026/03/room-30-modernizing-room.html)
- [Navigation 3 (Android Developers)](https://developer.android.com/guide/navigation/navigation-3)
- [Dependency injection with Hilt (Android Developers)](https://developer.android.com/training/dependency-injection/hilt-android)
