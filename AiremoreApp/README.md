# Airemore Field App (Kotlin / Android)

Offline-capable Android app for staff doing PM, Repair/Checkup, and
Installation visits on-site. Built with Jetpack Compose + Room + WorkManager.

## How it works (short version)

1. Staff logs in once while online. The app downloads and caches the
   company list, AC brand/type/capacity options, checklists, and finding
   options (`api/lookups.php`) into a local Room database.
2. From then on, **every screen works with zero signal.** New PM/Repair/
   Installation forms are saved locally as soon as the staff starts typing
   (auto-save on every field change — nothing is lost if the app is killed
   or the phone dies).
3. Tapping **Submit** just flips the record's status to `PENDING` locally.
4. `SyncWorker` (WorkManager) is the queue: it wakes up the moment the OS
   reports a network connection, uploads every `PENDING` record (JSON +
   photos in one multipart request per record), and marks each one
   `SYNCED` (with the server's official Service Report No.) or `FAILED`
   (if the server rejected it — staff can reopen it from "Mga Records Ko"
   and fix it).
5. A 15-minute periodic sync runs as a safety net in case the live
   connectivity callback is missed (e.g. Doze mode).

## Building it

You need **Android Studio** (Koala/2024.1 or newer) — this can't be
compiled in a plain terminal without the Android SDK.

1. Open this `AiremoreApp/` folder in Android Studio ("Open" → select the
   folder, not a file).
2. Let Gradle sync (first sync downloads dependencies — needs internet).
3. **Set your server URL** in `app/build.gradle.kts`:
   ```kotlin
   buildConfigField("String", "API_BASE_URL", "\"https://your-domain.com/airemore_system/api/\"")
   ```
   Point this at wherever you deployed the `api/` add-on from the PHP
   project. Must end with a trailing slash. Must be `https://` — the app
   blocks cleartext (`http://`) traffic by default for security
   (`android:usesCleartextTraffic="false"` in the manifest); only relax
   that for local testing against a non-HTTPS dev server.
4. Run on a device/emulator (▶ button), or **Build > Generate Signed Bundle
   / APK** to produce an installable APK for staff phones.

## What's implemented

- Login / logout (token-based, persisted with DataStore)
- Offline cache of companies + brand/type/capacity/checklist/finding lists
- PM form: company, personnel (up to 10), repeatable AC units with 12
  before/after readings + 17-item checklist + up to 2 before/after photos
  each, findings + A.F.I./Recommendation/Action Taken/A.L.I., next PM
  date, customer name/position/signature
- Repair/Checkup form: same shape minus checklist, plus location/serial
  number per unit
- Installation form: company, personnel, repeatable units (qty/type/
  brand/model/capacity), job details, optional site photos, customer
  signature
- Full offline queue + auto-sync (WorkManager) with per-record status:
  DRAFT → PENDING → SYNCING → SYNCED / FAILED
- Records list per module showing sync status, tap a FAILED record to
  fix and resubmit

## Deliberately not implemented (v1)

Matches the API add-on's scope — see `api/README_API.md` on the PHP side
for the full reasoning. In short: SM Store checklist variant, COA
(Certification of Accomplishment), Installation Start-Up Certificates,
and "particulars" rows are admin/rare-case features left on the web
dashboard. Editing a record **after** it has synced is also not in the
app — corrections happen on the web, same as today.

## Known things to double-check before rolling out to staff

- **App icon** is a placeholder vector (`app/src/main/res/drawable/
  ic_launcher_foreground.xml`) — swap in your real logo before
  distributing.
- **Table name casing**: the API add-on assumes `pm_*` (lowercase) and
  `Repair_*` / `Installation_*` (capitalized) table names, matching your
  current `*/save.php` files exactly. If your live DB differs, fix the
  API side first (see `api/README_API.md`) — the app itself doesn't
  care about table names, only the JSON contract.
- **Distribution**: since this isn't on the Play Store, staff install the
  APK directly ("sideloading") — they'll need to allow "install from
  unknown sources" once. For a nicer rollout later, consider Firebase
  App Distribution or a simple download link on your own domain.
- This project has **not been compiled** in this environment (no Android
  SDK access here) — Android Studio will likely surface a few small
  issues on first build (a missed import, a Compose API that shifted
  slightly between versions). Everything is written against
  Compose BOM 2024.06.00 / Kotlin 1.9.24 / AGP 8.5.2; if you're on very
  different tool versions, that's the first place to look.
