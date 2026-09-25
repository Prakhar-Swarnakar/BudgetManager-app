# Development Best Practices

The rules we follow while building the real app. They sit on top of the structure in [12-tech-architecture.md](12-tech-architecture.md). Each rule is short and has a reason, so we can tell when it is worth bending.

Where a rule protects your money numbers or your data, it is marked **Must**. The others are **Should**: follow them unless there is a good reason.

## 1. Architecture rules

| Rule | Level |
|---|---|
| Dependencies point downward: UI, then domain, then data. Nothing in `core` or `domain` imports Android UI code, Room, or a feature | Must |
| Only the data layer touches Room and DataStore. Screens and ViewModels use repository interfaces | Must |
| One ViewModel per screen, exposing one `UiState` as a `StateFlow`. User actions arrive as ViewModel methods | Should |
| No Android `Context`, Activity, or resources inside a ViewModel | Should |
| Features never import other features. Shared code moves to `core` | Should |
| Business rules live in plain Kotlin functions that can be tested without a phone | Must |
| Repositories are interfaces with a Room version and a fake for tests | Should |
| Add a use case only when a rule spans several repositories or is shared by several screens | Should |

## 2. Kotlin style

- Follow the official Kotlin coding conventions. Android Studio's **Reformat Code** (Ctrl+Alt+L) is the formatter. Run it before every commit.
- Prefer `val` over `var`, and immutable data classes for state. Copy a state object to change it, rather than modifying it.
- Use sealed types for states with a fixed set of shapes, such as `Loading`, `Success`, and `Error`. The compiler then forces every case to be handled.
- Avoid `!!`. If a value can be null, handle it. If it cannot, make the type say so.
- Name things for what they mean in the product: `remaining`, `budget`, `spent`, not `x`, `val2`.
- Keep functions short and single-purpose. If a function needs a comment to explain its sections, split it.
- Comments explain **why**, not what. Never leave commented-out code. Git remembers.
- Turn on compiler warnings as errors for new code once the project is stable, and fix warnings as they appear.

## 3. Jetpack Compose

- **Stateless components.** Pass state in and events out. Hoist state to the lowest common parent that needs it.
- **Two functions per screen.** `XScreen` (stateful) gets the ViewModel. `XContent` (stateless) draws the state. Previews and UI tests use `XContent`.
- **Collect state safely:** `collectAsStateWithLifecycle()`, never plain `collectAsState()`, so collection pauses while the app is off-screen.
- **No one-off events from the ViewModel.** Put the effect in state (for example `showUndo = true`) and clear it after the UI handles it. This avoids lost or repeated events.
- **`Modifier` is the first optional parameter** of every component and is applied to the outermost element.
- **Use `LazyColumn` with stable `key`s** for lists (a message's ID, not its position). This keeps swipe and animation state attached to the right row.
- **Never do work inside a composable body.** No database calls, no sorting big lists, no formatting in a loop on every redraw. Compute in the ViewModel, or use `remember` with the right key.
- **Every screen handles four states:** loading, empty, error, and content.
- **Every component has a preview** in light and dark themes.
- **Theme tokens, not literals.** Colours, spacing, text styles, and shapes come from the theme. Status colours live in one file.

## 4. Coroutines and Flow

- Repositories expose `Flow` for data that changes and `suspend` functions for one-off actions.
- Never use `GlobalScope`. ViewModels use `viewModelScope`.
- Database and file work never runs on the main thread. Room's `suspend` and `Flow` functions handle this. Everything else that blocks uses an injected dispatcher, so tests can replace it.
- ViewModels expose state with `stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initial)`.
- Catch failures at the boundary (repository or ViewModel) and turn them into state the screen can show. Do not let exceptions travel to the UI.
- Never swallow `CancellationException`.

## 5. Money, dates, and numbers

These protect the app's core promise of honest numbers, so they are **Must**.

- **Store money as `Long` paise.** Never `Double` or `Float`. Convert only when formatting.
- **Format money in one place** (`MoneyText` and its underlying formatter). It handles ₹, Indian grouping (₹1,25,000), and negatives (-₹2,000). Test it.
- **Parse user-entered amounts strictly.** Reject empty, zero, negative, or non-numeric input before it reaches a repository.
- **Use `java.time` for dates.** Store a timestamp and a month key such as `2026-09`. Take the transaction date from the SMS time, in the phone's time zone.
- **Test month edges:** 31 December to 1 January, a leap February, and a transaction at 23:59 on the last day.
- **Remaining is `budget - spent`**, and may be negative. Never clamp it to zero.
- **Percent used is computed once**, in a pure function, and reused by the bar, the status, and the alerts. Do not recompute it in three places.

## 6. Database (Room)

- `exportSchema = true`, and commit the schema files. They let us test migrations.
- **Every schema change bumps the version and ships a migration.** Never use destructive migration in a release build. It deletes your data.
- **Test each migration** against a database created at the previous version, with sample data in it.
- Related writes go in **one database transaction**. Accepting a message is the example: insert the transaction and mark the message accepted together, or not at all.
- Add indexes for columns used in filters (month key, category ID, status). Add unique constraints where duplicates must not exist (`dedupeKey`, budget per month and category).
- Do not use `fallbackToDestructiveMigration`. If a migration is hard, ask first.
- Write DAO functions as `suspend` or `Flow`, so they match what Room 3.0 will require.

## 7. SMS, privacy, and security

- **Ask for only what is needed.** SMS permissions, notification permission, and nothing else. No internet permission in the release app, unless a future feature needs it and we agree on it.
- **Never log message text or amounts.** Log outcomes only ("parsed 1 message").
- **Never store OTPs.** A message needs an amount and a debit word to become a spend.
- **Sample SMS files** for tests have card digits, account numbers, names, and balances removed. They never go into a public repository.
- **`allowBackup` is off** (decision 29 in [07-open-questions.md](07-open-questions.md)). With it on, Android's cloud backup would copy SMS-derived data to a Google account. Your own export file is the backup instead.
- **The receiver never crashes.** It catches everything and saves the raw text if parsing fails. Never make network calls or do long work in it.
- **Keep the receiver's manifest entry restricted** with the `BROADCAST_SMS` permission, as in the Phase 1 test app.
- **Explain permissions before asking**, in the app's own words, and handle "denied" without dead ends. A link to system settings is the fallback.
- **Secrets stay out of the project.** The signing key file and its passwords live outside the repository, and `.gitignore` lists `*.jks`, `*.keystore`, `keystore.properties`, and `local.properties`.

## 8. Errors and edge cases

- Anything that can fail (parsing, import, permission, a missing file) has a defined outcome that the user can see and understand. Silent failure is the worst kind.
- Show plain-language messages ("Could not read that file. Pick a backup made by this app."), never stack traces.
- Backup import validates the file before touching data: format version, required fields, and sanity checks. It replaces data only after confirmation, and only if the whole file is valid.
- Deleting something that has history keeps the history where it can. A category is never deleted while it still has transactions attached to it.
- Undo beats confirm for common actions. Swipe-to-reject shows an undo bar for a few seconds.

## 9. Accessibility

- Touch targets are at least 48dp.
- Colour is never the only signal. Status uses an icon and a label as well as a colour, as the mockups already do.
- Every icon or emoji that carries meaning has a content description.
- Text uses scalable sizes (`sp`), and screens are tested at the largest font setting.
- Check contrast in both light and dark themes.

## 10. Performance and battery

- Do not run anything in the background that does not need to. The app reacts to SMS through the receiver and does not poll.
- Keep the receiver's work to milliseconds.
- Avoid heavy work on start-up. Load the current month first, and compute trends when the Trends screen opens.
- Check any list with more than a few hundred rows for smooth scrolling. Message history can grow.
- Measure before optimising. Use Android Studio's profiler if something feels slow.

## 11. Testing

| Rule | Level |
|---|---|
| SMS parsing has a test for every real sample message, with the expected amount, merchant, and debit or credit result | Must |
| Budget maths, status thresholds (79%, 80%, 100%, 101%), month boundaries, and money formatting have tests | Must |
| Every database migration has a test | Must |
| Every new bank pattern is added together with its sample messages | Must |
| ViewModels are tested with fake repositories | Should |
| A bug found on the phone gets a test that fails first, then a fix | Should |
| UI tests cover only the main flows (accept a message, add a transaction, edit a budget) | Should |

Prefer fakes over mocks. A fake repository behaves like the real one and makes tests read clearly.

Tests must not depend on the current date or the phone's time zone. Inject a clock.

## 12. Git and project hygiene

- **The code is a separate project folder outside OneDrive**, with its own Git repository.
- **Commit small and often**, one idea per commit. The message says what and why: "Fix month rollover for 31 Dec transactions".
- **Branch per feature** (`feature/messages-page`), merged into `main` when it works. `main` always builds.
- **Never commit** the signing key, passwords, `local.properties`, the `build` folder, or real SMS text.
- **Keep a `CHANGELOG`** section in the code project's README. It records what changed in each version.
- **The version catalog** (`libs.versions.toml`) holds every library version. Update versions on purpose, one at a time, and build after each.
- **Library policy:** add a library only when writing it ourselves would be slower and riskier, and when it is actively maintained. Every dependency is something we must keep updated.

## 13. Releasing and updating

- **The same signing key for every release.** A phone only accepts an update signed with the original key. Back the key up in two places.
- **Increase `versionCode` for every release**, and set a readable `versionName`.
- **Release builds use R8 minification.** Test the release build on the phone, not just debug, because minification can remove code that is reached only indirectly.
- **Never uninstall the app to update it.** Uninstalling deletes your data. Install the new APK over the old one (`adb install -r`, or Run from Android Studio).
- **Export a backup before every update**, until updates have proved safe.
- Record what happened in the results file after each release, especially install warnings (R1, R7).

## 14. Definition of done

A feature is done when all of these are true:

1. It works on your Pixel, not only in a preview.
2. Loading, empty, error, and content states are handled.
3. Money, dates, and rules are in the pure-Kotlin layer, with tests.
4. New components are in the design system, have previews, and use theme tokens.
5. No warnings, no `!!`, no leftover debug code, no logged message text.
6. Any schema change has a version bump, a migration, and a migration test.
7. The docs are updated (decision log, flows, or pages, whichever changed).
8. It is committed with a clear message.

## 15. How we work together

- **I write the code and you build it in Android Studio**, because my environment cannot reach the Android build servers. So I keep changes small and self-contained to make errors easy to trace.
- **When the build fails**, paste the first error from the Build panel. It usually explains the rest.
- **When something looks wrong on the phone**, tell me what you did, what you expected, and what happened. Add a screenshot if you can.
- **Before a bigger change**, I say what will change and why, and record the decision in the log.

## Sources

- [Recommendations for Android architecture (Android Developers)](https://developer.android.com/topic/architecture/recommendations)
- [Kotlin coding conventions (kotlinlang.org)](https://kotlinlang.org/docs/coding-conventions.html)
