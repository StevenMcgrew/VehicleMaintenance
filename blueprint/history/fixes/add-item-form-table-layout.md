# Current Feature

## Add item form table layout

**Type:** Fix
**Status:** verified
**Branch:** fix/add-item-form-table-layout

### The problem

The Add item and Edit item form (`MaintenanceItemFormScreen.kt`) stacks full-width
outlined fields, each with its label pinned into the outline. The recurrence and
reminder units sit on their own full-width rows below their values, and the last
done date is a read-only field plus separate text buttons. The form is long and
the relationship between a number and its unit is hard to see.

### The fix

Lay the form out as a borderless three-column table. The table is for layout
only: no cell borders, dividers, or header row.

| Column | Holds |
|--------|-------|
| 1 | Field label (plain text, not inside the input outline) |
| 2 | Field input |
| 3 | One extra item when the row needs it, otherwise empty |

Rows, in order:

| # | Label | Input | Column 3 |
|---|-------|-------|----------|
| 1 | Service | Text input, placeholder `ex: Oil change`, spanning columns 2 and 3 | (taken by the input) |
| 2 | Due every | Number input, no placeholder | Text `miles` |
| 3 | Due every | Number input, no placeholder | Unit dropdown: Not set, Days, Weeks, Months, Years. Months preselected |
| 4 | Remind me every | Number input, no placeholder | Unit dropdown: Days, Weeks, Months, Years. Months preselected |
| 5 | Date last done | Tappable `Choose` button that opens the date picker | The chosen date, or `Not set` when none |
| 6 | Mileage last done | Number input, no placeholder | Text `miles` |

Layout details:

- Every cell in a column starts at the same x position on every row. Column 1
  sizes to the widest label (labels may wrap on a narrow phone), column 3 sizes
  to its widest content, and column 2 takes the remaining width.
- The Service input spans columns 2 and 3: it starts where column 2 starts and
  ends at the right edge of column 3. Its error message spans the same width.
- Cells are vertically centered within their row, with the existing 16dp screen
  padding and even spacing between rows and columns.
- Inputs drop their in-outline labels and placeholders except the Service
  placeholder. Each input still exposes its row label to accessibility services
  (for example, merge the label and input semantics or set the input's content
  description) so TalkBack announces "Service", "Due every", and so on.
- A validation message appears directly under the input it belongs to, inside
  column 2, and pushes following rows down. A unit error appears under the unit
  dropdown in column 3.
- Chosen dates in column 3 use the existing `formatShortDate` so the cell stays
  narrow.
- Clearing the last done date moves into the date picker dialog: when a date is
  already set, the dialog shows a `Clear` button next to Cancel and OK.
- Keyboard order stays Service, mileage, recurrence value, reminder value, last
  done mileage (Done on the last one).

Behavior changes the layout requires:

- **Recurrence unit defaults to Months.** New items start with Months selected.
  Editing an item that has no recurrence also shows Months, since the empty
  value is what marks "no recurrence".
- **Recurrence is decided by the value alone.** An empty recurrence value means
  no recurrence, whatever the unit shows. A value with the unit set to Not set is
  rejected with "Choose a unit". This replaces the old "a unit without a value is
  rejected" rule, which would otherwise fire on every new item.
- **Reminder dropdown has no Not set.** Reminder is required, so its dropdown
  lists only Days, Weeks, Months, and Years, as it does today. It already
  defaults to Months.

Strings: update `item_name`, `item_mileage_interval`, `item_recurrence_value`,
`item_reminder_value`, `item_last_done_date`, `item_last_done_mileage`, and
`item_name_placeholder` to the new text, and add `item_miles`, `item_choose_date`,
and `item_clear_date` (reuse or rename the existing clear string). Remove
placeholder and unit strings that become unused so lint stays clean.

Must not break:

- Saving, editing, deleting, and the notification permission gate on first save.
- Live re-validation after a failed save.
- The stored data format (no schema change).
- Light and dark mode, and a narrow phone width without horizontal scrolling.

### Build steps

1. [x] **Recurrence validation and default.** Change `MaintenanceItemFormFields` so
   `recurrenceUnit` defaults to Months, make `toFormFields` fall back to Months,
   and make the validator decide recurrence from the value alone. Update
   `MaintenanceItemFormValidatorTest`: replace the "unit without a value" case
   with "an empty value with a unit is accepted as no recurrence", and keep the
   "value without a unit" case.
   *Done when* `./gradlew testDebugUnitTest` passes with the updated cases.
2. [x] **Table layout.** Replace the stacked fields in `MaintenanceItemFormContent`
   with the three-column table and the six rows above, including the `Choose`
   date button, the date shown in column 3, the dialog `Clear` button, the
   Service input spanning columns 2 and 3, the Not set option on the recurrence
   dropdown only, the string updates, and the updated previews.
   *Done when* `./gradlew assembleDebug lintDebug` passes with no new warnings
   and the previews show the six aligned rows in light and dark mode.

### Verify

- `./gradlew testDebugUnitTest` and `./gradlew lintDebug` pass.
- On the emulator, open a vehicle and tap Add item:
  - Six rows appear with aligned label, input, and extra columns and no borders.
  - The Service input stretches across columns 2 and 3.
  - Both unit dropdowns show Months. The Due every dropdown lists Not set, Days,
    Weeks, Months, Years; the Remind me every dropdown lists Days, Weeks, Months,
    Years.
  - Tapping Choose opens the date picker; after OK the date shows in column 3.
    Reopening shows Clear, which resets the cell to Not set.
  - Saving with only Service and Remind me every filled creates an item with no
    time recurrence.
  - Entering a Due every value with Not set shows "Choose a unit" under that
    dropdown.
- Edit an existing item: its values load into the right rows and save correctly.
- Check dark mode and a narrow screen (about 360dp wide).
