# Steady

A lightweight personal training and nutrition tracker with a static web app and a native Android shell. No framework, analytics, ads, accounts or health-data backend.

## Features

- Three A/B/C workouts designed for approximately 45 minutes including warm-up. Chest and arms get priority, with biceps, triceps, forearms and optional gentle neck isometrics. Back and lower-body work remain in the plan.
- View & edit plan: preview each session, swap exercises, change sets/rep ranges, reorder, add and remove. Estimates flag how much time a session needs. Existing workouts retain a snapshot of their original exercises.
- Workout logging: kg/reps per set, previous completed results, autosaved drafts and rest timer. A/B/C follows completed sessions rather than calendar days. One workout per date.
- MyFitnessPal-style daily totals entered manually: calories, protein, carbohydrates and fats. No MyFitnessPal account integration.
- Daily weight, weekly waist, date-based charts, front/side/back photos and workout history.
- Homepage steps: Samsung Health through Health Connect in the Android app, with date/source/last-sync time and explicit permission/no-data/error states. Manual entry works in both versions.
- Editable three-day schedule and Calendar reminders, light/dark themes, complete backup/restore.

Default program: Mon/Wed/Fri, October 19, 2026 to January 31, 2027. First two weeks cap each exercise at two sets. Default later A/B/C cycle: 10 chest sets, 6 direct biceps sets and 6 direct triceps sets, plus forearm work, with pressing also training triceps. The cheat sheet is an exercise reference; its high set targets are not imposed on a returning beginner.

## Web development

Node.js 20+, no dependencies/install step.

```sh
npm start
npm test
npm run check
```

Open http://127.0.0.1:4173. Static production files are in `dist/`. Service worker caching supports offline reuse after first successful load; private hosting authentication may constrain offline browser launches.

## Android

`android/` is a Kotlin/AndroidX WebView app bundling exactly the same `dist/` screens via WebViewAssetLoader. Requires Android 9+ with Google Play services for Health Connect. Galaxy A16 is within the intended Android device range. Compile SDK 36, target 35, Java 17 and Gradle 8.11.1.

GitHub Actions builds an installable personal/debug APK on relevant pushes. The repository secret `STEADY_DEBUG_KEYSTORE` holds a stable signing key so later APK updates can retain app data. Do not replace the key or uninstall before exporting a backup. No keystores, APKs or personal runtime data are committed. This is a personal sideload build, not a Play Store release; Play publication would require the relevant health-app declarations and release process.

To build locally with Android SDK/Java 17/Gradle installed:

```sh
cd android
gradle assembleDebug
```

### Samsung Health setup

1. Samsung Health → Settings → Health Connect → App permissions: allow Samsung Health to write Steps. Open Samsung Health after granting sharing permission.
2. In Steady's Today screen, tap Connect / refresh and allow read-only Steps.
3. Today's steps refresh when opening/returning to the app; select a recent log date to query that date. Health Connect reads are limited to recent days without requesting extended-history access.
4. No-record, permission-denied and read-failure states are distinct from a genuine zero. Previously saved totals are labelled with their source and last-sync time.

The app reads `StepsRecord.COUNT_TOTAL` through the aggregate API filtered to Samsung Health (`com.sec.android.app.shealth`). It does not add Samsung counts to raw phone/watch totals. Step sync requests READ_STEPS. Optional MyFitnessPal import separately requests READ_NUTRITION. No health writes, location, heart rate, sleep or background-health permission is requested. Rest alerts request notification and precise-alarm access. Data can lag until Samsung Health shares it. Actual Samsung permission and sync behavior must be verified on the user's phone.

The native bridge only exists in bundled local pages. External links open in the user's browser; non-asset WebView network requests are blocked. Android file picker handles photo/backup selection, and Android's Save dialog handles JSON/ICS export. App automatic Android backup is disabled. No backend receives health records.

## Data and privacy

IndexedDB stores settings, plans, logs, measurements, steps, photos and workout snapshots locally. Android app and Chrome storage are separate; export and restore a JSON backup to move between them. Uninstalling/clearing app storage loses local records. Backups include personal data/photos and are not encrypted. Images are resized to at most 1400 pixels and re-encoded as JPEG. Backup schema validation rejects malformed data before replacement, and older backups retain their legacy workout exercise lists.

## Reminders

Google Calendar links create one repeating event per chosen weekday through the goal date, lasting 45 minutes. Users must save those events and choose notification settings. An ICS option includes 30-minute alarms, which calendar apps may override. Editing the Steady schedule does not update existing Calendar events: delete old repeating events and add the revised ones. No background browser notification service runs.

## Verification

14 core checks cover phase boundaries, missed days/drafts, previous performance, time-spaced charts, 45-minute calendar durations/midnight boundaries, nutrition/photo/workout backup round trips, malformed backups, legacy workout preservation, plan customization, default session time budgets and steps metadata restoration. Phone-sized browser QA covers nutrition logging, a full session, reload persistence, measurements, light/dark modes, plan editing and step entry. Native APK compilation and physical-device Health Connect verification are separate checks; a successful build does not prove Samsung has shared steps.

## Deployment

The source lives in the owner's private GitHub repository. `.openai/hosting.json` identifies its private Sites deployment. Only `dist/` and the deployment manifest are included in the static hosting archive.

The optional local name setting only changes the homepage hello. There are no app accounts, sign-up, passwords, profiles or account-management features. The private web host's owner sign-in is separate from the installed app, which needs no login.


Version 1.3 adds an insertion + below every exercise and an Add set action that duplicates the latest kg/reps into a new unchecked, editable set. There is no set-count limit in workout logging or backup restoration. These changes affect the current workout only. Native rest alerts use Android notification sound (subject to notification permission, channel settings, volume and Do Not Disturb). Allow precise alarms for timely alerts with the screen off; otherwise Android can delay background delivery. Stopping or restarting a timer cancels the preceding alert.

MyFitnessPal import reads only that app's NutritionRecords from Health Connect, prefills available calories/protein/carbs/fat for the selected day, and requires review/save. Missing values remain manual; no-data and permission failures do not overwrite the saved day. MyFitnessPal must be linked to Health Connect and have synced its meals. Step queries cover the complete local calendar day and explain whether Samsung records are absent or Health Connect contains steps from another source. Physical-phone sharing and notification delivery remain device checks.
