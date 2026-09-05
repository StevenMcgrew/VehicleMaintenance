# Current Feature

**Feature 4 - Log an ad-hoc repair**

**Branch:** `feature/log-an-ad-hoc-repair`

**Status:** verified

## Goal

Let the user record work on a vehicle that was never set up as a tracked
maintenance item: description, date, odometer, and optional cost and notes. The
entry is the same `ServiceLogEntry` a completion writes, with
`maintenanceItemId` left null, so features 5, 6, and 8 read one list.

## In scope

- A "Log repair" entry point on the vehicle detail screen, available whenever the
  vehicle loaded, including when it has no maintenance items yet.
- A route to the log form with no item id.
- `ServiceLogFormViewModel` and `ServiceLogFormScreen` accept a null item id:
  empty starting description, repair-specific title and placeholder, no item
  lookup.
- A not-found state for the ad-hoc form when the vehicle no longer exists,
  mirroring the item-linked form's item-not-found state.
- Saving writes a `ServiceLogEntry` with `maintenanceItemId = null` and touches
  no maintenance item.

## Out of scope

- Showing logged entries anywhere. Service history is feature 5.
- Editing or deleting a logged entry. Not in the plan for v1.
- Due, overdue, or the mileage check that a new odometer reading triggers
  (feature 6), notifications (feature 7), cost totals (feature 8).
- Attaching a repair to an item after the fact, or converting a repair into a
  tracked item.
- Any change to the stored `ServiceLogEntry` shape or the schema version.

## Build loop

`workflow.stepReview` is `feature` and `workflow.checkpointCommits` is
`disabled`: implement all three steps, then present one review packet. No
checkpoint commits; `/complete` creates the single feature commit.

## Build steps

- [x] **1. Make the log form work without an item.**
  Change `ServiceLogFormViewModel.itemId` and its `factory` to `String?`. When it
  is null, skip the maintenance item lookup, leave `description` empty, and set
  `date` to today as the item path already does; instead check that
  `vehicleId` matches a vehicle from `VehicleRepository.vehicles` and expose a new
  `vehicleNotFound` flag in `ServiceLogFormUiState`. Add `vehicleRepository` to
  the constructor and wire it from the app container in `factory`. In
  `ServiceLogFormScreen`, take `itemId: String?`, give the `viewModel` key a
  distinct value for the ad-hoc case, use `R.string.log_repair_title` and
  `R.string.log_repair_description_placeholder` when `itemId` is null, render the
  `vehicle_not_found` message in the same not-found column the item path uses,
  and include `vehicleNotFound` in the `actionsEnabled` guard. Add the two new
  strings to `strings.xml`. Add an `@Preview` for the ad-hoc form state.
  *Done when:* `./gradlew testDebugUnitTest` and `./gradlew lintDebug` pass with
  no new warnings, and the existing item-linked log screen still opens with the
  item name prefilled and the "Log service" title.

- [x] **2. Add the route and the entry point.**
  Add `Routes.LOG_REPAIR = "vehicles/{vehicleId}/repairs/new"` and
  `Routes.logRepair(vehicleId)` in `VehicleMaintenanceApp.kt`, with a `composable`
  that passes `itemId = null` and pops the back stack on done. Add an
  `onLogRepair: () -> Unit` parameter to `VehicleDetailScreen` and
  `VehicleDetailContent`, rendered as a `TextButton` labelled
  `R.string.log_repair` in the top app bar next to "Edit vehicle" under the same
  `vehicle != null` condition, and wire it in the nav graph. Update the existing
  `VehicleDetailContent` previews for the new parameter.
  *Done when:* `./gradlew assembleDebug` succeeds, and on a device or emulator
  "Log repair" on vehicle detail opens a form titled "Log repair" with an empty
  description, both for a vehicle with items and for one with none.

- [x] **3. Cover the ad-hoc path with tests.**
  In `ServiceLogFormValidatorTest`, add cases proving that validating with
  `itemId = null` yields a draft whose `maintenanceItemId` is null and that the
  required, future-date, odometer, and cost rules still apply. In
  `app/src/androidTest/.../servicelog/`, add a Compose test in the style of
  `VehicleDetailScreenTest` (real repositories over a temp `JsonFileStore`) that
  opens the ad-hoc form for a seeded vehicle, fills description, date, odometer,
  and cost, saves, and asserts the on-disk store gained one entry with
  `maintenanceItemId == null` and the seeded maintenance item unchanged.
  *Done when:* `./gradlew testDebugUnitTest` passes; the instrumented test is run
  with `./gradlew connectedDebugAndroidTest` and passes when a device or emulator
  is available, and is reported as written but not run when none is.

## Files / areas

- `app/src/main/java/com/example/vehiclemaintenance/servicelog/ServiceLogFormViewModel.kt` - nullable item id, vehicle check, `vehicleNotFound`
- `app/src/main/java/com/example/vehiclemaintenance/servicelog/ServiceLogFormScreen.kt` - nullable item id, title, placeholder, not-found state, preview
- `app/src/main/java/com/example/vehiclemaintenance/VehicleMaintenanceApp.kt` - `LOG_REPAIR` route and `logRepair` builder
- `app/src/main/java/com/example/vehiclemaintenance/maintenance/VehicleDetailScreen.kt` - `onLogRepair` top bar action
- `app/src/main/res/values/strings.xml` - `log_repair`, `log_repair_title`, `log_repair_description_placeholder`
- `app/src/test/java/com/example/vehiclemaintenance/servicelog/ServiceLogFormValidatorTest.kt` - null item id cases
- `app/src/androidTest/java/com/example/vehiclemaintenance/servicelog/` - new ad-hoc flow test

Unchanged: `ServiceLogEntry`, `ServiceLogRepository`, `ServiceLogFormValidation`,
`MaintenanceStore`. The repository already accepts a null `maintenanceItemId` and
`JsonServiceLogRepositoryTest` already covers that write.

## Data / contracts

- Written entry: `id` from the repository's `newId` (UUID string), `vehicleId`
  from the route, `maintenanceItemId = null`, `description` trimmed and
  non-empty, `date` not in the future, `odometer` a non-negative `Int` (miles),
  `cost` optional integer minor units parsed through `BigDecimal` with more than
  two decimal places rejected rather than rounded, `notes` trimmed or null.
  `ServiceLogFormValidator` already enforces all of this; do not restate the
  rules in a second place.
- No maintenance item is read or modified when `maintenanceItemId` is null. The
  entry is appended in one `MaintenanceStoreHolder.update` write.
- Route shape: `vehicles/{vehicleId}/repairs/new`. `vehicleId` is a required
  string argument; the screen never receives an item id on this route.
- No new persisted field and no schema version change. Existing stores stay
  readable and files written by this feature stay readable by the current app.

## Testing

- JVM unit tests are the gate: `./gradlew testDebugUnitTest`.
- `./gradlew lintDebug` gains no new warnings.
- Instrumented Compose test for the ad-hoc flow, run with
  `./gradlew connectedDebugAndroidTest` when a device or emulator is available
  (`verification.uiEvidence` is `when-available`). Report honestly if none was.
- Manual on-device pass: light and dark mode, the vehicle-with-no-items case,
  validation errors showing and clearing, and the save-failure snackbar path.

## Notes for the AI

- Read `blueprint/context/coding-standards.md` before editing. No em dashes. No
  hardcoded user-visible strings. Comment only what the code cannot say.
- Keep `ServiceLogFormScreen` and its ViewModel as one screen serving both cases
  rather than forking a near-duplicate repair screen. The two differ only by
  title, starting description, placeholder, and which not-found check applies.
- Do not add a vehicle-exists guard inside `JsonServiceLogRepository`. Vehicle
  deletion already cascades to items and entries, and the existing repository
  tests seed stores with no vehicles; the guard belongs in the ViewModel.
- Preserve the existing error-field behavior: errors appear on save, then update
  live while `showErrors` is set, and each errored field keeps its
  `semantics { error(...) }` announcement and supporting text.
- The top app bar title already uses `maxLines = 1` with ellipsis, so a second
  text action fits without restructuring the bar or renaming "Edit vehicle".
- Do not touch the completion path's clock reset (`lastDoneDate`,
  `lastDoneMileage`, `lastNotifiedAt`); it must keep working exactly as it does.
