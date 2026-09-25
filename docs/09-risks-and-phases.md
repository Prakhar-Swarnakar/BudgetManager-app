# Risks and Build Phases

This document lists what could be hard or could go wrong, ranks each item by importance, and uses that ranking to split the build into four phases. Phase 1 is a small test that proves SMS reading, notifications, and installation work on your phone before anything else is built.

**Your setup:** a Pixel 7a running Android 17, Android Studio on your laptop, and a native Android app written in Kotlin with Jetpack Compose. This is your first Android app. You are in India, which matters for one of the installation risks below.

## How to read the ratings

| Rating | Meaning |
|---|---|
| Critical | If this fails, the app cannot do its main job or cannot be installed. Test it first |
| High | Likely to cause bugs or rework if we ignore it |
| Medium | Worth planning for, but fixable later |
| Low | Minor, or has a simple fix |

## What we checked

These facts drive the top risks. They come from the sources listed at the end.

- **Restricted settings:** an app installed from outside the Play Store cannot be granted SMS permission until the user opens the app's info page, taps the three-dot menu, and chooses **Allow restricted settings**. Until then the permission looks blocked.
- **Play Protect and SMS permission:** Google Play Protect can block installing an app that requests SMS permission (or notification-listener or accessibility permission) when the app comes from the internet, meaning a browser, a messaging app, or a file manager. Google describes this as active in "select markets", and it announced a pilot in **India** in October 2024. Sources say installs made offline are still allowed. We could not confirm from the sources whether the India block is active today, so this must be tested on your phone.
- **Developer verification:** Google is rolling out identity verification for developers. From 30 September 2026 it is enforced in Brazil, Indonesia, Singapore, and Thailand, and it expands globally in 2027. Unregistered apps can still be installed with ADB (a developer tool that installs apps over USB) or an "advanced flow" that includes a 24-hour wait. Google also plans a free "limited distribution" account for sharing an app with up to 20 devices without government ID. Its launch date differs between sources, so check its current status.
- **Why native Kotlin instead of Flutter:** a native app receives SMS directly from Android through a small built-in component, so no third-party plugin is involved. The popular Flutter plugin (`telephony`) was archived by its owner in March 2025, and its maintained fork (`another_telephony`) runs its background code in a separate context that Android may stop. Flutter's main benefit, one codebase for Android and iPhone, does not apply because iPhone apps cannot read SMS. Even natively, Android may stop long-running background work, so the SMS receiver must stay very small.
- **Notifications:** Android 13 and newer require the user to grant notification permission at runtime.

## Risk list

### A. Installing the app and getting permissions

| ID | Risk | Why it matters | Rating | What we do about it | Phase |
|---|---|---|---|---|---|
| R1 | Play Protect blocks the install because the app asks for SMS permission | If the app cannot be installed, nothing else works. It is most likely when the file comes from a browser, messaging app, or file manager | Critical | Install over USB from Android Studio or ADB instead of copying a file. Test both ways and record what happens | 1 |
| R2 | "Restricted settings" silently blocks the SMS permission | The permission prompt appears to do nothing, which looks like a bug in the app | Critical | Add a permission screen that explains the step and opens the app info page. Test on your Pixel | 1 |
| R3 | Notification permission denied or not asked | No notifications at all on Android 13 and newer | High | Ask at the right moment, handle "denied", and link to system settings | 1 |
| R4 | Updates and signing keys | An update only installs over the old app if it is signed with the same key. Uninstalling deletes all on-device data | High | Create a release signing key in Phase 1 and back it up. Never uninstall once real data exists | 1 |
| R5 | First-time setup of USB debugging, the Android SDK components, and the first build | Setup problems can eat time and are unrelated to the app itself. Android Studio is already installed, which removes most of this | Medium | Phase 1 has a step-by-step guide, and the test app is built from Android Studio's own project template so its settings match your Studio version | 1 |
| R6 | Developer verification rules reach India in 2027 | Installing by file may need extra steps or a verified developer account later | Medium | Keep installing over USB. Look at the free limited distribution account before 2027 | 4 |
| R7 | Google changes sideloading rules again | The install method that works today might not work later | Medium | Record exactly what works in Phase 1 and recheck before each phase | 1 to 4 |

### B. Reading SMS

| ID | Risk | Why it matters | Rating | What we do about it | Phase |
|---|---|---|---|---|---|
| R8 | Receiving SMS when the app is closed | The whole point is to catch spends without opening the app. Android may stop background work that runs too long, and a force-stopped app receives nothing until it is opened again | Critical | Keep the receiver tiny: save the message and show the notification, and do the heavy work later. Test with the app open, in the background, swiped away, force-stopped, after a reboot, and with the screen off for an hour | 1 |
| R9 | Android 17 treats SMS, notifications, or background receivers differently from what we expect | Rules change between Android versions, and I could not confirm Android 17's rules from documentation | High | Test on your Pixel 7a running Android 17. Keep all SMS code in one place so a fix touches one file | 1 |
| R10 | Missed messages | If the live receiver misses one, the user never sees that spend | High | On app open, scan the phone's SMS inbox for anything newer than the last message processed | 2 |
| R11 | The same SMS processed twice (once live, once by the inbox scan) | Double-counted spending | Medium | Give each message a unique key from its SMS id, time, and text, and skip repeats | 2 |
| R12 | Bank SMS formats differ across banks and change over time | Wrong or missing amounts and merchants make the app untrustworthy. This is the biggest risk to the product's value | Critical | Collect 30 to 50 real SMS from your banks (with personal details removed). Write parsing rules with automated tests. Anything that cannot be parsed still appears with its raw text so you can enter it by hand | 2 |
| R13 | Non-spend SMS treated as spends (OTPs, promotions, balance alerts, credits) | Noise in the Messages page and wrong numbers | High | Be strict: a message needs a debit word and an amount. Never store OTP messages | 2 |
| R14 | Privacy of SMS content | The app holds a sensitive permission and sees OTPs and personal details | High | Read only what is needed, keep everything on the phone, do not log message text, and leave the internet permission out of the release app | 1 to 2 |
| R15 | Long messages, dual SIM, and unusual sender names | Split messages or a second SIM can be missed or read wrongly | Medium | Test with real messages from each SIM and each bank | 2 |
| R16 | Duplicate payments (a bank alert and a UPI app confirmation for one spend) | Double-counted spending | Medium | Backlog: flag two messages with the same amount within a few minutes | 4 |
| R17 | Testing without waiting for real bank SMS | Slow testing hides bugs | Medium | Use the Android emulator to send test SMS, and send messages to your phone from another phone. Also test with a few real transactions | 1 |

### C. Notifications

| ID | Risk | Why it matters | Rating | What we do about it | Phase |
|---|---|---|---|---|---|
| R18 | Battery saving delays or drops notifications | The notification is how you find out about a spend | Medium | Pixel is the friendliest for this. Test with battery saver on and the screen off for an hour. If you switch phone brands, test again | 1 |
| R19 | Several messages arrive close together | A stack of separate notifications is annoying | Low | Update one notification with the new count ("3 new spends detected"). Burst grouping is already in the backlog | 2 |
| R20 | Tapping the notification opens the wrong screen, especially when the app is closed | The flow feels broken | Medium | Phase 1 only opens the app. Phase 2 opens the Messages page from a cold start | 2 |
| R21 | Budget alerts (80% and over budget) fire too often or not at all | Alerts you learn to ignore, or miss | Medium | Store which alerts were already sent for each category and month | 3 |

### D. Data and app logic

| ID | Risk | Why it matters | Rating | What we do about it | Phase |
|---|---|---|---|---|---|
| R22 | Losing data when the phone is lost, reset, or the app is uninstalled | All history lives only on the phone | High | Export and import in Settings. Until then, do not uninstall the app | 3 |
| R23 | Changing the database after real data exists | A wrong migration can corrupt or lose history | Medium | Use Room, version the schema, and test migrations with sample data (see [12-tech-architecture.md](12-tech-architecture.md)) | 2 |
| R24 | Month boundaries, time zones, and transaction dates | A transaction near midnight on the last day can land in the wrong month | Medium | Use the SMS date as the transaction date, and write tests for month-end cases | 3 |
| R25 | Rounding and number formatting for money | Small errors add up, and Indian digit grouping (₹1,25,000) is easy to get wrong | Medium | Store amounts as whole numbers of paise, and format for Indian grouping in one place | 2 |
| R26 | A category with a ₹0 budget that gets spending | The rules are not decided yet (see open questions) | Low | Decide the behaviour before Phase 3 | 3 |
| R27 | Emoji icon input | Users may type text instead of one emoji, and some emoji render differently | Low | Accept a single emoji only and test on your phone | 3 |

### E. Screens and charts

| ID | Risk | Why it matters | Rating | What we do about it | Phase |
|---|---|---|---|---|---|
| R28 | Swipe-to-accept and swipe-to-reject rows with coloured backgrounds | Gestures are easy to get slightly wrong (accidental swipes, no undo) | Medium | Use Jetpack Compose's built-in swipe-to-dismiss support, add a short undo, and test on the phone | 2 |
| R29 | The This month donut chart with a "used" part inside each slice | Chart packages usually draw plain pie charts, so this likely needs custom drawing | Medium | Build it with custom drawing in Phase 4 and keep the list below it as a fallback | 4 |
| R30 | Reorderable category list, drag handle sharing a row with a tap-to-edit target | Was originally about drag-to-reorder conflicting with swipe-to-archive on one row; archiving was removed from v1 entirely 2026-09-25 (see [06-backlog.md](06-backlog.md)), so only the drag-handle-vs-tap conflict remains | Low | Tested on M5/M6 (now folded into the Monthly budget list) - a mid-drag reorder without a stable row key cancelled the drag gesture outright; fixed by keying each row on its category id | 3 |

## Top risks at a glance

In the order they should be tested:

1. **R1** Play Protect blocking the install
2. **R2** Restricted settings blocking SMS permission
3. **R8** Receiving SMS when the app is closed
4. **R9** Android 17 behaviour for SMS and notifications
5. **R3** Notification permission
6. **R4** Signing key and updates
7. **R12** Parsing many banks' SMS formats

R1 to R9 are all covered by Phase 1. R12 is the largest risk to the product itself and is the focus of Phase 2.

## The four phases

### Phase 1: Prove the plumbing

**Goal:** show that a native Android app can be installed on your Pixel, get SMS permission, catch a bank SMS, and show a notification. This is a throwaway test app, not the real app.

**What we build**

- A small native test app (Kotlin and Jetpack Compose) running on your phone, with a step-by-step guide for Android Studio. The guide is [10-phase-1-guide.md](10-phase-1-guide.md) and the code is in the `phase1-test-app` folder.
- One test screen showing the status of SMS permission, notification permission, and the restricted-settings step.
- A live list of incoming SMS (raw text and time) and a list of the latest messages already in your SMS inbox.
- A simple check for spend-like messages (words such as "debited", "spent", "Rs", or "₹"). When one arrives, the app shows a notification such as "1 new spend detected". Tapping it opens the app.
- A release signing key, created and backed up.

**Tests to run and record**

| Test | What we learn |
|---|---|
| Run from Android Studio over USB | Whether the basic install works (R5) |
| Install a release APK over USB with ADB | Whether Play Protect blocks it (R1) |
| Copy the APK to the phone and install from the file manager or a download | Whether Play Protect blocks that route (R1) |
| Grant SMS permission without and with "Allow restricted settings" | What the user sees and what the app needs to explain (R2) |
| Deny notification permission, then allow it later | How the permission flow behaves (R3) |
| Receive a real bank SMS with the app open, in the background, swiped away, and after a reboot | Whether background reading works (R8) |
| Turn the screen off for an hour and turn battery saver on | Whether notifications are delayed (R18) |
| Send test SMS from the emulator and from another phone | Whether we can test quickly (R17) |
| Install an update over the existing app with the same key | Whether updates keep data (R4) |
| Force stop the app in Settings, then send an SMS | Confirms that a force-stopped app stays silent until it is opened again (R8) |

**Done when**

- The app installs on your Pixel in at least one way that you are happy to use every time.
- SMS permission can be granted, and the app explains the restricted-settings step.
- A real bank SMS produces a notification with the app open and with it closed.
- We know how Android 17 treats SMS and notifications for the test app (R9).
- The results are written into [11-phase-1-results.md](11-phase-1-results.md).

**Not in Phase 1:** parsing amounts, categories, budgets, or any of the real screens.

### Phase 2: Messages to transactions

**Goal:** turn an SMS into a reviewed transaction, end to end. Built as milestones M0 to M4 in [14-implementation-plan.md](14-implementation-plan.md).

**What we build**

- The local database, with a versioned schema (R23).
- SMS parsing for your banks, with a test collection of real messages (R12, R13, R15).
- The Messages page with white, green, and red statuses, filter chips, the red circle, and swipe actions (R28).
- The Add transaction page, pre-filled from a message, with a keyword-based category suggestion. Categories are a fixed starter set until Phase 3.
- The simple "new spends detected" notification that opens the Messages page (R19, R20).
- Catching up from the inbox on app open, and removing duplicates (R10, R11).

**Done when:** most spend messages from your banks are parsed correctly. We agree the target with real data, for example 9 out of 10, and everything else falls back to the raw text.

### Phase 3: Budget core

**Goal:** the app becomes a working monthly budget. Built as milestones M5 to M9 in [14-implementation-plan.md](14-implementation-plan.md). You can start using it daily after M9.

**What we build**

- The Monthly budget page: every category listed, ₹0 where nothing is set, tap a category to rename it, change its icon, or edit its amount, drag to reorder, and the + button for a new category (R27, R30). Category management lives here - there is no separate Categories page.
- Manual transactions, and editing and deleting them from the Category detail page.
- The Home page with remaining budget per category, negative amounts, and status colours.
- Budget alerts at 80% and when over budget (R21).
- Month logic and the copy-from-last-month step (R24).
- Settings with permissions, alert switches, and Backup and restore (R22).

**Done when:** you can run a full month's budget in the app, and back it up and restore it.

### Phase 4: Insights, polish, and hardening

**Goal:** finish the app and make it dependable for daily use. Built as milestones M10 to M12 in [14-implementation-plan.md](14-implementation-plan.md).

**What we build**

- The Trends page with This month, Previous month, and Historic tabs (R29).
- Duplicate detection for SMS, and any other items you choose from the backlog (R16).
- The trends range setting.
- First-run onboarding for permissions.
- A repeatable release and update routine, and a decision on developer verification (R6, R7).
- A week of real use, with fixes for what comes up.

**Done when:** you have used it for a week without needing to work around anything.

## What I need from you

- **Phase 1 results:** run the tests in the guide and record what happened in the results file. Paste any build errors from Android Studio so I can fix the code.
- **Sample SMS (before Phase 2):** 30 to 50 real bank spend messages with card digits and names removed, covering each bank you use.
- **Building the app:** I cannot build Android apps in my own environment, because its network blocks the Android build servers. You build the app in Android Studio, and I write the code and fix errors. USB installs from your laptop are also the safest route for R1, so Phase 1 is planned around that.

## Not verified

- Whether the Phase 1 code compiles first time. I could not build it, so the first build may show errors that I need to fix.
- Whether the India Play Protect block is active on your phone today. Phase 1 tests this directly.
- Whether the emulator's test SMS feature works with the background handler the same way a real SMS does. Real messages remain the final test.
- The launch date and rules for Google's free limited distribution account.

## Sources

- [Developer Guidance for Google Play Protect Warnings (Google)](https://developers.google.com/android/play-protect/warning-dev-guidance)
- [Google pilots blocking some sideloaded apps in India (TechCrunch)](https://techcrunch.com/2024/10/03/google-pilots-blocking-some-sideloaded-apps-in-india)
- [How to allow SMS permission on Android 15 and 16 (textbee.dev)](https://textbee.dev/blog/android-15-send-sms-permission-guide)
- [Android developer verification: rolling out to all developers (Android Developers Blog)](https://android-developers.googleblog.com/2026/03/android-developer-verification-rolling-out-to-all-developers.html)
- [Android's sideloading changes are getting closer (Android Authority)](https://www.androidauthority.com/android-sideloading-changes-timeline-3679204/)
- [another_telephony package (pub.dev)](https://pub.dev/packages/another_telephony)
- [Telephony plugin repository, archived (GitHub)](https://github.com/shounakmulay/Telephony)
