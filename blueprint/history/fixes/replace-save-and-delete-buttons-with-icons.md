# Current Feature

**Replace Save and Delete buttons with icons**

**Type:** Fix

**Status:** verified

**Branch:** `fix/replace-save-and-delete-buttons-with-icons`

## The problem

The forms and the vehicle list still use text buttons for Save and Delete. The
other top-bar controls are icons (← Back, ✕ Cancel, ↑↓ Export and import), so
these text buttons look out of place and take width away from the titles.

| Where | Button | Code |
|---|---|---|
| Add/Edit vehicle form, top bar | Save | `VehicleFormScreen.kt` |
| Add/Edit maintenance item form, top bar | Save | `MaintenanceItemFormScreen.kt` |
| Edit maintenance item form, top bar | Delete | `MaintenanceItemFormScreen.kt` |
| Log service / Log repair form, top bar | Save | `ServiceLogFormScreen.kt` |
| Vehicle list, each row | Delete | `VehicleListScreen.kt` (`VehicleRow`) |

## The fix

**Icons.**

- **Save:** Material Symbols `save` (the floppy disk), filled, 24dp. The
  current icon library (`material-icons-core`) has no save icon, so this works
  the same way as the `ic_import_export` drawable: add
  `app/src/main/res/drawable/ic_save.xml` from Material Symbols Outlined `save`
  at weight 400, fill 1, 24dp (Apache 2.0). Use fill 1 so it matches the
  filled core icons. A ✓ checkmark (`Icons.Filled.Check`) is the other common
  choice for "save" in Android top bars, but you asked for a save icon.
- **Delete:** `Icons.Filled.Delete` (the trash can). It is already in
  `material-icons-core`, so no drawable is needed.

**Buttons.** Replace each `TextButton` in the table above with an `IconButton`.
Keep the same `onClick` and `enabled` logic. Inside it, add an `Icon` whose
`contentDescription` comes from existing strings:

- Save → `R.string.save` ("Save")
- Delete on the item form → `R.string.delete` ("Delete")
- Delete on a vehicle row → the existing `deleteLabel`
  (`delete_vehicle_action`, for example "Delete 2014 Toyota Tacoma"). Put the
  label on the `Icon` and remove the `clearAndSetSemantics` workaround, which
  the icon no longer needs.

No new strings are needed. Disabled icon buttons dim automatically, as the
disabled text buttons did.

**Out of scope (these stay text).**

- The **Delete** and **Cancel** buttons in the confirmation dialogs
  (`DeleteVehicleDialog`, `DeleteItemDialog`). Material dialogs use text
  actions, and a destructive confirmation should spell out the word.
- The **Delete item** row in the vehicle detail bottom sheet. It is a labelled
  menu row, not a button.

**Must not break.**

- **Existing tests that find these buttons by text.** Update them to use
  `onNodeWithContentDescription`:
  - `AdHocRepairTest.kt:108` clicks Save.
  - `VehicleListScreenTest.kt:72,81` waits for Save, then clicks it.
  - `VehicleDetailScreenTest.kt:127,135,147,151,306,421,425` waits for or
    clicks Save.
  - `VehicleDetailScreenTest.kt:308` clicks the top-bar Delete.

  The `waitForText(R.string.save)` calls wait for the form to open. They need
  an equivalent helper that waits on the content description. The dialog-Delete
  lookups at `VehicleDetailScreenTest.kt:311,341` already scope to
  `isDialog()` and stay as they are.
- **Screen-reader labels.** Every icon button needs a non-null content
  description. A vehicle row's delete keeps its vehicle-specific label.
- **Right-to-left layouts.** Neither icon is directional, so no auto-mirroring
  is needed.

## Build steps

- [x] **1. Save icons.** Add `ic_save.xml`, swap the three top-bar Save buttons,
   and update the Save lookups in `AdHocRepairTest`, `VehicleListScreenTest`,
   and `VehicleDetailScreenTest`.
   *Done when:* `./gradlew assembleDebug compileDebugAndroidTestKotlin` passes,
   and no form top bar shows the word "Save".
- [x] **2. Delete icons.** Swap the item-form top-bar Delete and the vehicle-row
   Delete, and update the top-bar Delete lookup in `VehicleDetailScreenTest`. In
   `VehicleListScreenTest`, add a test that adds a vehicle, taps the row's
   delete icon (found by its "Delete <vehicle>" content description), and
   checks that the delete dialog appears.
   *Done when:* `./gradlew assembleDebug lintDebug testDebugUnitTest` passes,
   and neither the item form nor the vehicle list shows a "Delete" text button.

## Verify

- `./gradlew assembleDebug lintDebug testDebugUnitTest` passes.
- `./gradlew connectedDebugAndroidTest` passes on the emulator, including the
  new vehicle-row delete test.
- On the device:
  - Add vehicle: the top-right control is a floppy-disk icon. Tapping it saves
    the vehicle and returns to the list.
  - Vehicle list: each row ends with a trash-can icon. Tapping it opens
    "Delete <vehicle>?", and the dialog buttons are still the words
    **Cancel** and **Delete**.
  - Open a vehicle, then Add item: the top bar shows ✕ and the save icon.
  - Edit an existing item: the top bar shows ✕, a trash can, and the save icon.
    The trash can opens the delete dialog.
  - Log service or Log repair: the top bar shows the save icon.
  - While saving, the icons dim and cannot be tapped.
  - In dark mode, the icons use the same tint as ✕.

## Verification results

- `./gradlew assembleDebug lintDebug testDebugUnitTest compileDebugAndroidTestKotlin`
  passed. Lint reported 0 errors and 18 warnings, the same as before this fix.
  None of the warnings are in the changed files.
- `./gradlew connectedDebugAndroidTest` passed on the Pixel 10a emulator
  (API 37): 38 tests, 0 failures. That includes the new
  `VehicleListScreenTest.rowDeleteIconAsksToConfirmDeletingThatVehicle` and the
  updated Save and Delete lookups.
- Checked on the emulator:
  - **Add vehicle:** the top bar shows ✕ and the floppy-disk icon. Tapping the
    icon saved "2014 Toyota Tacoma" and returned to the list.
  - **Vehicle list:** the row ends with a trash-can icon. Tapping it opened
    "Delete 2014 Toyota Tacoma?" with the text buttons **Cancel** and
    **Delete**.
  - **Add item:** the top bar shows ✕ and the save icon, and saving worked.
  - **Edit item:** the top bar shows ✕, the trash can, and the save icon. The
    trash can opened "Delete Oil?".
  - **Log service:** the top bar shows ✕ and the save icon, in both light and
    dark mode.
- **Icon tint:** the action icons use the Material 3 top-bar action color
  (`onSurfaceVariant`, the same as the ↑↓ icon). That is slightly softer than
  the ✕ navigation icon (`onSurface`) in both themes. This is the standard
  `TopAppBar` styling, not a defect.
- **Not checked on the device:**
  - the Log repair form, which uses the same `ServiceLogFormScreen` code
  - the disabled icons while a save is in progress

