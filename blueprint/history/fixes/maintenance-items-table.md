# Maintenance items as a table

**Type:** Fix
**Status:** verified
**Branch:** `fix/maintenance-items-table`

## The problem

`VehicleDetailScreen.kt` renders each maintenance item as a Material `ListItem`
inside a `LazyColumn`. The schedule is squashed into one sentence built by
`scheduleSummary()` ("every 5,000 miles, every 6 months, remind 5 months after"),
so the three intervals cannot be compared down a column. The overview already
calls for "maintenance items as a table with due status" on the vehicle detail
screen, so the list is also drifting from the plan.

Each row also carries a trailing "Delete" button. Four data columns plus a
per-row button will not fit a phone width, and delete belongs with the item it
destroys rather than on a scanning surface.

## The fix

Two coordinated changes: move delete into the item form, then turn the list into
a table.

### Delete moves to the maintenance item form

- `MaintenanceItemFormScreen` shows a "Delete" action in its top bar, only when
  `uiState.isEditing` and the item was found. It sits before Save.
- Tapping it opens the existing confirmation dialog, titled with the item name
  from `uiState.fields.name`. `DeleteItemDialog` moves out of
  `VehicleDetailScreen.kt` into its own file in the `maintenance` package so
  both screens can reach it. Its strings are unchanged.
- `MaintenanceItemFormViewModel` gains `delete()`, `dismissDeleteError()`, and
  `isDeleting` / `deletedSuccessfully` / `deleteFailed` on its ui state,
  mirroring the existing save trio. `isDeleting` also disables Save and Delete
  so a double tap cannot fire twice.
- On success the screen calls `onDone()` and lands back on the detail screen,
  where the repository flow has already dropped the row. On failure it stays put
  and shows `delete_item_failed` in the snackbar it already hosts.
- `VehicleDetailScreen` then sheds its delete plumbing: the trailing button,
  `pendingDeletionId`, the dialog, the snackbar host, and the `onDeleteItem` /
  `onDeleteErrorShown` parameters. `VehicleDetailViewModel` sheds `deleteItem`,
  `dismissDeleteError`, and `deleteFailed`. `delete_item_action` becomes unused
  and is removed; the form's visible "Delete" button needs no separate
  accessibility label.

### The list becomes a table

| Column | Source | Shown when unset |
| --- | --- | --- |
| Service | `name` | always set |
| Miles | `mileageInterval`, grouped (`5,000`) | `-` |
| Time | `recurrence`, short form (`6 mo`) | `-` |
| Remind | `reminder`, short form (`5 mo`) | always set |

- A fixed header row sits above a scrolling `LazyColumn` of item rows, with the
  existing `HorizontalDivider` between rows.
- Short interval units keep a row to one line in a narrow phone column:
  `d`, `wk`, `mo`, `yr`. New string resources, not the existing `duration_*`
  plurals, which are too wide for a column.
- Service takes the widest weight and ellipsizes at two lines. Miles, Time, and
  Remind are end-aligned so digits line up.
- `scheduleSummary()` and its `Interval.spelled()` helper are deleted once
  nothing calls them. `unitLabel()` stays; the item form uses it.

Must not break:

- Tapping a row still opens the edit form, and the row keeps a 48dp minimum
  touch target.
- Deleting an item still confirms first, still keeps logged services, and still
  reports failure without losing the user's place.
- Loading, load-failure, vehicle-not-found, and empty states are untouched.
- Light and dark mode both readable; header uses `onSurfaceVariant`, no
  hardcoded colors.
- No new Gradle dependency. The project has no Material icons artifact, so both
  the Delete action and the table stay text based.

## Build steps

- [x] **1. Delete moves to the item form.** Extract `DeleteItemDialog` to its own file,
   add the delete state and `delete()` to `MaintenanceItemFormViewModel`, add the
   top bar Delete action to `MaintenanceItemFormScreen`, and strip the delete
   plumbing from `VehicleDetailScreen`, `VehicleDetailContent`,
   `VehicleDetailViewModel`, the detail previews, and `delete_item_action` in
   `strings.xml`. The item list itself is untouched in this step.
   *Done when:* the detail rows no longer show a Delete button, opening an item
   for edit shows Delete in the top bar, confirming it returns to the detail
   screen with the item gone, adding a new item shows no Delete action, and
   `./gradlew lintDebug assembleDebug` is clean.

- [x] **2. Table rows replace the list.** Add the short-unit and placeholder strings,
   add a private `MaintenanceItemTable` (header row plus row composable) to
   `VehicleDetailScreen.kt`, swap it in for the `LazyColumn` of `ListItem`s,
   delete the now-unused summary helpers, and update both `@Preview`s so the
   populated one shows an item with no recurrence.
   *Done when:* the detail screen shows a Service / Miles / Time / Remind header
   over one row per item, a row with no mileage or recurrence shows `-` in those
   cells, tapping a row still opens the edit form, and
   `./gradlew lintDebug assembleDebug` is clean.

- [x] **3. Cover it.** In `VehicleDetailScreenTest`, wire the harness so `onEditItem`
   opens the form, assert the four column headers and the added item's Remind
   cell, and add a test that deletes from the form and checks both the vanished
   row and the store file. Add a JVM unit test for the mileage grouping helper
   under `app/src/test/.../maintenance/`.
   *Done when:* `./gradlew testDebugUnitTest` passes and the instrumented test
   passes on an emulator.

## Verify

1. Run the app, open a vehicle that has at least two maintenance items.
2. Confirm the header reads Service, Miles, Time, Remind and each item is one row.
3. Confirm an item created without a mileage interval or recurrence shows `-` in
   Miles and Time, and still shows its Remind value.
4. Tap a row: the edit form opens with that item loaded and a Delete action in
   the top bar.
5. Tap Delete, confirm the dialog names that item, confirm it: you land back on
   the table and the row is gone.
6. Tap Add item: no Delete action appears on the new-item form.
7. Toggle dark mode and confirm the header and rows stay legible.
