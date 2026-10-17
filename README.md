# Steady

A lightweight Android-friendly training and nutrition tracker. No framework, runtime dependencies, accounts, analytics, or external fonts. Installable as a PWA when supported by the browser and hosting environment.

## Use

- Today: workout status, weekly schedule, nutrition summary and weight trend.
- Workout: chest/biceps-first A/B/C sessions, per-set kg/reps, previous completed performance, draft autosave and rest timer. Sessions advance by completed workout dates; skipped calendar days do not advance the plan. One workout per date.
- Nutrition: copy calorie, protein, carbohydrate and fat totals manually from MyFitnessPal. No MyFitnessPal integration or credentials required.
- Progress: daily weight, weekly waist, date-based trend charts, front/side/back photos and workout history. Dates can be changed to edit older logs.
- Settings: exactly three editable gym days, workout time, dates, targets, home-screen installation and full backup/restore.
- Theme toggle: dark/light, saved on device.

Defaults: Mon/Wed/Fri at 18:00, October 19, 2026 to January 31, 2027. Food guides are starting estimates, not strict limits. The training plan eases in for two weeks, then provides 12 chest and 10 direct biceps sets per A/B/C cycle, plus back, legs, glutes, shoulders and triceps.

## Local development

Requires Node.js 20+; no installation step.

```sh
npm start
npm test
npm run check
```

Open http://127.0.0.1:4173. The static production app is `dist/`. The service worker caches the app shell for offline reuse after a successful first load. Service worker installation and offline behavior can be constrained by private hosting authentication.

## Storage and privacy

All logs, settings and photos live in IndexedDB in the visitor's browser. There is no application backend and no cloud synchronization. Data is not in this repository. Changing device/browser/profile or clearing site storage can remove access. Export JSON backups regularly. Backups contain personal data and photos and are not encrypted. Images are resized to at most 1400 pixels and re-encoded as JPEG before local storage. Restore validates the backup schema before replacing data.

## Reminders

Android reminders use Google Calendar rather than unreliable browser timers. Each selected weekday has an Add to Calendar link with weekly recurrence ending on the goal date. Users must save each event and choose their Calendar notification settings. An ICS alternative includes 30-minute alarms; calendar apps may override or ignore imported alarms. Changing app schedule does not modify previously created Calendar events; users must remove old repeating events and add the new ones. The rest timer is a foreground convenience; no push notification service runs.

## Validation

Core tests cover phase boundaries, skipped days/drafts, previous performance, date-spaced charts, calendar end dates/midnight, full backup round trips and malformed backup rejection. Browser QA at a phone-sized viewport covers daily nutrition, complete workout logging, reload persistence, measurements and both themes. Native Android installation/notification delivery must be verified on the device.

## Deployment

`.openai/hosting.json` identifies the private Sites deployment. `dist/` is ready for any static HTTPS host. The source is also mirrored to the owner's private GitHub repository. No personal runtime logs or photos are committed.
