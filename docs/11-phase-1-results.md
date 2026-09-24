# Phase 1 Results

Fill this in while you follow [10-phase-1-guide.md](10-phase-1-guide.md). Write what you saw, even for things that worked. Short notes are fine.

**Date tested:**
**Phone:** Pixel 7a, Android 17 (build: )
**Laptop and Android Studio version:**

## Build and install

| Test | Risk | Result (worked / blocked / warning) | What exactly happened |
|---|---|---|---|
| Project builds without errors | R5 | | |
| Run from Android Studio over USB | R5 | | |
| Release APK installed with `adb install -r` | R1 | Worked | No Play Protect warning shown. First attempt failed with `INSTALL_FAILED_UPDATE_INCOMPATIBLE` because a debug-signed build (from Part D's Run button) was already on the phone; uninstalling the debug build then installing the release APK fresh succeeded cleanly |
| APK copied to phone and installed from file manager or browser | R1 | Blocked | Downloaded the APK via browser into the Downloads folder, then tried to open and install it. Installation was blocked outright, with no "install anyway" override offered |
| Play Protect warning shown? (text of warning) | R1 | Blocked | Exact wording not captured (no screenshot taken). General description: install was refused outright from the Downloads-folder route, consistent with the India Play Protect sideloading pilot |
| Update installed over the old app with the same key | R4 | Worked | `adb install -r` over the existing app (no uninstall) succeeded and applied new code (title text changed v3 to v4) |
| Data kept after the update? | R4 | Worked (on clean retest) | First attempt (v2 to v3) appeared to lose one logged test message; cause unconfirmed (possibly an accidental "Clear this list" tap, or the message was never saved to that install). Retested cleanly (v3 to v4): sent one test SMS, confirmed it was in the list, updated with `adb install -r`, and the message was still there afterwards. Treat in-place updates as preserving data, but the first ambiguous result is worth remembering |
| Signing key created and backed up | R4 | Warning | First keystore's password was lost before it could be reused (never backed up), making that key permanently unusable. A second keystore was created; **confirm its `.jks` file and password are now saved somewhere durable** before relying on it further |

## Permissions

| Test | Risk | Result | What exactly happened |
|---|---|---|---|
| SMS permission dialog appeared straight away? | R2 | | |
| "Allow restricted settings" was needed? Where did you find it? | R2 | | |
| SMS permission granted after that step | R2 | | |
| Notification permission dialog appeared | R3 | | |
| Notification denied, then allowed later | R3 | | |

## Receiving SMS

For each row: did the message appear in the list, did a notification show, and how long was the delay?

| # | Situation | In list? | Notification? | Delay (s) | Notes |
|---|---|---|---|---|---|
| 1 | App open | | | | |
| 2 | App in background | | | | |
| 3 | App swiped away | Yes | First attempt: no (Bedtime/DND was active on the phone, silently suppressed it - see note below). Retest with DND off: yes | Not timed | **First attempt is a real finding, not a bug**: the receiver caught and logged the SMS correctly with the app fully closed - this is R8, the most important risk in the project, confirmed working. The notification specifically was swallowed by the phone's Bedtime automatic DND rule ("while charging after 11pm"), because the app's notification channel doesn't have "Allow to bypass Do Not Disturb" enabled - Android requires the user to grant that per channel, apps can't self-grant it. With DND off, the notification appeared normally. Real-app implication: consider prompting the user to enable DND-bypass for the alert channel during onboarding, since a night-time spend would otherwise go silently unnoticed |
| 4 | After reboot, app not opened | | | | |
| 5 | Screen off one hour, Battery Saver on | | | | |
| 6 | After Force stop (expect nothing) | | | | |
| 6b | After opening the app once again | | | | |
| 7 | Notifications turned off, then on | | | | |
| 8 | Two spends close together ("2 new spends") | | | | |
| 9 | OTP or other non-spend message (no notification) | | | | |
| 10 | Second SIM (if used) | | | | |
| 11 | A real bank SMS | Yes | Yes | Not timed | Tested against the **real app** (BudgetManager, M2a build), not the BudgetSpike spike - a genuine ICICI Bank debit SMS (₹223.25, UPI) arrived naturally and was caught with no test string involved. Verified directly in the database (not just on screen): correctly classified as spend-like, stored with status=NOT_ASSIGNED, is_new=1, a proper dedupe_key, and the "spend_alerts" notification channel showed an active, unseen notification. The 9 starter categories were also confirmed seeded on first real use. Real message content was pulled briefly to verify, then deleted immediately per the privacy rules in 13-development-best-practices.md |

## Overall

**Install route I will use every time:**

**Anything surprising or annoying:**

**Android 17 differences noticed (R9):**

**Phase 1 done?** (see the "Done when" list in [09-risks-and-phases.md](09-risks-and-phases.md))
