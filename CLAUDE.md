# Gymable — Architecture & Design Reference

## Project Overview

Android fitness tracking app (Kotlin, Jetpack Compose, Hilt, Room). Users log workouts per exercise (any number of sets, with optional per-side reps/weight for unilateral exercises), track personal records, view stats charts, and browse workout history day-by-day. Also includes a first-launch guided tutorial, PDF+JSON data export/import, one-time IAP donations that unlock premium accent colors, and an in-app Play Store review prompt.

Min SDK: 24. Package: `com.gymable`.

---

## Package Structure

```
com.gymable
├── data/
│   ├── local/
│   │   ├── entity/          Room entities (no logic)
│   │   ├── dao/             DAOs — queries only, no business logic
│   │   └── GymDatabase.kt   Single RoomDatabase, version = 7
│   ├── repository/          Business logic; injected into ViewModels
│   └── DataSeeder.kt        One-time dev seed, gated by BuildConfig.SEED_DATABASE
├── di/
│   ├── DatabaseModule.kt    Hilt @Module providing all DAOs + DB
│   └── AppModule.kt         @ApplicationScope CoroutineScope (billing/review callbacks)
├── ui/
│   ├── components/          Reusable Compose components (incl. TutorialOverlay)
│   ├── navigation/          AppNavigation.kt — NavHost + Routes
│   ├── screens/             One file per screen composable
│   ├── theme/               Color.kt, Type.kt, Shape.kt, Theme.kt, AccentOption.kt, ThemeViewModel.kt, UnitSystem.kt
│   ├── donation/            DonationViewModel
│   ├── edit/                EditDataViewModel
│   ├── exercise/            AddExerciseViewModel
│   ├── history/             WorkoutHistoryViewModel
│   ├── main/                MainViewModel (dashboard + gym/muscle-group/exercise selection)
│   ├── overview/            OverviewViewModel
│   ├── settings/            SettingsViewModel
│   ├── stats/                StatsViewModel
│   ├── tutorial/             TutorialViewModel
│   └── workout/               WorkoutViewModel
├── GymApplication.kt        @HiltAndroidApp, seeds data on start (debug builds only)
└── MainActivity.kt          installSplashScreen() + setContent
```

---

## Data Model (Room, version = 7)

```
gyms             id, name
muscle_groups    id, name, gymId  (FK → gyms.id CASCADE)
exercises        id, name, muscleGroupId (FK → muscle_groups.id CASCADE), isUnilateral (default false)
workout_sessions id, exerciseId (FK → exercises.id CASCADE), date(ms, default now), rating (1-10, default 5)
workout_sets     id, sessionId (FK → workout_sessions.id CASCADE), setNumber, reps, weight, weightRight?, repsRight?
records          id, exerciseId (FK → exercises.id CASCADE, UNIQUE), maxWeight, maxReps  (one row per exercise)
```

Sets live in their own `workout_sets` child table (one row per set, not fixed columns) — this supports any number of sets per exercise, plus nullable `weightRight`/`repsRight` for unilateral (single-limb) exercises. `rating` is per-session (1–10, default 5), set on all sessions from the same workout at confirm time via `WorkoutRepository.updateSessionRatings(ids, rating)`.

Schema is exported to `app/schemas/` (Room Gradle plugin). Schema versions 1–6 predate migrations and are still wiped on upgrade (`PRE_MIGRATION_SCHEMA_VERSIONS`); from version 7 onward every upgrade must have a `Migration` in `data/local/Migrations.kt` — there is no blanket destructive fallback anymore, a missing path crashes instead of deleting user data. See the checklist in `Migrations.kt`; `MigrationChainTest` (unit) and `MigrationTest` (androidTest) guard it.

**View-only DAOs** (no entity, no migration needed): `ExerciseOverviewDao`, `WorkoutHistoryDao`, `DashboardDao`, `ExportDao` (all-suspend, read-only tree walk used for export).

Never add a view-only DAO to the `@Database` entities list. Never bump the schema version for a view-only DAO.

---

## Navigation

File: `ui/navigation/AppNavigation.kt`

| Route constant             | Screen               | Notes |
|-----------------------------|-----------------------|-------|
| `SPLASH`                    | SplashScreen          | No back stack entry after nav |
| `MAIN`                      | MainScreen            | Root destination |
| `NEW_WORKOUT`                | NewWorkoutScreen      | Shares `workoutViewModel` |
| `WORKOUT_SUMMARY`            | WorkoutSummaryScreen  | Shares `workoutViewModel` from NEW_WORKOUT |
| `ADD_EXERCISE`               | AddExerciseScreen     | Pops to MAIN on close |
| `ADD_EXERCISE_FROM_WORKOUT`  | AddExerciseScreen     | Plain pop — returns to NEW_WORKOUT |
| `EXERCISES`                  | ExercisesScreen       | Pops to MAIN on back |
| `STATS`                      | StatsScreen           | Pops with popBackStack() |
| `HISTORY`                    | WorkoutHistoryScreen  | Pops with popBackStack() |
| `SETTINGS`                   | SettingsScreen        | Pops with popBackStack() |
| `COLOR_SETTINGS`             | ColorSettingsScreen   | Pops with popBackStack() |
| `EDIT_DATA`                  | EditDataScreen        | Pops with popBackStack() |
| `DONATION`                   | DonationScreen        | Pops with popBackStack() |
| `CONTACT`                    | ContactScreen         | Pops with popBackStack() |

Transitions: `slideInHorizontally + fadeIn` (280ms) for push; mirrored for pop, set NavHost-wide. Splash→Main uses no enter animation so the system splash dissolves directly into the app.

**WorkoutViewModel sharing**: `AppNavigation(themeViewModel)` hoists a single `WorkoutViewModel` via `hiltViewModel()` at the top of the composable (Activity-scoped, not back-stack-entry-scoped) and threads it explicitly as a parameter into `NewWorkoutScreen` and `WorkoutSummaryScreen`. There is no `getBackStackEntry`-based scoping in this codebase — the shared instance is passed down directly. `resetWorkout()` is called explicitly to clear its state between workouts rather than relying on ViewModel recreation.

`WORKOUT_SUMMARY`'s `onConfirm` reads `workoutViewModel.uiState.value.showDonationNudge`, calls `resetWorkout()`, then either navigates to `DONATION` (`popUpTo(MAIN){inclusive=false}`) or pops straight back to `MAIN`.

---

## Screen Inventory

### SplashScreen
Pure Compose animation (logo reveal, scanner sweep, expanding rings, fade-out, ~1.4s) using `Animatable`s on a `Canvas`. Calls `onReady()` which navigates to MAIN. No ViewModel.

### MainScreen
App home: header with settings icon + donor badge (Supporter/Gym Rat/Legend based on donation tier), Start/Continue Workout card (last session or in-progress state), Stats/History/Exercises/Add-Exercise nav cards, Personal Records (top 5 by weight), a "This Week" weekly-activity widget (day dots + session count/volume), a Support-the-Developer link, and the first-launch `TutorialOverlay`. Uses `MainViewModel` + `TutorialViewModel`. `MainViewModel` doubles as the gym/muscle-group/exercise selector consumed by `NewWorkoutScreen`.

### NewWorkoutScreen
Active-workout logging flow: cascading Gym → Muscle Group → Exercise dropdowns, dynamic set-count picker, per-set reps/weight inputs (L/R rows for unilateral exercises), record display, draft auto-save on dispose. Uses the shared `WorkoutViewModel` plus a `MainViewModel` instance for selection. On "End Workout" navigates to WORKOUT_SUMMARY (does not pop).

### WorkoutSummaryScreen
End-of-workout recap: per-exercise set cards, total volume, 1–10 "how was it" rating picker. Receives the shared `WorkoutViewModel` as an explicit parameter — never calls `hiltViewModel()` itself. "Confirm" applies the rating, then either launches the in-app review flow (see below) or navigates per `onConfirm` in `AppNavigation.kt`.

### AddExerciseScreen
3-step wizard (Gym → Muscle Group → Exercise name + Bilateral/Unilateral toggle) to create a new exercise, with select-existing-or-add-new per step. Uses `AddExerciseViewModel`.

### ExercisesScreen
"My Exercises" — read-only list grouped by gym/muscle group, showing last-workout date, last set count/top weight, and PR badge per exercise. Uses `OverviewViewModel`.

### StatsScreen
Gym → Muscle Group → Exercise cascading selectors. Weight/Reps/Volume mode toggle (segmented, animated color). Month/Week/Custom period selector with prev/next navigation and a custom date-range calendar dialog. Custom-drawn Canvas line/area chart (see Stats Chart below). Uses `StatsViewModel`.

### WorkoutHistoryScreen
Day-by-day (not month-based) history browser: `DaySelector` with prev/next arrows, label format `"EEE, d MMM yyyy"`. Above each day's session cards, a heads-up row shows that day's distinct muscle groups (e.g. "Back & Triceps") alongside the day's `RatingBadge`. Session cards show per-set detail and total volume; long-press a session to delete (confirmation dialog). Uses `WorkoutHistoryViewModel`.

### SettingsScreen
Settings hub: links to Color settings, Edit Exercises, Support the Developer, Contact & Feedback; unit system (kg/lbs) toggle; Export Data (date-range dialog → PDF+backup) and Import Data (SAF file picker → destructive-overwrite confirmation) rows; app version from `BuildConfig`. Uses `SettingsViewModel`.

### ColorSettingsScreen
Accent-color picker grid (11 options). Colors locked behind an undonated tier show a lock icon and dimmed swatch; tapping one navigates to Donation instead of applying it. Takes `themeViewModel` as a parameter, no own ViewModel.

### EditDataScreen
Expandable Gym → MuscleGroup → Exercise tree with per-row rename and delete (confirmation dialog warns about cascade delete). Uses `EditDataViewModel`.

### DonationScreen
Three one-time IAP tiers (One Scoop / Gym Chalk / Recovery Shake) as cards with live Play Billing price, "already owned" state, success/error dialogs. Uses `DonationViewModel`.

### ContactScreen
Static developer bio card + name/message form; submitting opens an `ACTION_SENDTO` mailto intent. No ViewModel.

---

## Design System

### Colors (`ui/theme/Color.kt`)

Dark-only theme. Key colors:

| Token           | Hex         | Usage |
|-----------------|-------------|-------|
| Background      | `#0E0F11`   | Screen background |
| Surface         | `#16181B`   | Cards, surfaces |
| SurfaceElevated | `#1E2125`   | Elevated cards |
| SurfaceHigh     | `#272B30`   | Highest elevation |
| Outline         | `#34383E`   | Dividers, borders |
| Accent          | `#3DDC97`   | Electric mint-green — CTAs, chart line, headers (default; user-selectable, see AccentOption) |
| AccentPressed   | `#2FB87E`   | Pressed state of Accent |
| OnAccent        | `#06160F`   | Text on Accent backgrounds |
| AccentContainer | `#123022`   | Subtle accent backgrounds |
| TextPrimary     | `#F2F3F5`   | Main text |
| TextSecondary   | `#A8ADB5`   | Secondary / hint text |
| TextDisabled    | `#5C616A`   | Disabled text |
| ErrorRed        | `#FF6B6B`   | Error states |
| RecordGold      | `#FFC857`   | New personal record indicator |

The Accent family is swappable at runtime via `AccentOption` (see Donations & Premium Colors below) — treat the table above as the default (`MINT_GREEN`), not the only possible value.

### Typography
`displayMedium` — app title. `titleLarge` / `titleMedium` — screen titles. `bodyMedium` / `bodySmall` — content. `labelSmall` — metadata.

### Components (`ui/components/`)
- `Buttons.kt` — `PrimaryButton` / `SecondaryButton`, accept `text`, `onClick`, `icon`, `large`, `modifier`
- `AppDropdown` — labelled dropdown for gym/muscle group/exercise selection
- `AppTextField` — styled text input
- `InfoCard` — labeled value display
- `ScreenScaffold` — top bar (title + optional back arrow), content slot with horizontal padding
- `SectionLabel`, `SelectableListItem`, `StepProgress`
- `KeepScreenOn` — keeps the screen awake (used during active workout logging)
- `TutorialOverlay` — spotlight-cutout scrim + tooltip for the first-launch walkthrough

---

## State Management

- All UI state in `StateFlow` inside `@HiltViewModel` classes
- Every `stateIn()` uses `SharingStarted.WhileSubscribed(5_000)`
- Cascading selection (Stats, NewWorkout): `combine()` + `flatMapLatest`
- Side effects (saved, newRecord, error) are consumed via `consumeXxx()` functions that clear the flag
- `collectAsStateWithLifecycle()` in all Composables (not `collectAsState()`)

---

## WorkoutViewModel Invariant

`sessionLog` in `WorkoutUiState` accumulates entries across exercise changes within one workout session. `load(exerciseId)` MUST preserve the existing log:

```kotlin
fun load(exerciseId: Long) {
    val existingLog = _uiState.value.sessionLog  // capture before reset
    _uiState.value = WorkoutUiState(loading = true, sessionLog = existingLog)
    viewModelScope.launch {
        // ... fetch data ...
        _uiState.value = WorkoutUiState(..., sessionLog = existingLog)
    }
}
```

Never reset sessionLog to `emptyList()` inside `load()`.

`applyRating(rating)` is the single "workout confirmed" event: it writes the rating to every session in the log, then decides — in order — whether to fire the donation nudge and/or the review request (see their respective sections below). These two asks are mutually exclusive within the same confirm to avoid stacking prompts on the user.

---

## Stats Chart

`StatsChart` / `ChartCanvas` in `StatsScreen.kt` — pure Canvas, no external charting library.

- Padding: left=52dp (Y labels), right=8dp, top=16dp, bottom=28dp
- 4 horizontal grid lines with Y-axis labels via `rememberTextMeasurer()` + `drawText(textLayoutResult, topLeft)`
- Gradient fill under the line (Accent → transparent)
- Line: Accent, stroke 2dp, `StrokeCap.Round`
- Regular dots: SurfaceHigh fill + Accent stroke 1.5f
- Last dot: 6dp Accent + 3dp Background hole (highlights most recent point)
- X labels: max 5 evenly spaced + always the last point

Do not replace this with a third-party chart library without removing this implementation first.

---

## Startup / Splash

- AndroidX SplashScreen API (`androidx.core:core-splashscreen:1.0.1`)
- `MainActivity.installSplashScreen()` called before `enableEdgeToEdge()` and `super.onCreate()`
- Theme: `Theme.Gymable.Starting` (parent `Theme.SplashScreen`) → `postSplashScreenTheme = Theme.Gymable`
- `Theme.Gymable.Starting` set as `android:theme` on MainActivity in AndroidManifest
- System splash covers the cold-start black screen; Compose SplashScreen composable runs afterward

---

## Test Data (DataSeeder)

`DataSeeder.kt` is injected into `GymApplication` and seeds only when **both** conditions hold: `BuildConfig.SEED_DATABASE` is true (debug build type only — `beta`/`release` set it false) **and** the database is empty (`gymDao.count() > 0` short-circuits). Safe to leave wired permanently: it will never touch a release install or re-seed an already-populated debug install.

Gyms seeded:
- **Iron Paradise**: Chest (Bench Press, Incline Dumbbell Press, Cable Fly), Back (Deadlift, Pull-ups, Seated Row), Legs (Squat, Leg Press, Romanian Deadlift), Shoulders (Overhead Press, Lateral Raise), Arms (Barbell Curl, Tricep Pushdown)
- **Planet Fitness**: Push (Dumbbell Bench Press, Chest Fly Machine), Pull (Lat Pulldown, Cable Row), Legs (Leg Press, Hack Squat)

~190 workout sessions with linear weight progression over the last 90 days.

---

## Key Rules

1. **One DAO per concern** — do not add queries to the wrong DAO. View-only projections go in their own DAO file (e.g., `WorkoutHistoryDao`, `ExerciseOverviewDao`, `DashboardDao`, `ExportDao`).
2. **No schema version bumps for view-only DAOs** — they join existing tables and don't touch `@Database(entities=[...])`.
3. **No duplicate routes** — all routes are constants in `Routes` object in `AppNavigation.kt`.
4. **No duplicate ViewModel state** — check existing `UiState` data classes before adding fields.
5. **No comments unless the WHY is non-obvious** — well-named identifiers carry the meaning.
6. **No external chart/graph library** — custom Canvas chart is already in place.
7. **ScreenScaffold for all secondary screens** — provides consistent top bar and padding.
8. **`.values()` not `.entries`** — existing enum-iterating code (`StatsScreen.kt`'s `StatMode`/`PeriodMode`, `ColorSettingsScreen.kt`/`ThemeRepository.kt`'s `AccentOption`) consistently uses `.values()`. Match that for new code — this is a consistency convention, not a Kotlin-version constraint (the project runs Kotlin 2.2.10, which supports `.entries` fine).
9. **Never stack the donation nudge and the review request in the same workout confirm** — see the Invariants under each feature below.

---

## Implemented Feature: Donations & Premium Colors

One-time consumable Google Play Billing donations that unlock premium accent colors. Tier is tracked locally in `SharedPreferences` (lost on reinstall — accepted tradeoff).

**Files**: `data/repository/BillingRepository.kt`, `data/repository/PrefsRepository.kt` (donation tier + nudge-shown flag), `ui/donation/DonationViewModel.kt`, `ui/screens/DonationScreen.kt`, `ui/theme/AccentOption.kt`, `ui/screens/ColorSettingsScreen.kt`.

**Tiers** (product IDs `donation_ron_5`/`donation_ron_10`/`donation_ron_20`, cumulative):

| Tier | Label | Premium colors unlocked |
|---|---|---|
| 1 | One Scoop ☕ | 2 of 6 |
| 2 | Gym Chalk 🧴 | 4 of 6 |
| 3 | Recovery Shake 🏆 | 6 of 6 (all) |

`AccentOption` has 11 total: 5 free (`MINT_GREEN` default, `LAVENDER_PURPLE`, `GRAPEFRUIT_PINK`, `BLUE_BELL`, `STRAW_GOLD`) + 6 premium (`BLOOD_ORANGE`/`ARCTIC_BLUE` at tier 1, `HOT_PINK`/`EMERALD` at tier 2, `ROYAL_PURPLE`/`GOLDEN_AMBER` at tier 3).

**Invariants**:
- `BillingRepository.launchBillingFlow(activity, product)` takes `Activity` per call, never stores it (avoids leaks) — the established pattern any similar per-call system flow should follow (see Review Request below).
- `consumeAsync` also acknowledges automatically — never call `acknowledgePurchase` separately.
- `PrefsRepository.maxDonationTier` only increases, never decreases.
- Locked colors (`requiredTier > maxDonationTier`) can't be selected; `ColorSettingsScreen` shows them dimmed with a lock icon and routes taps to `DONATION` instead of applying them. If the current accent somehow becomes locked (theoretical reinstall edge case), fall back to `MINT_GREEN`.
- Billing-unavailable (emulator, no Play Store) degrades gracefully — no crash.
- A one-time nudge fires from `WorkoutViewModel.applyRating()` once `workoutSessionDao.countAll() >= 10`, gated by `PrefsRepository.donationNudgeShown`, navigating WORKOUT_SUMMARY → DONATION. Skipped/deferred if it would coincide with a review request (see below).

---

## Implemented Feature: Contact & Feedback

`ui/screens/ContactScreen.kt`, reachable from `SettingsScreen` → "Contact & Feedback". Static developer bio card plus a name/message form; submitting opens an `Intent.ACTION_SENDTO` mailto intent with the message pre-filled. No ViewModel — pure Compose state + Intent. No manifest permissions needed (`ACTION_SENDTO` doesn't require any).

---

## Implemented Feature: Play In-App Review Request

Native Play Store review prompt (no redirect out of the app), via `com.google.android.play:review-ktx:2.0.2`.

**Files**: `data/repository/ReviewRepository.kt` (wraps `ReviewManagerFactory` and the `com.google.android.play.core.ktx` suspend extensions `requestReview()`/`launchReview()`), `ui/workout/WorkoutViewModel.kt` (`applyRating()`, `launchReviewFlow()`), `ui/screens/WorkoutSummaryScreen.kt`.

**Trigger**: after the 5th confirmed workout — `PrefsRepository.incrementCompletedWorkouts()` reaching `REVIEW_REQUEST_WORKOUT_THRESHOLD` (= 5) — gated one-time-ever by `PrefsRepository.reviewRequested`. Skipped (deferred to a later workout, flag stays unset) if the donation nudge is firing in the same confirm, so the two never stack.

**Invariants**:
- `ReviewRepository.launchReviewFlow(activity)` takes `Activity` per call, same "never store it" pattern as `BillingRepository.launchBillingFlow`.
- Wrapped in try/catch inside `ReviewRepository`, silently no-ops on failure (no Play Store, quota exhausted) — a failed review request must never crash or surface an error to the user.
- `WorkoutSummaryScreen` launches it from `LaunchedEffect(uiState.showReviewRequest)`, separate from the `workoutFinished` navigation effect. The actual call runs in `viewModelScope` (inside `WorkoutViewModel`), so it survives the screen navigating away right afterward.

---

## Implemented Feature: Onboarding Tutorial

First-launch guided walkthrough, 6 steps, spotlight-style overlay.

**Files**: `ui/tutorial/TutorialViewModel.kt` (`TutorialState`: `Hidden` / `Step(index)` / `Final`, `TOTAL_STEPS = 6`), `ui/components/TutorialOverlay.kt`, `data/repository/OnboardingRepository.kt` (`SharedPreferences` `onboarding_done` flag).

Triggered from `MainScreen`, which tracks the screen-space bounds of 6 target UI elements (Start Workout card, Stats, History, Exercises, Add Exercise, Settings icon) via `Modifier.onGloballyPositioned` and renders `TutorialOverlay` as a full-screen scrim with a rounded-rect spotlight cutout (`BlendMode.Clear`) around the current target, plus a tooltip with step counter and Skip/Next controls. Completing the final step or tapping Skip both call `OnboardingRepository.markComplete()` — shows once per install until app data is cleared.

---

## Implemented Feature: Data Export / Import

**Files**: `data/local/dao/ExportDao.kt`, `data/repository/ExportRepository.kt` (PDF via `PdfExporter` + JSON backup via `GymDataExporter`), `data/repository/ImportRepository.kt` (`GymDataImporter`), triggered from `ui/screens/SettingsScreen.kt` / `ui/settings/SettingsViewModel.kt`.

- **Export**: date-range picker → writes a `.pdf` report (accent-themed) and a `.gymdata` JSON backup. On API 29+ (`Build.VERSION_CODES.Q`) both are written straight to `MediaStore.Downloads`, no permission needed; on older APIs they go to `cacheDir` and are shared via `FileProvider` + `Intent.ACTION_SEND_MULTIPLE`.
- **Import**: SAF `ActivityResultContracts.OpenDocument()` picks a `.gymdata` file → destructive-confirmation dialog → `ImportRepository.importFromJson()` runs `gymDao.deleteAll()` (cascades via FK `ON DELETE CASCADE`) then reinserts the entire tree (gyms → muscle groups → exercises → records → sessions → sets) from the parsed JSON.
- This is a full destructive **replace**, not a merge — keep the confirmation dialog copy saying so.
