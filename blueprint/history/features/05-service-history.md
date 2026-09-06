# Current Feature

**Feature 5 - Service history**

**Branch:** `feature/service-history`

**Status:** verified

## Goal

Give each vehicle a screen that lists everything ever logged against it, newest
first, so the owner can see the documented history the app exists to keep. This
is read-only: it shows entries that features 3 and 4 already write.

## In scope

- A `Service history` screen for one vehicle, reached from the vehicle detail
  screen.
- A read-only, newest-first list of every `ServiceLogEntry` whose `vehicleId`
  matches, covering both tracked completions and ad-hoc repairs.
- Per-entry display of description, date, odometer, cost when set, and notes
  when set.
- Loading, load-failure with retry, vehicle-not-found, and empty states that
  match the existing screens.
- A currency formatter that renders the stored minor units without ever going
  through a floating point type.
- Live updates: logging a service and returning to the history shows the new
  entry without a manual refresh.

## Out of scope

- Editing or deleting a log entry. Nothing in the build plan asks for it, and no
  repository method exists.
- Due and overdue status (feature 6), notifications (feature 7), cost totals
  (feature 8), export and import (feature 9), photos (feature 11).
- Filtering, search, sorting controls, grouping by year, or paging.
- A cross-vehicle "all history" view. The overview locks history as per-vehicle.
- Any change to how entries are written, to `ServiceLogEntry`, or to the JSON
  schema version.

## Build loop

`workflow.stepReview` is `feature`, so implement every build step in order and
present one review packet at the end. `workflow.checkpointCommits` is
`disabled`, so make no commits during implementation; `/complete` creates the
single feature commit after review.

Run `./gradlew testDebugUnitTest` after each step that adds or changes JVM
logic, and `./gradlew assembleDebug` plus `./gradlew lintDebug` before handing
the feature over. Instrumented tests need a device or emulator; run
`./gradlew connectedDebugAndroidTest` when one is available and say plainly if
it was not.

## Build steps

- [x] 1. **Format a stored cost for display.** Add
  `servicelog/CostFormat.kt` with
  `fun formatCost(minorUnits: Int, locale: Locale = Locale.getDefault()): String`,
  mirroring the shape of `maintenance/MileageFormat.kt` (injectable locale so
  tests are deterministic). Build the value as
  `BigDecimal.valueOf(minorUnits.toLong(), 2)` and render it with
  `NumberFormat.getCurrencyInstance(locale)`; never construct a `Double` or
  `Float` from the amount. Add
  `app/src/test/java/com/example/vehiclemaintenance/servicelog/CostFormatTest.kt`
  covering `0`, `6499`, and `123456` with `Locale.US` (expect `$0.00`,
  `$64.99`, `$1,234.56`).
  **Done when** `./gradlew testDebugUnitTest` passes with the new
  `CostFormatTest` cases.

- [x] 2. **Expose the per-vehicle history from the repository.** Add
  `fun entriesFor(vehicleId: String): StateFlow<List<ServiceLogEntry>>` to
  `ServiceLogRepository` and implement it in `JsonServiceLogRepository` with
  `holder.state.mapState { ... }`, the same pattern
  `JsonMaintenanceItemRepository.itemsFor` uses. Order it newest first using the
  contract under Data / contracts. Extend
  `app/src/test/java/com/example/vehiclemaintenance/servicelog/JsonServiceLogRepositoryTest.kt`
  with cases for: only the requested vehicle's entries are returned; distinct
  dates come back newest first; entries sharing a date come back most recently
  added first; the flow value reflects a new entry immediately after `add`
  succeeds; an entry whose `maintenanceItemId` was cleared by item deletion is
  still listed.
  **Done when** `./gradlew testDebugUnitTest` passes with those cases.

- [x] 3. **Add the history view model.** Add
  `servicelog/ServiceHistoryViewModel.kt` with a `ServiceHistoryUiState`
  (`isLoading`, `vehicle`, `entries`, `loadFailed`, `vehicleNotFound`) and a
  `refresh()` that calls `vehicles.load()` and reduces failure exactly the way
  `maintenance/VehicleDetailViewModel` does, including the
  `vehicleNotFound = !failed && vehicle == null` rule. Collect
  `vehicles.vehicles` and `serviceLog.entriesFor(vehicleId)` in
  `viewModelScope`. Provide a `factory(vehicleId)` reading
  `application.container`.
  **Done when** the module compiles (`./gradlew assembleDebug`) and
  `./gradlew testDebugUnitTest` still passes.

- [x] 4. **Add the history screen.** Add
  `servicelog/ServiceHistoryScreen.kt` with a stateful `ServiceHistoryScreen`
  and a stateless `ServiceHistoryContent`, following
  `maintenance/VehicleDetailScreen.kt`: `Scaffold` with a `TopAppBar`, a `Back`
  text button as the navigation icon, and a `when` over loading, load failure
  with `Retry`, vehicle not found, empty history, and the list. Render the list
  with `LazyColumn` keyed by `entry.id` and a `HorizontalDivider` between rows.
  Add the new strings to `app/src/main/res/values/strings.xml`. Add `@Preview`
  composables for the populated and empty states.
  **Done when** `./gradlew assembleDebug` and `./gradlew lintDebug` pass with no
  new warnings, and the previews render both states in Android Studio.

- [x] 5. **Wire up navigation and the vehicle action row.** Add
  `Routes.VEHICLE_HISTORY = "vehicles/{vehicleId}/history"` plus
  `fun vehicleHistory(vehicleId: String)` and a `composable` block in
  `VehicleMaintenanceApp.kt` that passes the required `vehicleId` through
  `requireVehicleId()` and pops the back stack on back. Add an
  `onViewHistory: () -> Unit` parameter to `VehicleDetailScreen` and
  `VehicleDetailContent`.

  Approved during implementation: rather than adding a third top bar action,
  move the existing `Log repair` and `Edit vehicle` text buttons out of the
  `TopAppBar` and render all three, plus `Service history`, as a single action
  row directly under the top bar. The top bar keeps only the vehicle title and
  the `Back` navigation icon, so a long title is no longer squeezed. The row
  sits above both the empty-items message and the maintenance table, so history
  stays reachable when a vehicle has entries but no items, and it scrolls
  horizontally so a large font scale cannot clip an action. Show it only in the
  loaded states, never over loading, load failure, or vehicle not found. Update
  the existing previews and any call site.
  **Done when** `./gradlew assembleDebug` passes and, in the running app,
  opening a vehicle shows the full title plus all three actions, and tapping
  `Service history` then going back returns to the vehicle detail screen with
  its state intact.

- [x] 6. **Prove it on a device.** Add
  `app/src/androidTest/java/com/example/vehiclemaintenance/servicelog/ServiceHistoryScreenTest.kt`
  following `AdHocRepairTest`: real repositories over a temp `JsonFileStore`,
  seeded with one vehicle, one maintenance item, and at least three entries
  (two sharing a date, one belonging to a different vehicle). Assert that only
  the current vehicle's entries appear, that they read newest first, that cost
  and notes appear only when set, and that a vehicle with no entries shows the
  empty state. Add a case to `VehicleDetailScreenTest` asserting the
  `Service history` row invokes `onViewHistory`.
  **Done when** `./gradlew connectedDebugAndroidTest` passes on a device or
  emulator, or, if none is available, the test is written, `assembleDebug`
  passes, and the unrun state is reported in the review packet.

## Files / areas

New:

- `app/src/main/java/com/example/vehiclemaintenance/servicelog/CostFormat.kt`
- `app/src/main/java/com/example/vehiclemaintenance/servicelog/ServiceHistoryViewModel.kt`
- `app/src/main/java/com/example/vehiclemaintenance/servicelog/ServiceHistoryScreen.kt`
- `app/src/test/java/com/example/vehiclemaintenance/servicelog/CostFormatTest.kt`
- `app/src/androidTest/java/com/example/vehiclemaintenance/servicelog/ServiceHistoryScreenTest.kt`

Changed:

- `app/src/main/java/com/example/vehiclemaintenance/servicelog/ServiceLogRepository.kt` - add `entriesFor`
- `app/src/main/java/com/example/vehiclemaintenance/VehicleMaintenanceApp.kt` - route and destination
- `app/src/main/java/com/example/vehiclemaintenance/maintenance/VehicleDetailScreen.kt` - `onViewHistory` parameter and the entry row
- `app/src/main/res/values/strings.xml` - new strings
- `app/src/test/java/com/example/vehiclemaintenance/servicelog/JsonServiceLogRepositoryTest.kt` - ordering and filtering cases
- `app/src/androidTest/java/com/example/vehiclemaintenance/maintenance/VehicleDetailScreenTest.kt` - entry point case

Not changed: `ServiceLogEntry`, `MaintenanceStore`, `JsonFileStore`, the schema
version, and every form screen.

## Data / contracts

**Ordering (the whole feature's contract).** `entriesFor` filters
`store.serviceLogEntries` by `vehicleId`, then reverses that filtered list, then
applies a stable `sortedByDescending { it.date }`. Result: later dates first,
and among entries sharing a date the most recently added one first. Reversing
before the stable sort is what produces that tiebreak, since the store appends
new entries to the end. Do not sort by `id`; ids are random UUIDs and carry no
order. Cover both halves of this in step 2's tests.

**Read-only.** The screen adds no repository write path. `entriesFor` returns a
`StateFlow` view over the shared `MaintenanceStoreHolder` state through
`mapState`, so no second cache is introduced.

**Cost.** `cost` stays an `Int` of minor units end to end. `formatCost` builds a
`BigDecimal` with scale 2 and formats it with the locale's currency instance;
v1 has no currency setting, exactly as it has no distance-unit setting. A null
`cost` renders nothing at all, not a zero and not a `-`.

**Odometer.** Rendered with the existing `formatMileage` from
`maintenance/MileageFormat.kt` and labelled in miles, matching the rest of the
app.

**Date.** Rendered as the ISO `LocalDate.toString()`, the same form the log
form's date field already shows, so the two screens agree and the instrumented
assertions are locale independent.

**Ad-hoc versus tracked.** The row does not distinguish them. `description` is
copied onto the entry at write time and item deletion clears
`maintenanceItemId` while keeping the entry, so `maintenanceItemId` is not a
reliable display signal and must not be read for one.

**Route.** `vehicles/{vehicleId}/history`, a sibling of the existing
`vehicles/{vehicleId}/...` routes, with `vehicleId` a required
`NavType.StringType` argument read through the existing `requireVehicleId()`
helper. There is no auth or tenancy model in this app: a single local user, one
JSON file, nothing off device.

**States.**

- Loading: `CircularProgressIndicator`, same as vehicle detail.
- Load failure: the existing `vehicles_load_error` string plus a `Retry` button
  wired to `refresh()`.
- Vehicle not found: the existing `vehicle_not_found` string plus `Back`.
- Empty: a title and body pair in the style of `maintenance_empty_*`, telling
  the user that logging a service or repair fills this in.
- Populated: the newest-first list.

**User text.** `description` and `notes` are user supplied and render through
Compose `Text`, which draws them as literal text with no markup interpretation.
Do not route them through `AnnotatedString` parsing, HTML, or a formatted
string argument that could reinterpret their content. Long values wrap rather
than being silently truncated to nothing; cap `description` at 3 lines with
`TextOverflow.Ellipsis` and let `notes` wrap freely.

## Testing

Unit (`./gradlew testDebugUnitTest`):

- `CostFormatTest` - minor units to a currency string at `Locale.US`, including
  zero and a value that needs a thousands separator.
- `JsonServiceLogRepositoryTest` - vehicle filtering, newest-first ordering,
  same-date tiebreak, live update after `add`, and an unlinked entry still
  listed after its item is deleted.

Instrumented (`./gradlew connectedDebugAndroidTest`, needs a device):

- `ServiceHistoryScreenTest` - the seeded entries render in the right order for
  the right vehicle, cost and notes appear only when present, and the empty
  state shows for a vehicle with nothing logged.
- `VehicleDetailScreenTest` - the `Service history` row invokes its callback.

No browser test harness applies; this is a native Android app.

## Notes for the AI

- Follow `blueprint/context/coding-standards.md`: strings in
  `strings.xml`, stateless composable plus `@Preview`, `modifier` as the first
  optional parameter, state from a `ViewModel`, Material 3 colors and type only,
  no hardcoded hex, both light and dark verified.
- No em dashes anywhere, including comments and the review packet.
- Comment only what the code cannot say. The ordering tiebreak in `entriesFor`
  deserves one line; nothing else in this feature does.
- Reuse `mapState`, `formatMileage`, `vehicles_load_error`, `vehicle_not_found`,
  `retry`, and `back` rather than adding parallel versions.
- Do not add a `Verify` command, a CI workflow, or a test runner as part of this
  feature.
