# Current Feature

**Feature 9: Export and import**

**Branch:** `feature/export-and-import`

**Status:** verified

## Goal

Let the user write the entire store to a JSON file they choose, and restore the
app from a file they choose, with the schema version checked before anything is
replaced. This is the app's only escape hatch: the on-device JSON file is
currently the sole copy of the user's history.

## In scope

- A new Export and import screen, reached from an action in the vehicle list top
  app bar, routed at `backup`.
- Export: pick a destination with the system file picker (`CreateDocument`),
  write the current store as JSON, report success or failure.
- Import: pick a file with the system file picker (`OpenDocument`), read and
  validate it, show a confirmation dialog naming what will be restored and that
  everything currently in the app is replaced, then replace the store atomically.
- Schema version handling: a file must carry an explicit `schemaVersion`;
  anything other than the version this build understands is rejected with a
  message that says so.
- Structural validation before replacement: object root, decodable lists, unique
  ids within each list, and every `vehicleId` resolving to a vehicle in the file.
- A store replacement seam on `MaintenanceStoreHolder` that also works when the
  existing file failed to parse, so import doubles as recovery.
- Loading, success, invalid-file, unsupported-version, too-large, cancelled, and
  write-failure behavior on the new screen.
- Unit tests for the parser, the file name builder, the holder seam, and the
  backup repository; a Compose test for the screen states.

## Out of scope

- Merging or appending an imported file into existing data. Import replaces.
- Selective import (one vehicle, one date range) or selective export.
- Sharing an export through a share sheet, email, or any other app.
- Scheduled or automatic backups, and any change to Android auto-backup rules.
- Cloud backup, photos, encryption, and password protection (features 11 and 12).
- CSV, PDF, or any human-report export format.
- Migration code for older schema versions. Version 1 is the only version that
  has ever shipped, so there is nothing to migrate from yet; the parser rejects
  every other version and leaves a single place to add migrations later.
- Any change to reminder scheduling. Imported `lastNotifiedAt` values round trip
  as-is and the existing daily worker reads the store on its next run.

## Build loop

`workflow.stepReview` is `feature` and `workflow.checkpointCommits` is
`disabled`, so build every step below without pausing for approval between them,
then stop after the last step and hand over one review packet covering the whole
feature. Do not create checkpoint commits. `/complete` creates the single feature
commit.

Each step must leave the app building and the existing tests passing. Run
`./gradlew testDebugUnitTest` after every step that adds or changes JVM logic and
`./gradlew lintDebug` before the handoff. Instrumented tests need a device or
emulator; if none is attached, say so in the handoff instead of claiming they ran.

## Build steps

- [x] **1. Store replacement seam.** Add `suspend fun replace(store: MaintenanceStore): StoreResult<Unit>`
  to `MaintenanceStoreHolder`. It takes the same mutex as `update`, saves through
  `JsonFileStore.save`, and only on success sets `_state.value` and `loaded = true`.
  Unlike `update` it does not require `loaded`, because replacing an unparseable
  file is exactly how a user recovers from one. On save failure, leave `_state`
  and `loaded` untouched and return the failure.
  **Done when** `./gradlew testDebugUnitTest` passes with new
  `MaintenanceStoreHolderTest` cases: replace after a failed load succeeds and
  makes a later `update` work; a failing save leaves the previous state and the
  previous `loaded` value unchanged.

- [x] **2. Backup parsing and validation.** Add `backup/BackupParser.kt` with a
  pure `parseBackup(json: String): BackupParse` and a sealed `BackupParse`:
  `Valid(store: MaintenanceStore)`, `UnsupportedVersion(version: Int)`,
  `Invalid`. It parses to a `JsonObject` first, because every field of
  `MaintenanceStore` has a default and `storeJson` ignores unknown keys, so
  decoding `{}` or an unrelated JSON document would otherwise look like a valid
  empty backup and wipe the user's data. Rules, in order: root must be a JSON
  object; `schemaVersion` must be present as an integer primitive, otherwise
  `Invalid`; a version other than `CURRENT_SCHEMA_VERSION` is
  `UnsupportedVersion`; decode with `storeJson`, and any `SerializationException`
  is `Invalid`; ids must be unique within each of the three lists; every
  `maintenanceItems[].vehicleId` and `serviceLogEntries[].vehicleId` must match a
  vehicle in the same file. Any violation is `Invalid`. A file with zero vehicles
  is valid.
  Add `backup/BackupFileName.kt` with `backupFileName(today: LocalDate): String`
  returning `vehicle-maintenance-YYYY-MM-DD.json`.
  **Done when** `./gradlew testDebugUnitTest` passes with `BackupParserTest`
  covering: a round trip of a store with all three lists populated; `{}`;
  malformed JSON; a JSON array root; a missing `schemaVersion`; a non-integer
  `schemaVersion`; `schemaVersion` 2 and 0; duplicate ids; an orphan
  `vehicleId` on an item and on a log entry; an empty but valid backup. Plus
  `BackupFileNameTest` pinning the exact name for a known date.

- [x] **3. Backup repository.** Add `backup/BackupRepository.kt`: an interface
  with `suspend fun exportSnapshot(): StoreResult<String>` and
  `suspend fun applyBackup(store: MaintenanceStore): StoreResult<Unit>`, and
  `JsonBackupRepository(holder: MaintenanceStoreHolder)`. `exportSnapshot`
  returns `StoreResult.Failure(StoreUnavailableException())` when the store has
  not loaded, so a failed read can never be written out as an empty "backup";
  otherwise it encodes `holder.state.value` with `storeJson`. `applyBackup`
  delegates to `holder.replace`. Expose `backupRepository` from `AppContainer`.
  This needs a `loaded` read on the holder; add a minimal internal accessor
  rather than widening `state`.
  **Done when** `./gradlew testDebugUnitTest` passes with
  `JsonBackupRepositoryTest`: export before a successful load fails with
  `StoreUnavailableException`; export after load returns JSON that
  `parseBackup` accepts and that round trips to the same store; `applyBackup`
  replaces the file contents and the in-memory state.

- [x] **4. Export screen and entry point.** Add `backup/BackupViewModel.kt`,
  `backup/BackupScreen.kt` (`BackupScreen` plus a stateless `BackupContent` with
  `@Preview`), and `backup/BackupFiles.kt` holding the `ContentResolver` work on
  `Dispatchers.IO`. Add `Routes.BACKUP = "backup"` and its `composable` to
  `VehicleMaintenanceApp`, and a top app bar action on the vehicle list that
  navigates to it. Wire export only: an `ActivityResultContracts.CreateDocument("application/json")`
  launcher seeded with `backupFileName(LocalDate.now())`, writing the snapshot as
  UTF-8 to the returned `Uri`. A null `Uri` means the user cancelled: change
  nothing and say nothing. Show a snackbar for success and for failure, cleared
  through a `...Shown` callback the way `VehicleListUiState.deleteFailed` is.
  Disable both actions while work is in flight. Catch `ActivityNotFoundException`
  around `launch` and show the picker-unavailable message. All strings go in
  `strings.xml`.
  **Done when** the app builds, `./gradlew lintDebug` gains no warnings, and on a
  device the vehicle list top bar opens the screen, Export writes a file the
  system picker created, and the resulting file parses back as a valid backup.

- [x] **5. Import.** Add an `ActivityResultContracts.OpenDocument` launcher,
  reading the chosen `Uri` as UTF-8 with an 8 MiB cap. Over the cap, unreadable,
  `Invalid`, and `UnsupportedVersion` each produce their own message and change
  nothing. A `Valid` parse moves the parsed store into UI state and opens a
  confirmation dialog naming the counts (vehicles, maintenance items, service
  records) and stating that everything currently in the app is replaced and that
  this cannot be undone. Cancel discards the parsed store. Confirm calls
  `applyBackup` and reports success or failure by snackbar.
  **Done when** `./gradlew testDebugUnitTest` still passes and, on a device,
  importing a file exported in step 4 restores the same vehicles, items, and
  history after the confirmation, while a text file, a `{}` file, and a file with
  `"schemaVersion": 2` are each rejected with their own message and leave the
  existing data untouched.

- [x] **6. Screen coverage and final verification.** Add
  `app/src/androidTest/.../backup/BackupScreenTest.kt` driving `BackupContent`
  with hoisted state, in the style of `VehicleListScreenTest`: both actions
  visible and enabled at rest, both disabled while busy, the confirmation dialog
  showing the counts and the replacement warning, cancel invoking the dismiss
  callback and not the confirm callback, and confirm invoking the confirm
  callback.
  **Done when** `./gradlew testDebugUnitTest` and `./gradlew lintDebug` pass, and
  `./gradlew connectedDebugAndroidTest` passes with a device attached, or the
  handoff states plainly that no device was available.
  Added beyond the listed test: `backup/BackupRoundTripTest.kt`, which drives
  export and import over a real `ContentResolver` stream, because the round-trip
  done-when in steps 4 and 5 needs evidence and the picker itself cannot be
  driven from a test.

## Files / areas

- `app/src/main/java/com/example/vehiclemaintenance/data/MaintenanceStoreHolder.kt` - add `replace`
- `app/src/main/java/com/example/vehiclemaintenance/backup/BackupParser.kt` - new
- `app/src/main/java/com/example/vehiclemaintenance/backup/BackupFileName.kt` - new
- `app/src/main/java/com/example/vehiclemaintenance/backup/BackupRepository.kt` - new
- `app/src/main/java/com/example/vehiclemaintenance/backup/BackupFiles.kt` - new
- `app/src/main/java/com/example/vehiclemaintenance/backup/BackupViewModel.kt` - new
- `app/src/main/java/com/example/vehiclemaintenance/backup/BackupScreen.kt` - new
- `app/src/main/java/com/example/vehiclemaintenance/VehicleMaintenanceApplication.kt` - `backupRepository` on `AppContainer`
- `app/src/main/java/com/example/vehiclemaintenance/VehicleMaintenanceApp.kt` - `Routes.BACKUP` and its destination
- `app/src/main/java/com/example/vehiclemaintenance/vehicles/VehicleListScreen.kt` - top app bar action and its `onOpenBackup` parameter
- `app/src/main/res/values/strings.xml` - every new string
- `app/src/test/java/com/example/vehiclemaintenance/data/MaintenanceStoreHolderTest.kt` - `replace` cases
- `app/src/test/java/com/example/vehiclemaintenance/backup/BackupParserTest.kt` - new
- `app/src/test/java/com/example/vehiclemaintenance/backup/BackupFileNameTest.kt` - new
- `app/src/test/java/com/example/vehiclemaintenance/backup/JsonBackupRepositoryTest.kt` - new
- `app/src/androidTest/java/com/example/vehiclemaintenance/backup/BackupScreenTest.kt` - new

No new dependency is needed. The Storage Access Framework contracts ship with
`androidx.activity`, which is already on the classpath.

## Data / contracts

**Export format.** Byte-for-byte the same shape as the on-disk store, encoded
with the existing `storeJson` instance (`encodeDefaults = true`,
`explicitNulls = false`, `ignoreUnknownKeys = true`, `prettyPrint = false`). The
overview locks the store and the export format as the same thing, so do not
introduce a second `Json` configuration, a wrapper envelope, or export-only
metadata. Dates stay ISO-8601 `YYYY-MM-DD` through `LocalDateSerializer`; cost
stays integer minor units.

**Export file.** MIME `application/json`. Suggested name
`vehicle-maintenance-YYYY-MM-DD.json` from the local date. The picker owns the
final name and location; never write to a path the app chose.

**Import contract.**

- Read as UTF-8. Reject above 8 MiB without parsing, so a wrong pick cannot
  exhaust memory.
- `schemaVersion` must be present and an integer. Absent is `Invalid`, not
  version 1, because `MaintenanceStore` defaults it and would silently accept
  unrelated JSON.
- `schemaVersion != CURRENT_SCHEMA_VERSION` is `UnsupportedVersion(version)`.
  A higher version tells the user the file came from a newer app version.
- After decoding: ids unique within `vehicles`, within `maintenanceItems`, and
  within `serviceLogEntries`; every `vehicleId` on an item or a log entry
  resolves to a vehicle in the same file. A `maintenanceItemId` that does not
  resolve is allowed, because the app already clears that link when an item is
  deleted while its history is kept.
- Import is whole-file replacement, never a merge, and is applied only through
  `MaintenanceStoreHolder.replace`, so it inherits the temp-file-plus-atomic-
  rename write. A rejected file changes nothing on disk or in memory.

**Picker MIME filter.** `OpenDocument` is launched with `arrayOf("*/*")`.
Providers label a `.json` file inconsistently (`application/json`,
`application/octet-stream`, `text/plain`), and a narrow filter would grey out the
user's own export. The parser is the real gate, so a wrong pick is rejected with
a clear message rather than being unselectable.

**No permissions.** The Storage Access Framework grants per-Uri access; no
manifest permission and no runtime prompt are added by this feature.

## Testing

JVM unit tests under `app/src/test/` for everything listed in the build steps:
parser rules, file name, holder `replace`, and repository behavior. These are
pure logic and need no device.

One Compose test under `app/src/androidTest/` driving the stateless
`BackupContent`, matching how `VehicleListScreenTest` and
`ServiceHistoryScreenTest` already test screens without touching the file system.
Do not attempt to drive the system file picker from a test; the SAF launchers
stay outside the tested surface.

Commands: `./gradlew testDebugUnitTest`, `./gradlew lintDebug`, and
`./gradlew connectedDebugAndroidTest` when a device or emulator is attached.

## Notes for the AI

- Follow `blueprint/context/coding-standards.md`: strings in `strings.xml`,
  Material 3 through `VehicleMaintenanceTheme`, stateless composable plus
  `@Preview`, `modifier` as the first optional parameter, no disk work on the
  main thread, expected failures as return values rather than thrown exceptions,
  and no raw exception text in the UI. No em dashes anywhere.
- Verify the new screen in both light and dark mode.
- Match the existing snackbar idiom: a flag or message in the UI state, a
  `LaunchedEffect` that shows it, and a callback that clears it.
- The confirmation dialog is the destructive-action gate. Never replace the store
  on the strength of a file pick alone.
- Export deliberately fails when the store never loaded. Writing an empty file
  the user believes is a backup is worse than refusing.
- Known rough edge, leave as is: if the store file was unparseable, the vehicle
  list keeps showing its load-error state after a successful import until the
  user taps Retry, which now succeeds. Do not add refresh plumbing for this; the
  error screen already offers the recovery action.
- `MaintenanceItem.lastNotifiedAt` is a plain string owned by feature 7. Round
  trip it untouched in both directions.
- Do not add a schema migration framework. One rejection branch and one place to
  extend later is the whole requirement.
