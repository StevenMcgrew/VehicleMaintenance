# Current Feature

**Feature 1: Vehicle list and details**

**Branch:** `feature/vehicle-list-and-details`

**Status:** verified

## Goal

Let the owner add, edit, and delete the vehicles they track, and see them in a
list on the home screen. This is also the feature that creates the app's single
JSON store in app-private storage, with the atomic write and `schemaVersion`
guarantees every later feature depends on.

## In scope

- The on-disk JSON store: root shape with `schemaVersion`, `vehicles`,
  `maintenanceItems`, and `serviceLogEntries`, read and written atomically.
- A `Vehicle` model with `id`, optional `nickname`, `year`, `make`, `model`,
  `engine`.
- A repository interface over persistence, plus its JSON-file implementation,
  covering list, add, update, and delete with cascade of the owned lists.
- Vehicle list screen as the app home, replacing the scaffold `Greeting`.
- Add/edit vehicle form with validation, inline errors, and accessible labels.
- Delete with a confirmation dialog.
- Loading, empty, loaded, and load-failure states on the list.
- Navigation between list and form, with correct system back behavior.
- The dependencies this needs: kotlinx.serialization, Navigation Compose,
  lifecycle viewmodel-compose, declared in the version catalog.

## Out of scope

- The vehicle detail screen with its maintenance item table and cost totals.
  Those arrive with features 2 and 8. In this feature, editing a vehicle is
  reached from the list.
- Maintenance items, service log entries, due status, notifications, cost
  totals, export/import. Their lists exist in the file from day one but stay
  empty and are never read or written by this feature except to preserve them
  across writes and to cascade on delete.
- Any unit setting, odometer capture, or photo support.
- Updating the stale persistence TODO in `blueprint/context/coding-standards.md`
  and the TODO in `AGENTS.md`. Both are flagged in the project overview's open
  questions and belong in a separate docs pass, not buried here.

## Build loop

`workflow.stepReview` is `feature` and `workflow.checkpointCommits` is
`disabled`. Work through every step below without stopping for per-step
approval and without checkpoint commits, then present one review packet covering
all steps. `/complete` creates the final feature commit and merges after
approval.

Run after every step, from the project root:

```
./gradlew testDebugUnitTest
```

Run before the review packet:

```
./gradlew assembleDebug && ./gradlew testDebugUnitTest && ./gradlew lintDebug
```

There is no configured `Verify` command yet, so those three are the gate.

## Verification at completion

Ran and passing: `./gradlew assembleDebug`, `./gradlew testDebugUnitTest`
(31 tests, 0 failures), `./gradlew lintDebug` (no warnings from feature files),
`./gradlew assembleDebugAndroidTest`.

Not observed: no device or emulator was available on the build machine, so
`./gradlew connectedDebugAndroidTest` never ran and the instrumented Compose
test of step 8 is compiled only. The on-device clauses in steps 6 and 7 were
likewise not observed. Those paths are covered by JVM tests at the validator,
repository, and store layers, but there is no on-device or persisted-data
evidence from a real run.

## Build steps

- [x] **1. Baseline and dependencies.** Confirm the untouched project builds
  (`./gradlew assembleDebug`, `./gradlew testDebugUnitTest`) before changing
  anything, and report the result. Then add to `gradle/libs.versions.toml`, and
  reference from `app/build.gradle.kts`: the
  `org.jetbrains.kotlin.plugin.serialization` plugin at `version.ref = kotlin`,
  `org.jetbrains.kotlinx:kotlinx-serialization-json`,
  `androidx.navigation:navigation-compose`, and
  `androidx.lifecycle:lifecycle-viewmodel-compose`. No version string may appear
  in a build file.
  - Check: pick versions that actually resolve. Start from
    `kotlinx-serialization-json` 1.9.x and `navigation-compose` 2.9.x; if
    resolution fails, move to the nearest stable version that builds and record
    what you used.
  - Check: give `lifecycle-viewmodel-compose` the existing
    `lifecycleRuntimeKtx` version ref. If Navigation pulls a newer lifecycle and
    Gradle reports a conflict or a lint warning, raise the single
    `lifecycleRuntimeKtx` version in the catalog so all lifecycle artifacts stay
    aligned, rather than pinning one artifact separately.
  - Check: the configuration cache is on. If a change breaks it, fix the build
    logic rather than disabling the cache.
  - **Done when:** `./gradlew assembleDebug` and `./gradlew testDebugUnitTest`
    both succeed with the new dependencies present and no version literal in
    `app/build.gradle.kts`.

- [x] **2. Store model and JSON contract.** Add `vehicles/Vehicle.kt` and
  `data/MaintenanceStore.kt` (the serializable root plus the shared `Json`
  instance configured as described under Data / contracts). No file I/O yet.
  Add JVM unit tests in `app/src/test/` for the encode/decode contract.
  - **Done when:** `./gradlew testDebugUnitTest` passes with tests proving:
    a round trip preserves every vehicle field; `schemaVersion` is always
    written as `1`; all four root keys are written even when the lists are
    empty; a null `nickname` is omitted from the output and a missing
    `nickname` decodes to null; an unknown root key and an unknown vehicle key
    both decode without throwing.

- [x] **3. Atomic file store.** Add `data/JsonFileStore.kt`: suspend `load()` and
  `save()` on `Dispatchers.IO`, guarded by a `Mutex` so concurrent writes
  serialize. It takes the target `File` as a constructor parameter and imports
  nothing from `android.*`, so it is testable on the JVM. Write to a sibling
  temp file, flush and sync it, then move it over the target with
  `ATOMIC_MOVE`. Return a result type for load, never throw across the layer.
  - **Done when:** `./gradlew testDebugUnitTest` passes with `TemporaryFolder`
    tests proving: a missing file loads as an empty store with
    `schemaVersion` 1 and no file created; a save followed by a load returns the
    same data; no temp file remains after a successful save; a file whose
    contents are not valid JSON returns the load-failure result and the file's
    bytes are left byte-for-byte unchanged; a stale temp file left on disk does
    not affect the loaded result and is not treated as the store.

- [x] **4. Vehicle repository.** Add `vehicles/VehicleRepository.kt` (interface:
  observe or list vehicles, add, update, delete) and its JSON-backed
  implementation over `JsonFileStore`. New ids come from an injected id
  generator so tests are deterministic; the production binding is
  `UUID.randomUUID().toString()`. Delete removes the vehicle and every
  `maintenanceItem` and `serviceLogEntry` whose `vehicleId` matches, in the same
  single write.
  - **Done when:** `./gradlew testDebugUnitTest` passes with tests proving: add
    appends a vehicle carrying the injected id and leaves existing entries
    untouched; update replaces fields while preserving the id and list position;
    delete removes the vehicle together with maintenance items and service log
    entries seeded for that vehicle, and leaves another vehicle's entries in
    place; every mutation is persisted, so a fresh store over the same file sees
    it.

- [x] **5. Application container and view models.** Add
  `VehicleMaintenanceApplication` holding a small container that owns the file
  (`filesDir/vehicle-maintenance.json`), the store, and the repository, and
  register it with `android:name` in the manifest. Add
  `vehicles/VehicleListViewModel.kt` and `vehicles/VehicleFormViewModel.kt`,
  each exposing one immutable state type and built through a `viewModelFactory`
  using `APPLICATION_KEY`. Put the form validation rules (see Data / contracts)
  in the form view model.
  - **Done when:** `./gradlew testDebugUnitTest` passes with tests over the form
    view model or an extracted pure validator proving: blank make, model,
    engine, or year each produce that field's error and block save; a non-
    numeric or out-of-range year produces the year error; leading and trailing
    whitespace is trimmed from all four text fields; a blank nickname is stored
    as null; a valid form saves and reports success. `./gradlew assembleDebug`
    still succeeds.

- [x] **6. Vehicle list screen and navigation.** Add
  `vehicles/VehicleListScreen.kt` and a nav host (`VehicleMaintenanceApp.kt`)
  with routes for the list, add, and edit (`vehicleId` argument). Wire it into
  `MainActivity`, deleting `Greeting` and `GreetingPreview`. Render loading,
  empty, loaded, and load-failure states. Include the delete confirmation
  dialog. All user-visible text goes in `strings.xml`.
  - **Done when:** `./gradlew assembleDebug` and `./gradlew lintDebug` succeed
    with no new lint warnings, no `Greeting` references remain in the source
    tree, and running the app on a device or emulator shows the empty state with
    a working add action.

- [x] **7. Add/edit vehicle form screen.** Add
  `vehicles/VehicleFormScreen.kt`: the five fields, save and cancel, inline
  validation errors, and a `@Preview` for the list and form screens.
  - **Done when:** `./gradlew assembleDebug` and `./gradlew lintDebug` succeed
    with no new warnings, and on a device or emulator: adding a vehicle returns
    to the list showing it, force-stopping and reopening the app still shows it,
    editing changes it in place, deleting asks for confirmation and removes it,
    submitting an empty form shows an error under every required field and does
    not navigate, and system back from the form returns to the list without
    saving.

- [x] **8. Compose UI test.** Add an instrumented Compose test in
  `app/src/androidTest/` covering the list empty state and the add-then-appears
  path against a repository backed by a temp file, and delete the scaffold
  `ExampleInstrumentedTest` only if it no longer compiles against the new
  `MainActivity`.
  - **Done when:** `./gradlew assembleDebugAndroidTest` succeeds, and
    `./gradlew connectedDebugAndroidTest` passes when a device or emulator is
    attached. Record in the review packet whether it was actually run on a
    device or only compiled.

## Files / areas

New:

- `app/src/main/java/com/example/vehiclemaintenance/data/MaintenanceStore.kt`
- `app/src/main/java/com/example/vehiclemaintenance/data/JsonFileStore.kt`
- `app/src/main/java/com/example/vehiclemaintenance/vehicles/Vehicle.kt`
- `app/src/main/java/com/example/vehiclemaintenance/vehicles/VehicleRepository.kt`
- `app/src/main/java/com/example/vehiclemaintenance/vehicles/VehicleListViewModel.kt`
- `app/src/main/java/com/example/vehiclemaintenance/vehicles/VehicleFormViewModel.kt`
- `app/src/main/java/com/example/vehiclemaintenance/vehicles/VehicleListScreen.kt`
- `app/src/main/java/com/example/vehiclemaintenance/vehicles/VehicleFormScreen.kt`
- `app/src/main/java/com/example/vehiclemaintenance/VehicleMaintenanceApp.kt`
- `app/src/main/java/com/example/vehiclemaintenance/VehicleMaintenanceApplication.kt`
- Unit tests mirroring these packages under `app/src/test/`
- One Compose UI test under `app/src/androidTest/`

Changed:

- `gradle/libs.versions.toml` - new versions, libraries, and the serialization plugin
- `app/build.gradle.kts` - apply the plugin, add the four dependencies
- `app/src/main/AndroidManifest.xml` - `android:name` for the application class
- `app/src/main/res/values/strings.xml` - all new user-visible text
- `app/src/main/java/com/example/vehiclemaintenance/MainActivity.kt` - host the nav graph
- `blueprint/build-plan.md` - `/complete` checks off item 1, not this feature

Untouched: `ui/theme/`, the launcher icons, `backup_rules.xml`,
`data_extraction_rules.xml`.

## Data / contracts

**File.** `filesDir/vehicle-maintenance.json` in app-private storage. Temp file
`vehicle-maintenance.json.tmp` in the same directory. This same shape is the
export format in feature 9, so the field names below are a contract, not an
internal detail.

**Root object.** All four keys are always written, even when empty:

```json
{
  "schemaVersion": 1,
  "vehicles": [],
  "maintenanceItems": [],
  "serviceLogEntries": []
}
```

**Vehicle object.**

| field | type | rule |
| --- | --- | --- |
| `id` | string | `UUID.randomUUID().toString()`, unique, never reused or rewritten by an edit |
| `nickname` | string, optional | omitted from JSON when absent |
| `year` | int | four digit |
| `make` | string | non-blank |
| `model` | string | non-blank |
| `engine` | string | non-blank |

**Json configuration.** `encodeDefaults = true` so empty lists and
`schemaVersion` are always present, `explicitNulls = false` so an absent
`nickname` is omitted rather than written as `null`, `ignoreUnknownKeys = true`
so a file written by a future schema still loads, `prettyPrint = false`.

**Load outcomes.** Missing file: an empty store, and no file is created until
the first write. Valid file: the decoded store. Unparseable or otherwise
unreadable file: a failure result. On failure the app shows an error state on
the list and **blocks all writes** for the session. Never overwrite a file that
failed to load and never silently start from empty, because this file is the
user's only copy of their history until they export it.

**Save.** Serialize the whole root, write to the temp file, flush and sync,
then move it over the target with `ATOMIC_MOVE`. A save that throws leaves the
previous file intact and surfaces a user-facing error, not a stack trace.
A stale temp file found at load time is never read as the store.

**Delete cascade.** Deleting a vehicle removes it plus every `maintenanceItem`
and `serviceLogEntry` with a matching `vehicleId`, in one write. Those lists are
always empty in this feature, but the contract is fixed now so features 2
through 5 do not have to reinterpret it.

**Form validation.** Trim `nickname`, `make`, `model`, and `engine` before
validating and before storing; a nickname that trims to empty is stored as
absent. `make`, `model`, and `engine` are required and non-blank. `year` is
required, must parse as an integer, and must fall between 1900 and the current
year plus 1 inclusive. Save is blocked while any field is invalid and every
invalid field shows its own message.

**List ordering.** Vehicles display in stored order; a new vehicle is appended
to the end and an edit keeps its position.

**Display.** With a nickname: nickname as the primary line, `year make model`
and `engine` as the secondary line. Without one: `year make model` primary,
`engine` secondary. All of these are user-entered strings rendered as plain
Compose `Text`; cap them with `maxLines` and ellipsis overflow so a long entry
cannot break the row, and never pass them through a format string as the format
argument.

**Accessibility.** Every field uses a Material 3 text field `label` so the
label is associated with its input, errors go in `supportingText` with
`isError = true` plus `Modifier.semantics { error(message) }` so they are
announced, and an error clears as soon as the field becomes valid. The delete
confirmation dialog names the vehicle being deleted.

## Testing

`./gradlew testDebugUnitTest` is the required gate and every step above names
what its tests must prove. Keep the store, repository, and validation logic free
of `android.*` imports so they test on the JVM with JUnit 4 and
`TemporaryFolder`. Instrumented Compose coverage is step 8; it needs a device or
emulator, so state plainly in the review packet whether it was run or only
compiled. Do not claim on-device, visual, or persisted-data evidence that was
not actually observed.

## Notes for the AI

- Follow `blueprint/context/coding-standards.md`: stateless composables with a
  leading `modifier` parameter, state from a view model, no repository access
  from a composable, no hardcoded colors or strings, no disk work on the main
  thread, light and dark both verified.
- Navigation Compose is the choice for routing because Jetpack first-party is
  the standard here and features 2 through 9 add seven more screens with
  arguments. This is the seam they will grow on.
- There is no DI library and this feature does not add one. The application
  container plus `viewModelFactory` is the wiring.
- Only the JSON store touches the four root lists. UI and view models go through
  the repository interface.
- No em dashes in code, comments, or commit messages.

## Open questions

None of these block implementation. Build with the stated assumption and correct
it at review if it is wrong.

- Year bounds are assumed to be 1900 through the current year plus 1. A
  different floor or ceiling is a one-line change.
- No maximum length is enforced on nickname, make, model, or engine. The UI
  truncates for display instead.
- List order is assumed to be insertion order. Sorting by nickname, year, or
  most recently serviced can replace it later without touching the file format.
