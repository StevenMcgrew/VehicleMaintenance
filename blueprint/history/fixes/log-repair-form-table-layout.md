# Current Feature

## Log repair form table layout

**Type:** Fix
**Status:** verified
**Branch:** fix/log-repair-form-table-layout

### The problem

The Log repair form (`ServiceLogFormScreen.kt`) still stacks full-width outlined
fields with labels pinned into the outline, and the date is a read-only field
with a separate "Choose the date" text button. The Add item form now uses a
borderless three-column table, so the two forms look and behave differently.

### The fix

Lay the form out as the same borderless three-column table used by the Add item
form: labels in column 1, inputs in column 2, one extra item in column 3 when
the row needs it. The table is for layout only: no cell borders, dividers, or
header row.

Rows, in order:

| # | Label | Input | Column 3 |
|---|-------|-------|----------|
| 1 | Repair | Text input, placeholder `ex: Replaced alternator`, spanning columns 2 and 3 | (taken by the input) |
| 2 | Date | Tappable `Choose` button that opens the date picker | The chosen date |
| 3 | Odometer | Number input, no placeholder | Text `miles` |
| 4 | Cost $ | Money input (decimal keyboard, U.S. dollars), no placeholder | Text `(optional)` |
| 5 | Notes | Text input, no placeholder | Text `(optional)` |

**Log service uses the same table.** `ServiceLogFormScreen` also renders the Log
service form (reached from an item's "Log a completed service"). Both forms get
the table, and they differ only in row 1:

| Form | Row 1 label | Row 1 placeholder |
|------|-------------|-------------------|
| Log repair | Repair | `ex: Replaced alternator` |
| Log service | Service | `ex: Oil and filter` (prefilled with the item name, as today) |

Layout details, matching the Add item form:

- Columns line up on every row. Column 1 sizes to the widest label (capped so
  labels can wrap on a narrow phone), column 3 sizes to its widest content, and
  column 2 takes the rest.
- Inputs drop their in-outline labels. Each input still carries its row label
  for screen readers.
- A validation message appears directly under its input in column 2, or under
  the Choose button for the date.
- The date shows with `formatShortDate`. It starts on today, as it does now, and
  stays required, so the date dialog keeps only Cancel and OK (no Clear).
- Keyboard order stays description, odometer, cost, notes (Done on notes).
- Cost keeps its current validation and storage in integer cents.

**Share the table instead of copying it.** Move the table pieces that are private
to `MaintenanceItemFormScreen.kt` into one shared file under `ui/` and use them
from both forms:

- the three-column layout and its "spans columns 2 and 3" marker
- the label and text cells
- the screen-reader description modifier
- the `Choose` date button with its error text
- the date picker dialog, with an optional Clear action (the Add item form
  passes one; the log form does not) and the UTC date conversion now
  duplicated in both screens

Strings: set `log_repair_description_placeholder` to `ex: Replaced alternator`,
`log_description_placeholder` to `ex: Oil and filter`, `log_cost` to `Cost $`,
and `log_notes` to `Notes`. Add row 1 labels for Repair and Service, and one
shared `(optional)` string. Rename `item_choose_date` to a shared "Choose"
string. Remove `log_description`, `log_choose_date`, and the odometer, cost, and
notes placeholders once unused.

Must not break:

- Saving a repair (unlinked entry) and logging a service (resets its item).
- Validation: description and odometer required, a date that isn't in the future,
  and a valid dollar amount.
- The Add item form, which moves onto the shared code with no visible change.
- Light and dark mode, and a narrow phone width without horizontal scrolling.

### Build steps

1. [x] **Extract the shared table.** Move the table, cells, description
   modifier, Choose button, and date dialog out of `MaintenanceItemFormScreen.kt`
   into a shared `ui/` file, and point the Add item form at it.
   *Done when* `./gradlew assembleDebug` passes and the Add item form looks and
   behaves exactly as before.
2. [x] **Table layout for the log form.** Replace the stacked fields in
   `ServiceLogFormContent` with the five-row table, the per-form row 1 label and
   placeholder, the string updates, and updated previews (including dark mode
   at 360dp).
   *Done when* `./gradlew assembleDebug lintDebug testDebugUnitTest` passes with
   no new lint warnings in the changed files, and the previews show five aligned
   rows for both forms.

### Verify

- `./gradlew build` passes, and the instrumented `AdHocRepairTest` and
  `VehicleDetailScreenTest` pass. Back up the emulator store first.
- On the emulator, open a vehicle and tap **Log additional repairs**:
  - Five aligned rows, no borders; the Repair input spans columns 2 and 3.
  - Date shows today; Choose opens the picker, and the new date shows in column 3.
  - Saving with Repair or Odometer empty shows "Required" under that input.
  - A future date shows its error under the Choose button.
  - Cancel without saving.
- Open an item's **Log a completed service**: the same table, with row 1
  labeled Service and prefilled with the item name. Cancel without saving.
- Open **Add item**: unchanged from the previous fix.
- Check dark mode.
