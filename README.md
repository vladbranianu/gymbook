# GymBook

Offline-first Android workout tracker. Log exercises set by set, track personal records, and review progress over time. Published on Google Play (500+ installs, no marketing).

- **Language / UI:** Kotlin, Jetpack Compose (Material, dark theme, custom Canvas charts)
- **Architecture:** single-activity, MVVM, unidirectional state via `StateFlow`
- **DI / Persistence:** Hilt, Room (SQLite)
- **Other:** Navigation Compose, Play Billing (one-time donations), Play In-App Review, PDF + JSON export/import via MediaStore / SAF
- **Min SDK:** 24

## Features

- Log workouts per exercise with any number of sets; per-side reps/weight for unilateral exercises
- Gym → muscle group → exercise hierarchy, fully user-defined
- Personal records detection and display
- Stats screen with a custom-drawn Canvas chart (weight / reps / volume; week, month or custom range)
- Day-by-day workout history with per-session ratings
- Export a PDF report and a JSON backup; import a backup (destructive replace, with confirmation)
- kg / lbs toggle, selectable accent colors (some unlocked by optional one-time donations)
- First-launch spotlight tutorial

## Architecture

```
com.gymable
├── data/        Room entities + DAOs, repositories (business logic), DataSeeder (debug only)
├── di/          Hilt modules
└── ui/          Compose screens, reusable components, theme, one ViewModel per feature
```

Design decisions and invariants (state handling, ViewModel sharing, DAO rules, billing/review flows) are documented in [`CLAUDE.md`](CLAUDE.md).

Note: the Kotlin namespace is `com.gymable` (the project's original name) while the published `applicationId` is `com.vladbranianu.gymbook`. The applicationId is fixed by the Play Store listing; the source package was not renamed.

## Build

Requirements: Android Studio (recent stable) with JDK 17+, Android SDK.

`JAVA_HOME` must point to a full JDK, not a JRE — for example Android Studio's bundled one (`<Android Studio>/jbr`).

```bash
./gradlew assembleDebug
```

On Windows (cmd/PowerShell), use `gradlew.bat assembleDebug` instead.

Debug builds seed sample data on first launch (`BuildConfig.SEED_DATABASE`). Release builds are signed using a local, untracked `keystore.properties`:

```properties
storeFile=/path/to/keystore
storePassword=...
keyAlias=...
keyPassword=...
```

## Known limitations / roadmap

- No automated tests yet — unit tests for repositories and ViewModels are the next priority.
- Database schema is exported (`app/schemas/`) and upgrades from schema v7 onward must go through real Room migrations (see `Migrations.kt`); only installs older than v7 are still wiped on upgrade.
