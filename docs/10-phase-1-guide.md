# Phase 1 Guide: Test SMS, Notifications, and Installing

This guide walks you through building the throwaway Phase 1 test app in Android Studio and running it on your Pixel 7a. It answers the top risks in [09-risks-and-phases.md](09-risks-and-phases.md): can the app be installed, can it get SMS permission, does it catch a bank SMS, and does it show a notification.

**Important:** I wrote this code without being able to compile it, because my environment cannot reach the Android build servers. The first build may show errors. If it does, copy the message from Android Studio's **Build** panel and paste it to me. Fixing them is normal and quick.

Record what you see in [11-phase-1-results.md](11-phase-1-results.md) as you go.

## Part A: Create the project

1. Open Android Studio and choose **New Project**.
2. Choose the **Empty Activity** template (the one with the Compose logo, not "Empty Views Activity").
3. Fill in:
   - **Name:** `BudgetSpike`
   - **Package name:** `com.example.budgetspike` (it must match exactly, because the code files use it)
   - **Save location:** a folder **outside OneDrive**, for example `C:\Dev\BudgetSpike`. OneDrive syncing build folders causes slow builds and odd errors.
   - **Language:** Kotlin
   - **Minimum SDK:** API 26
   - **Build configuration language:** Kotlin DSL (the default)
4. Click **Finish** and wait until the bottom bar stops showing progress (the first Gradle sync can take several minutes and downloads files).

## Part B: Add the code

The code is in the `phase1-test-app` folder next to this guide.

1. In the Project panel on the left, open `app > kotlin+java > com.example.budgetspike` (or `app/src/main/java/com/example/budgetspike`).
2. **Replace** the contents of `MainActivity.kt` with the contents of `phase1-test-app/MainActivity.kt`.
3. Add four new Kotlin files in the same folder (right-click the package, **New > Kotlin Class/File**, name it, choose **File**), and paste in the matching content:
   - `SmsReceiver.kt`
   - `SpendLog.kt`
   - `SpendDetector.kt`
   - `Notifier.kt`
4. Open `app/src/main/AndroidManifest.xml` and follow the comments in `phase1-test-app/AndroidManifest-additions.xml`. In short: add three `<uses-permission>` lines above `<application>`, and add the `<receiver>` block inside `<application>`.
5. Ignore the `ui/theme` folder the template created. The test app does not use it. If Android Studio complains about the theme, tell me.
6. Click **Build > Make Project**. If it succeeds, continue. If not, paste the errors to me.

## Part C: Prepare the Pixel

1. On the phone, open **Settings > About phone** and tap **Build number** seven times. It will ask for your lock screen PIN.
2. Go to **Settings > System > Developer options** and turn on **USB debugging**.
3. Connect the phone to the laptop with a USB cable that carries data (some charge-only cables do not).
4. On the phone, accept the **Allow USB debugging?** prompt for this computer. Tick "Always allow" if you trust the laptop.
5. In Android Studio, your Pixel 7a should appear in the device drop-down at the top. If it does not, try another cable or port, and set the USB mode on the phone to **File transfer**.

## Part D: Run and set up permissions (tests 1 and 2)

1. Press the green **Run** button. The app installs and opens on the phone.
2. Note whether any warning appeared, and write it in the results. (This is the "run from Android Studio over USB" test.)
3. On the test screen, tap **1. Ask for SMS permission**.
   - **If a normal permission dialog appears:** allow it.
   - **If nothing appears, or the permission is shown as blocked:** this is the "restricted settings" behaviour. Do this:
     1. Tap **Open App info**.
     2. Tap the three-dot menu at the top right.
     3. Tap **Allow restricted settings** and confirm with your PIN. If you do not see this option, tap **Permissions** on the App info page first and try the SMS permission there, then come back to the menu.
     4. Go back to the app and tap **1. Ask for SMS permission** again.
   - Write down exactly what you saw at each step. This tells us what the real app must explain.
4. Tap **2. Ask for notification permission** and allow it.
5. Tap **Show a test notification**, press the Home button, and check the notification shows "3 new spends detected". Tap it and confirm it opens the app.

## Part E: The SMS test matrix

Send yourself test messages. The easiest way is from another phone. Use text like this (the words "debited" and "Rs" are what the test looks for):

```
Rs 250.00 debited from a/c XX1234 on 24-09-26 at TEST STORE. Avl bal Rs 5000.
```

A real bank SMS is a better test. Do a small real transaction when you can.

For each situation below, send an SMS, then open the app and look at the **Caught by the receiver** list. Also note whether the notification appeared and how long it took.

| # | Situation | How |
|---|---|---|
| 1 | App open on screen | Keep the app in front and send an SMS |
| 2 | App in the background | Press Home, then send an SMS |
| 3 | App swiped away | Open the recent apps view, swipe the app away, then send an SMS |
| 4 | After a reboot | Restart the phone, do not open the app, then send an SMS |
| 5 | Screen off for an hour with Battery Saver on | Turn Battery Saver on, lock the phone, wait an hour, then send an SMS |
| 6 | After Force stop | In App info, tap **Force stop**, then send an SMS. **Expect nothing.** Open the app once and send another SMS to confirm it works again |
| 7 | Notification permission denied | In notification settings, turn notifications off, send a spend-like SMS, then turn them on. The message should still appear in the list |
| 8 | Two spends close together | Send two spend-like SMS within a minute. The notification should say "2 new spends detected" |
| 9 | A message that is not a spend | Send "Your OTP is 123456". It should appear as **Not spend-like** with no notification |

Also try sending a message from the second SIM, if you use one, and from each bank if you can.

The **delay** number on each entry shows how long Android took to deliver the message to the app. A few seconds is normal.

## Part F: Install tests (R1 and R4)

These show how the real app will be installed and updated. Do them after Part E.

### F1. Build a release APK

1. In Android Studio, choose **Build > Generate Signed App Bundle / APK**.
2. Choose **APK** and click **Next**.
3. Under **Key store path**, click **Create new**. Save the key file in a safe folder outside OneDrive, and choose a password. **Back up this file and the password somewhere safe.** Every future update of the real app must be signed with the same key.
4. Choose the **release** build type and finish. Android Studio tells you where the APK is (usually `app/release/app-release.apk`).

### F2. Install by USB with ADB

1. Open a terminal in the Android SDK's `platform-tools` folder (Android Studio's **Tools > SDK Manager** shows the SDK location; `adb.exe` is in `platform-tools`).
2. Run `adb devices` and check that your phone is listed.
3. Run `adb install -r path\to\app-release.apk`.
4. Note whether Play Protect showed a warning, and whether SMS permission still works.

### F3. Install by copying the file

1. Uninstall the test app from the phone.
2. Copy `app-release.apk` to the phone (USB file transfer, or through a browser, chat app, or Drive).
3. Open the file on the phone and install it.
4. Note exactly what warnings or blocks appear. This is the route Play Protect is most likely to block (R1). If it is blocked, that is a useful result, not a failure. USB then becomes our install route.

### F4. Update with the same key

1. Change one visible line of text in `MainActivity.kt` (for example, the title "Budget Phase 1 test" to "Budget Phase 1 test v2").
2. Build a new signed release APK with the **same key**.
3. Install it over the existing app with `adb install -r`.
4. Check that the app updated without asking you to uninstall, and that the log list is still there.

## Part G: When you are done

1. Fill in [11-phase-1-results.md](11-phase-1-results.md).
2. Tell me what happened, especially anything unexpected, such as a warning, a missing message, or a long delay.
3. Keep the signing key and its password safe. Do not delete it.

## If something goes wrong

| Problem | Try this |
|---|---|
| Gradle sync fails or hangs | Check the internet connection, then **File > Sync Project with Gradle Files**. Paste the error to me |
| Red errors in the code | Paste the first error from the **Build** panel to me. The first error is usually the cause of the rest |
| The phone does not appear in Android Studio | Try another cable, set USB mode to File transfer, and re-accept the debugging prompt |
| SMS permission dialog does nothing | Follow the restricted-settings steps in Part D |
| SMS arrives but nothing shows in the list | Check SMS permission is granted, and that the receiver was added to the manifest. Send me what you see |
| Notification does not appear | Check notification permission and the app's notification settings. The message may still be in the list |
| Play Protect warning during install | Take a screenshot or copy the text, and choose the option to install anyway only if the app is yours (it is) |
