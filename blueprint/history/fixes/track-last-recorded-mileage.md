# Current Feature

## Track last recorded mileage

**Type:** Fix
**Status:** verified
**Branch:** fix/track-last-recorded-mileage

## The problem

Mileage only enters the app through a service log entry's `odometer`. The owner
has no way to record the odometer between services, so the mileage check can
only fire when work is logged, and nothing on screen shows how far the vehicle
has been driven.

Requested changes:

- **Vehicle detail screen** - below the row of extra small rounded buttons
  (`VehicleActionsRow`), a row reading `Last recorded mileage: <value>` followed
  on the same row by an `Update` text button. Tapping it opens a dialog to enter
  the current mileage.
- **Vehicle list screen** - the engine moves into the headline with year, make,
  and model. The supporting line becomes `Last recorded mileage: <value>`.
- **Add and edit vehicle form** - a field for the current mileage.

## The fix

**Data.** Add `recordedMileage: Int? = null` to `Vehicle` and `VehicleDraft`.
The field is optional with a default, so existing files and backups decode
unchanged and older builds ignore it (`ignoreUnknownKeys`). No `schemaVersion`
bump: `BackupParser` rejects any version other than the current one, so a bump
would break restoring existing backups for an additive field.

**One definition of "last recorded mileage".** Once a vehicle has a
`recordedMileage`, that value is the last recorded mileage and feeds the
mileage check. A vehicle without one (every vehicle saved before this fix)
falls back to its highest service log `odometer`. Null when neither exists,
shown as `Not recorded`. Extend `currentOdometer` in `MaintenanceStatus.kt` to
take the vehicle's reading.

**Logging service keeps it current.** `JsonServiceLogRepository.add` raises the
vehicle's `recordedMileage` to the entry's odometer in the same store write when
the entry is higher than the current last recorded mileage. A back dated entry
with a lower reading still never lowers it.

**Validation (shared).** A mileage entry is a whole number of zero or more,
formatted with `formatMileage`. Any such number may be saved, including one
lower than before, so a typo or a replaced odometer can be corrected.

**Lower reading warning.** When the entered value is lower than the highest
reading recorded before (the larger of the current `recordedMileage` and the
highest service log `odometer`), saving first shows a warning dialog naming
both numbers. It saves only after the user taps OK; Cancel returns to the entry
with the text intact. A confirmed lower value becomes the last recorded mileage
and the mileage check uses it.

**Detail screen.**

- New `LastRecordedMileageRow` under `VehicleActionsRow`, above the divider:
  label text plus an `Update` `TextButton`, wrapping on large font scales like
  the actions row.
- `Update` opens an `AlertDialog` with a number field prefilled with the current
  value, Save and Cancel. Save shows the field error while invalid, then the
  lower reading warning when it applies; a store failure shows the existing
  snackbar pattern.
- `VehicleDetailViewModel` combines the vehicle's reading with the log when it
  computes the odometer, so a manual reading that pushes an item overdue by
  mileage raises the existing `NewlyOverdueDialog`, the same as logging service.
  The first emission stays a baseline, not a new reading.
- Saving goes through `VehicleRepository` (a focused `updateMileage(vehicleId,
  miles)` or `update(vehicle.copy(...))`), never the store directly.

**List screen.**

- Headline uses `vehicle_summary_with_engine` (year make model engine), one line
  with ellipsis.
- Supporting content is `Last recorded mileage: <value>` or `Not recorded`.
- `VehicleListViewModel` needs the highest logged odometer per vehicle, so expose
  it through the service log repository (for example a per-vehicle map derived
  from the store) and put a row model with the computed mileage in
  `VehicleListUiState`. The composable does no computation.

**Form.**

- New optional `Current mileage` number field after Engine in
  `VehicleFormScreen`, with `VehicleFormFields.mileage` and a
  `VehicleFormErrors.mileage` error for a value that is not a whole number of
  zero or more.
- Add: blank stores null; no warning, since a new vehicle has no readings.
- Edit: prefilled with the vehicle's last recorded mileage. Blank clears the
  manual reading (the display falls back to the log). Saving a lower value shows
  the lower reading warning, so `VehicleFormViewModel` reads the highest logged
  odometer from the service log repository.

**Docs.** Update `project-overview.md` and `blueprint/project-plan.md` where
they say mileage only enters through a service log: the Vehicle model gains
`recordedMileage`, `odometer` is no longer "the only way mileage ever enters the
app", and the "Mileage is opportunistic" rule adds manual readings. Still no
background tracking or projection.

**Must not break:**

- Existing stores and backups load and restore with `recordedMileage` absent.
- Logging a service still updates the mileage check and still raises the newly
  overdue dialog.
- A back dated service log with a lower reading still cannot lower the known
  mileage; only a confirmed manual entry can.
- Money, dates, and reminder scheduling are untouched; reminders are date based
  and do not read mileage.
- All new text goes in `strings.xml` (`last_recorded_mileage`, `update`,
  `mileage_not_recorded`, the dialog title, the field label, and errors).

## Build steps

- [x] 1. **Model and logic** - add `recordedMileage` to `Vehicle` and `VehicleDraft`,
   extend `currentOdometer`, add the shared mileage validation, and add unit
   tests: serialization round trip and decode of an old file without the field,
   the combined reading (manual only, log only, both, neither), the highest
   prior reading used by the warning, logging service raising the vehicle's
   reading only when higher, and validation (blank, non-number, negative).
   Done when `./gradlew testDebugUnitTest` passes.
- [x] 2. **Form field** - `Current mileage` on add and edit with the rules above,
   repository `add` stores it, edit prefill and the lower reading warning work.
   Done when adding a vehicle with 45000 saves it, editing shows 45000, and
   saving a lower value shows the warning and saves only after OK.
- [x] 3. **Detail screen row and dialog** - the row, the Update dialog, the view model
   save path, and the combined odometer feeding status and the newly overdue
   callout, with `@Preview` updates.
   Done when updating the mileage changes the row immediately and an item past
   its mileage due point turns overdue with the dialog.
- [x] 4. **List screen** - engine in the headline, mileage as the supporting line,
   computed in the view model.
   Done when the list shows `2019 Toyota Tacoma 3.5L V6` over
   `Last recorded mileage: 45,000`, and `Not recorded` for a vehicle with no
   reading.
- [x] 5. **Docs** - the overview and project plan changes above.
   Done when neither doc claims the service log is the only mileage source.

## Verify

- `./gradlew testDebugUnitTest` and `./gradlew lintDebug` pass with no new
  warnings.
- Install on an emulator with existing data: the app opens, vehicles load, and
  vehicles without a reading show `Not recorded`.
- Add a vehicle with a mileage, open it, and see the row under the buttons.
- Tap `Update`, enter a higher mileage that passes an item's mileage due point,
  save: the row updates and the item turns overdue with the newly overdue
  dialog.
- Log a service with an odometer above the manual reading: the row and the list
  show the logged value.
- Try `Update` with a value below that logged odometer: the warning appears.
  Cancel keeps the old value; OK saves the lower value and the row shows it.
- Edit the vehicle: the field is prefilled; clear it and save, and the display
  falls back to the logged reading.
- Check the list, detail row, dialog, and form in light and dark mode and at a
  large font scale.
- Export a backup, then restore a backup made before this change: both succeed.
