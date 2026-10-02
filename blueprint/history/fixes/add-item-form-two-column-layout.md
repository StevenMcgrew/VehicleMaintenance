# Current Feature

## Add item form two column layout

**Type:** Fix
**Status:** verified
**Branch:** fix/add-item-form-two-column-layout

### The problem

The Add item and Edit item form (`MaintenanceItemFormScreen.kt`) is a borderless
three column table: a plain text label, the input, and an extra cell. The
labels take a column of their own, which squeezes the inputs on a phone.

### The fix

Lay the form out as two columns and move each field's label into its input
outline.

| # | Column 1 | Column 2 |
|---|----------|----------|
| 1 | Service input spanning both columns, label `Service` in the outline, placeholder `ex: Oil Change` | (taken by the input) |
| 2 | Number input, label `Due every` in the outline | Text `miles` |
| 3 | Number input, label `Due every` in the outline | Unit dropdown (Not set, Days, Weeks, Months, Years) |
| 4 | Number input, label `Remind me every` in the outline | Unit dropdown (Days, Weeks, Months, Years) |
| 5 | Button `Date last done` that opens the date picker, no separate label | The chosen date, or `Not set` |
| 6 | Number input, label `Mileage last done` in the outline | Text `miles` |

Layout details:

- Labels always sit in the outline, even when the input is empty and
  unfocused, so the Service placeholder shows beneath its label. Only the
  `TextFieldState` based `OutlinedTextField` offers
  `TextFieldLabelPosition.Attached(alwaysMinimize = true)`, so a new shared
  `OutlineLabelTextField` in `ui/FormTable.kt` wraps it and keeps the existing
  `value` and `onValueChange` contract, including values loaded after the form
  opens.
- Column 1 fills the remaining width; column 2 keeps the unit dropdown's fixed
  width so its cells line up on every row.
- An outline label takes about 8dp above the border, so column 2 drops by that
  much on rows whose first column is a labelled input. The date row has no
  outline label and is not offset.
- `ChooseDateButton` gains an optional `text`, defaulting to `Choose`.
- `item_name_placeholder` becomes `ex: Oil Change`.

Must not break:

- The Log additional repairs form, which still uses the three column `FormTable`.
- Saving, editing, deleting, validation messages, and keyboard order.
- The date picker, including Clear when a date is set.

### Build steps

1. [x] **Two column layout.** Add `OutlineLabelTextField` and the
   `ChooseDateButton` text option, replace `ItemFormTable`'s `FormTable` with
   the six two column rows above, and update the placeholder string.
   *Done when* `./gradlew assembleDebug lintDebug testDebugUnitTest` passes and
   the emulator shows the six rows aligned.

### Verify

- `./gradlew assembleDebug lintDebug testDebugUnitTest` passes.
- On the emulator, open a vehicle and tap Add item:
  - Six rows in two columns; Service spans both, with `ex: Oil Change` showing
    under its outline label.
  - The `miles` text and unit dropdowns line up with their inputs' borders.
  - Typing into each input keeps the text.
  - Tapping Date last done opens the picker; after OK the date shows in column 2.
- Edit an existing item: its saved values load into the right rows.

Verified on the emulator on 2026-10-02: the Add item form shows the six aligned
rows, typing and picking a date work, and editing Oil change loads its saved
values. Build, lint, and unit tests pass.
