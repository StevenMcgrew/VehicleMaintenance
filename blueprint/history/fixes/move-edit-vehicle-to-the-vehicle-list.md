# Current Feature

**Move Edit vehicle to the vehicle list**

**Type:** Fix

**Status:** verified

**Branch:** `fix/move-edit-vehicle-to-the-vehicle-list`

## The problem

To edit a vehicle, you first have to open it, then tap the **Edit vehicle**
text button. That button sits in the vehicle detail screen's action row, next
to Service history and Log repair (`VehicleDetailScreen.kt`,
`VehicleActionsRow`). Editing should be reachable straight from the vehicle
list, next to Delete, as an icon.

## The fix

**Vehicle list row** (`VehicleListScreen.kt`, `VehicleRow`). Keep the current
text on the left:

- headline: year, make, and model, such as "2014 Toyota Tacoma"
- supporting line: the engine

The trailing slot becomes a `Row` holding two icon buttons, in this order:

| Order | Icon | Content description | Action |
|---|---|---|---|
| 1 | `Icons.Filled.Edit` (pencil, in `material-icons-core`) | New string `edit_vehicle_action`, "Edit %1$s", for example "Edit 2014 Toyota Tacoma" | Opens the vehicle form in edit mode |
| 2 | `Icons.Filled.Delete` (unchanged) | "Delete %1$s" (unchanged) | Opens the delete dialog (unchanged) |

Tapping anywhere else on the row still opens the vehicle detail screen.

**Wiring.**

- Add an `onEditVehicle: (String) -> Unit` parameter to `VehicleListScreen` and
  `VehicleListContent`, next to `onOpenVehicle`. Pass it through to
  `VehicleRow` as `onEdit`.
- In `VehicleMaintenanceApp.kt`, wire it to
  `navController.navigate(Routes.editVehicle(it))`. The existing
  `EDIT_VEHICLE` route already pops back when you save or cancel, so saving an
  edit returns you to the list.

**Remove Edit vehicle from the detail screen.** This is a move, not a copy.

- Drop the `Edit vehicle` `TextButton` from `VehicleActionsRow`, which keeps
  Service history and Log repair.
- Remove the `onEditVehicle` parameter from `VehicleDetailScreen`,
  `VehicleDetailContent`, `VehicleActionsRow`, their previews, the app's nav
  wiring, and the test harnesses that pass it:
  - `VehicleDetailScreenTest.kt:266,460`
  - `AdHocRepairTest.kt:164`
- Keep the `edit_vehicle` string. The vehicle form's "Edit vehicle" title still
  uses it.

**Must not break.**

- **Tapping the row still opens the vehicle.** The icon buttons consume their
  own taps, so tapping the pencil or the trash can does not also open the
  vehicle.
- **Screen-reader labels.** Both icons get vehicle-specific labels, so TalkBack
  can tell apart the rows for different vehicles.
- **Long names.** The headline is already capped at one line with an
  ellipsis, so a long vehicle name cannot push the icons off screen.
- **Tests.**
  - `VehicleDetailScreenTest.theActionRowOffersHistoryLogRepairAndEditFromTheBody`
    asserts that the Edit vehicle text is shown. Rename it to
    `theActionRowOffersHistoryAndLogRepair`, and assert that the Edit vehicle
    text no longer exists.
  - `VehicleListScreenTest`'s `Harness` passes the new callback.

## Build steps

- [x] **1. Add the edit icon to each vehicle row.** Add the string, the
   parameter, the row's icon pair, and the nav wiring. In
   `VehicleListScreenTest`, add a test that adds a vehicle, taps
   "Edit 2014 Toyota Tacoma", and asserts that the callback received that
   vehicle's id.
   *Done when:* `./gradlew assembleDebug compileDebugAndroidTestKotlin` passes,
   and each row shows the pencil and then the trash can on the right.
- [x] **2. Remove Edit vehicle from the detail screen.** Drop the button and the
   `onEditVehicle` plumbing, then update the detail test and the harnesses.
   *Done when:* `./gradlew assembleDebug lintDebug testDebugUnitTest` passes,
   and the detail screen's action row shows only Service history and Log repair.

## Verify

- `./gradlew assembleDebug lintDebug testDebugUnitTest` passes.
- `./gradlew connectedDebugAndroidTest` passes on the emulator, including the
  new row-edit test.
- On the device:
  - Vehicle list: each row shows "2014 Toyota Tacoma" with the engine below it
    on the left, and a pencil then a trash can on the right.
  - Tap the pencil. The **Edit vehicle** form opens, filled in with that
    vehicle. Change the engine and save. You return to the list, and it shows
    the new engine.
  - Tap the pencil, then ✕. You return to the list with nothing changed.
  - Tap the row text. The vehicle detail screen opens, and its action row shows
    only **Service history** and **Log repair**.
  - Tap the trash can. The delete dialog still opens.

## Verification results

- `./gradlew assembleDebug lintDebug testDebugUnitTest compileDebugAndroidTestKotlin`
  passed. Lint reported 0 errors and 18 warnings, the same as before this fix.
  None of the warnings are in the changed files.
- `./gradlew connectedDebugAndroidTest` passed on the Pixel 10a emulator
  (API 37): 39 tests, 0 failures. That includes the new
  `VehicleListScreenTest.rowEditIconOpensThatVehicleForEditing` and the renamed
  `VehicleDetailScreenTest.theActionRowOffersHistoryAndLogRepair`.
- Checked on the emulator:
  - **Vehicle row:** "2014 Toyota Tacoma" with the engine below it on the left,
    and a pencil then a trash can on the right.
  - **Editing:** the pencil opened **Edit vehicle**, filled in with that
    vehicle. Changing the engine to "4.0L V6" and saving returned to the list,
    which showed "4.0L V6".
  - **Cancelling:** the pencil, then ✕, returned to the list.
  - **Detail screen:** tapping the row text opened
    "2014 Toyota Tacoma 4.0L V6". Its action row shows only
    **Service history** and **Log repair**.
  - **Deleting:** the trash can still opened "Delete 2014 Toyota Tacoma?" with
    **Cancel** and **Delete**.

