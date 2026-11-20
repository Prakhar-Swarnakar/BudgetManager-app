# Budget Manager — cleanup plan (unused / redundant code + md docs)

## Context

You asked for a review of the app for unused or redundant code and stale docs, with a
plan only — nothing changed yet. This replaces the earlier Add Transaction mockup plan
(that work is built). Nothing in the repo was edited during this review; the only side
effect was Gradle regenerating its own `app/build/` outputs.

Treated as **used** throughout (never proposed for removal): `SmsReceiver` and every
manifest entry, notification channels in `Notifier`, all Room entities / DAOs /
`MIGRATION_1_2`…`6_7` / schema JSONs, the three Hilt modules and every `provide*`/`bind*`,
`BudgetApp`, all `Destination`s, use-case `invoke` operators. There are no workers and
no `@Preview` functions in the project.

## 1. Baseline (2026-11-16, `./gradlew assembleDebug lint test --continue`)

| Task | Result |
|---|---|
| `assembleDebug` | passes |
| `test` (unit) | passes — 297 tests, 0 failures |
| `lint` | **fails** — 2 errors, 32 warnings, 2 hints (all pre-existing) |

Overall Gradle exit is 1 because of lint only. The 2 errors are
`PermissionImpliesUnsupportedChromeOsHardware` on `AndroidManifest.xml:5-6`
(RECEIVE_SMS / READ_SMS). After cleanup the same command must give: build pass, 297
tests pass, lint errors ≤ 2 → 0 if item H1 is taken, warnings lower.

## 2. Removal / consolidation list

Overall finding: the Kotlin code is tight. No dead private members, no unused DAO
methods, no commented-out code, no `Log`/`println` calls, no TODO/FIXME anywhere.
What is left is mostly template leftovers and build config.

### High confidence (no behaviour change)

| # | What | Where | Why |
|---|---|---|---|
| H1 | 7 template colours | `res/values/colors.xml` (whole file) | Lint `UnusedResources`; theme is in Compose (`ui/theme/Color.kt`). Delete the file |
| H2 | `ScreenPlaceholder` | `core/designsystem/components/ScreenPlaceholder.kt` | "Temporary content for a screen not yet built" — zero references in main or tests |
| H3 | `Money.unaryMinus` | `core/model/Money.kt:13` | No call site in main or tests |
| H4 | `buildConfig = true` | `app/build.gradle.kts` | `BuildConfig` is referenced nowhere |
| H5 | Unused test-only deps | `app/build.gradle.kts` + `libs.versions.toml`: `androidx-espresso-core`, `androidx-compose-ui-test-junit4`, `androidx-compose-ui-test-manifest`, and the `androidTestImplementation(platform(compose-bom))` line that only serves them | No Espresso or Compose UI test exists in `androidTest` (5 files, all Room/PDF tests) |
| H6 | Unused imports | 1 in main (`FakeTransactionSplitRepository.kt`: `Money`), 7 in tests (`BuildMonthlyTrendsTest`, `MatchStatementTest`, `MonthlyBudgetViewModelTest`, `HomeViewModelTest`, `StatementValidatorViewModelTest`, `AddTransactionViewModelTest`, `DedupeKeyTest`) | Name never appears in the file body |
| H7 | `RedundantLabel` | `AndroidManifest.xml:25` (activity `android:label`, same as application's) | Lint; attribute only, the activity entry stays |
| H8 | `ObsoleteSdkInt` checks | `sms/Notifier.kt:88,99` | minSdk is 26, so the `SDK_INT >= O` guards are always true; channel creation itself stays |
| H9 | Boilerplate keep file | `app/src/main/keepRules/rules.keep` | Comments only; R8 is disabled in release. (Keep it if you plan to enable minify for M12) |

### Medium confidence (safe, but touches more files or needs your call)

| # | What | Why / caveat |
|---|---|---|
| M1 | Move all 13 fakes out of `src/main` into `src/test` (`data/repository/Fake*.kt` ×11, `domain/FakeBudgetAlertNotifier.kt`, `sms/FakeInboxScanner.kt`) | No production code references any of them; today they ship in the APK. No `androidTest` file uses them, so `src/test` is enough. Needs the `[FakeTransactionSplitRepository]` KDoc link in `OwedEntryGrouping.kt` reworded |
| M2 | Duplicate SMS→`SmsMessage` construction | `SmsReceiver.handle()` and `DefaultInboxScanner.scanAndIngest()` hold the same ~40-line debit/credit/else block. Extract one `buildMessage(sender, body, timestamp, categorySuggester, taxonomySuggester): SmsMessage?` in `sms/`. Core path — worth doing, but verify with a real SMS on-device |
| M3 | `ui-tooling-preview` (impl) and `ui-tooling` (debug) | No `@Preview` exists. Removing is safe today, but `13-development-best-practices.md §3` says every component should have a preview — either drop the deps or keep them and fix the doc rule (see docs D13) |
| M4 | Deprecated API uses | `hiltViewModel` old package in `AddTransactionScreen.kt` / `AppNavigation.kt`; `Icons.Filled.ShowChart` → `AutoMirrored` at `AppNavigation.kt:60`. Not dead code, but removes the compiler warnings |
| M5 | `UseKtx` / `AutoboxingStateCreation` lint hints | `SettingsScreen.kt:115`, `BudgetCategoryList.kt:51,53` — one-line swaps |

### Low confidence (list only — recommend leaving)

| # | What | Why leave |
|---|---|---|
| L1 | `TransactionSplitRepository.settle()` + `TransactionSplitDao.settle()` | Only called from tests; product code settles through the wholesale split replace in Add Transaction. But it is a DAO method and docs say per-friend settle is "paused, capability untouched" |
| L2 | `SmsMessage.smsProviderId` / `sms_provider_id` | Always written as `null`, never read. Removal needs a migration and a backup-format change — not worth it |
| L3 | `category.archived` column | Already documented as deliberately kept |
| L4 | `Money.minus` | Unused in main but used by 3 tests and pairs naturally with `plus` |
| L5 | `docs/phase1-test-app/` (6 files) | Historic Phase 1 test app; still referenced by `10-phase-1-guide.md` |
| L6 | `docs/ui-mockups/03-add-transaction-redesign.html` Option 1 / Option 3 rails | Option 1 declined, Option 3's premise was wrong. Trim or annotate rather than delete |
| L7 | Remaining lint warnings | `ConstantLocale` ×3, `BatteryLife`, dependency-version notices, bouncycastle `TrustAllX509TrustManager`, the 2 ChromeOS errors (fix = two `<uses-feature android:name="android.hardware.telephony" android:required="false"/>` lines, an addition not a removal) — separate decisions, not cleanup |

Not redundant after checking: the three hand-rolled `AlertDialog`s in Settings /
Statement validator (they have custom content `ConfirmDialog` can't hold), the
Home/CategoryDetail summary-card similarity (small, different data), and the
`getValue`/`setValue` imports (needed by `by` delegates).

## 3. Markdown changes needed

| # | File | Change |
|---|---|---|
| D1 | `README.md` (root) | Status paragraph is one enormous run-on changelog: shorten to current state and point at `docs/06-backlog.md`. Fix "M7 … not yet tested on-device" if now tested; "settle all-or-nothing / one row per friend per split" is outdated (one row per transaction since 2026-10-13); add schema is at v7 |
| D2 | `docs/README.md` | "Last updated: 2026-10-13" → current; Phase/status paragraph same trimming; `06-backlog.md` described as "Ideas deliberately postponed" though it now is the detailed changelog; mention mockups `01`–`03`; drop or keep `phase1-test-app` row per L5 |
| D3 | `docs/02-features.md` F6c | Remove "each either equal share or custom amount" toggle wording → one amount box with "Equal" placeholder; **remove** "a warning says saving will reset everyone back to unsettled" (false: settled state now survives an edit, test exists, no dialog); add the Settled/Pending chip on existing splits |
| D4 | `docs/06-backlog.md` | "Split with friends" entry: same two stale claims as D3. "Simplify Add Transaction's split UI" is marked *Proposed 2026-11-15* but is built → mark Built, fix the stray blank line that breaks the table before it. Add a Built entry for the Add Transaction redesign (Save in top bar, Title+Amount row, date label under SMS banner, `saveAttempt` scroll-to-error). Add this cleanup once done. Note the known date-picker off-by-one as an open item |
| D5 | `docs/07-open-questions.md` | Row 6 "Credit SMS: Ignored in v1" is superseded (credit detection built 2026-10-03). The open item about the Cancel button "needs 3 taps" is moot — Cancel no longer exists. Line 68 is missing its leading `|` (broken table row) |
| D6 | `docs/08-pages-and-navigation.md` | Add transaction section: order is now SMS banner → date label → Title+Amount row → Category → Payment method → Split → (boxed Date only for manual adds) → Note; "A large amount field" and "Cancel and Save buttons" are wrong (Save is in the top bar, back = cancel). Owed to you: "Settling is paused… planned to move to Add Transaction" → it has moved (Settled chip). "Validate a statement: Pick a CSV" in the page table → CSV/Excel/PDF. Mockup note should mention `01`–`03` |
| D7 | `docs/12-tech-architecture.md` §3 | Folder tree is out of date: package is `com.budgetmanager.app`; theme lives in `ui/theme/` not `core/designsystem/theme/`; no `core/notifications/` (it is `sms/Notifier.kt`); no `proguard-rules.pro` (it is `src/main/keepRules/`); no `test/resources/sms/`; `domain/BuildTrendSeries.kt` → `BuildMonthlyTrends`/`BuildThisMonthTrends` plus the statement, split and duplicate use cases; `sms/` list is missing `SpendClassifier`, `DedupeKey`, `TaxonomySuggester`, `DefaultInboxScanner`; `feature/` is missing `statementvalidator/`; `owedtoyou/` comment still says "one row per friend… with a Settle action" |
| D8 | `docs/12-tech-architecture.md` §4 | Component list names things that don't exist (`MoneyText`, `CategoryCard`, `SummaryCard`, `SegmentedTabs`, `FormSheet`) and omits ones that do (`RuleGroups`, `TaxonomyDropdown`, `CategoryDropdown`, `PermissionGate`, `DateField`/`DateEditLabel`). `CategoryChipGrid` "used on Add transaction, filters" → used only by Category rules. Remove `ScreenPlaceholder` if H2 is done. "Has a `@Preview`" rule vs reality (M3). Naming table: say where fakes live after M1 |
| D9 | `docs/12-tech-architecture.md` §5 | Add the missing tables: `friend`, `keyword_rule`, `taxonomy_keyword_rule` (check `transaction_split` row wording on settle) |
| D10 | `docs/13-development-best-practices.md` | §3 "Every component has a preview" and "Theme tokens… spacing" are not followed (0 previews, no `Spacing` object) — downgrade to Should / mark as known gap; §11 "Every database migration has a test" — say hand-diff + on-device rehearsal is the actual process while `MigrationTestHelper` is broken; "UI tests cover main flows" — none exist; §12 "Keep a CHANGELOG in README" and "Branch per feature" don't match practice (backlog is the changelog, work is on `main`) |
| D11 | `docs/14-implementation-plan.md` | Header status still says the Cancel-button fix is unconfirmed and M7 untested; add a progress row for 2026-11-15/16 (split simplification, redesign, cleanup); `sms-samples-template.txt` step is obsolete (M2b done differently) |
| D12 | `docs/04-messages-and-notifications.md` | Line 9 still says a message needs "a debit word and a rupee amount" — the inclusion gate dropped the amount requirement on 2026-10-07 |
| D13 | `docs/01`, `03`, `05`, `09`, `10`, `11` | Skimmed by keyword search only, no stale claims found; give `03-user-flows.md`'s add-transaction flow one read for Cancel/Save wording when doing D6 |

## 3b. Second pass (2026-11-16) — additions

Extra checks run: unused function parameters, enum values, DataStore keys,
`SettingsRepository` methods, navigation routes, theme colours, `StarterData`, test
helper functions, root Gradle/settings files, XML resources, tracked IDE files, and a
proper read of the six docs that were only keyword-searched the first time.

**Confirmed clean (nothing to remove):** every DataStore key and settings method is
used; all 11 `Destination`s are navigated to; every `StatusColors` / `Color.kt` value
is used; no unused test helpers; root `build.gradle.kts` plugins all applied.

### New code items

| # | Confidence | What | Where | Why |
|---|---|---|---|---|
| H10 | High | Unused parameter `expanded` | `feature/owedtoyou/OwedToYouViewModel.kt` `buildFriendSections(...)` (~line 126, call ~120) | Never read in the body; expansion is applied in `OwedToYouContent` from `state.expandedFriendIds` |
| M6 | Medium | Per-user IDE files are tracked in git | `.idea/deploymentTargetSelector.xml` (shows as modified in every status), `.idea/androidTestResultsUserPreferences.xml` | Machine-specific state; `git rm --cached` + add to `.gitignore`. Files stay on disk |
| M7 | Medium | Small duplication inside `Notifier` | `sms/Notifier.kt` — the `openApp` `PendingIntent` is built twice (lines ~32, ~69) and `ensureChannel` / `ensureBudgetAlertChannel` are the same function with different ids | Fold into one private `openAppIntent()` and one `ensureChannel(id, name)`. Both channels keep their ids — nothing user-visible changes. Pairs naturally with H8 |
| L8 | Low | Identical `otpRegex` in both classifiers | `sms/SpendClassifier.kt:17`, `sms/CreditClassifier.kt:18` | One line each; the two classifiers are deliberate mirrors. Leave |
| L9 | Low | Permission launchers duplicated | `PermissionGate.kt` and `SettingsScreen.kt` | Different surrounding UI; extracting would be a thin wrapper. Leave |
| L10 | Low | Template XML with the project's only `TODO` | `res/xml/backup_rules.xml`, `res/xml/data_extraction_rules.xml` | Manifest-referenced, so treated as used. Could be tidied to say "nothing is backed up" explicitly, but that is a behaviour decision, not cleanup |
| L11 | Low | `android.disallowKotlinSourceSets=false` | `gradle.properties` | Purpose unclear — needs a check that the build still configures without it before touching |

False positives ruled out: `TaxonomyType.CREDIT_CARD_VIA_UPI` / `OTHER` are never named
in code but reach the UI through `entries` and are stored in the database; DAO and
repository "unused parameters" are `@Query` bindings / interface signatures.

### New doc items

| # | File | Change |
|---|---|---|
| D14 | `docs/03-user-flows.md` | Flow 3 still calls the field **Merchant** (renamed Title 2026-10-04) and has no Save-in-top-bar / back-to-cancel wording. Flow 4 step 2 "checks whether it is a debit message. If not, it is ignored" — credits are kept since 2026-10-03, and an amount is no longer required. Flow 5 line 50 says the Rejected→Not assigned swipe is "currently under investigation" — root-caused and fixed 2026-10-13. No flows exist for Split with friends, Owed to you, or Validate a statement |
| D15 | `docs/01-product-overview.md` | Decision log rows are stale: Navigation "Bottom bar for Home, Messages, and Trends" (Owed to you missing); Tech stack "Canvas charts not built until M10" (built); Architecture "proven across M0 to M4"; Implementation "M0 to M6 done" |
| D16 | `docs/09-risks-and-phases.md` | R11 says the dedupe key uses the SMS id — it is timestamp + body (`DedupeKey`), `smsProviderId` is always null. R13 "a message needs a debit word and an amount" — amount no longer required. R16 duplicate payments marked "Backlog" — built 2026-10-01 |
| D17 | `docs/05-budget-rules.md` | Calculations section doesn't mention that only **your share** of a split counts toward spent, nor Home's Income / Saved lines |
| D18 | `docs/14-implementation-plan.md` | Opening paragraph is headed "Status on 25 September 2026" and describes M7/M8 as untested — replace with a short current status (extends D11) |
| — | `docs/10-phase-1-guide.md`, `11-phase-1-results.md` | Historic Phase 1 records. Read; nothing to correct. Leave as is |

## 4. Suggested order when you approve

1. High list (H1–H10) in one pass → build, 297 tests, lint. Do M7 alongside H8
   (same file), M6 as a git-only step.
2. M1 (move fakes) on its own → tests.
3. M2 (SMS builder) on its own → tests + install + one real/test SMS on the Pixel.
4. M3–M5 as you decide.
5. Docs D1–D18, plus a backlog entry recording what was removed and what was
   deliberately left (L1–L11).

No schema change anywhere, so no migration check is needed. No commits unless you ask.

## 5. Verification

- `./gradlew assembleDebug lint test --continue` after each step; compare with §1.
- After M1: confirm `app-debug.apk` no longer contains `Fake*` classes.
- After M2: install on the Pixel 7a, receive one SMS and run "Fetch SMS" for the
  month; confirm one row appears, no duplicate.

## Open questions for you

- M3: remove the preview tooling, or keep it and start adding previews?
- H9: keep `rules.keep` for a future minified release?
- Still unanswered from earlier: fix the date-picker off-by-one; delete the two phone
  DB copies in the temp folder; commit the pending work.
