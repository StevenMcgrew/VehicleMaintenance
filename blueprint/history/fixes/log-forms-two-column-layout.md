# Current Feature

## Log forms two column layout

**Type:** Fix
**Status:** verified
**Branch:** fix/log-forms-two-column-layout

### The problem

The Log service and Log repair forms (one screen, `ServiceLogFormScreen.kt`)
still use the borderless three column `FormTable`: a plain text label, the
input, and an extra cell. The Add item form moved to two columns with labels
in the input outlines (`blueprint/history/fixes/add-item-form-two-column-layout.md`),
so the forms no longer match.

### The fix

Lay both log forms out the same way as the Add item form: two columns, each
field's label in its input outline, and a fixed width second column.

| # | Column 1 | Column 2 |
|---|----------|----------|
| 1 | Description input spanning both columns, outline label `Service` (Log service) or `Repair` (Log repair), with the existing placeholder (`ex: Oil and filter` or `ex: Replaced alternator`) | (taken by the input) |
| 2 | Button `Date of service` that opens the date picker, no separate label | The chosen date |
| 3 | Number input, label `Odometer` in the outline | Text `miles` |
| 4 | Decimal input, label `Cost $` in the outline | Text `(optional)` |
| 5 | Text input, label `Notes` in the outline | Text `(optional)` |

Shared code:

- Move the Add item form's private `ItemFormRow`, its `TABLE_GAP` and
  `OUTLINE_LABEL_OFFSET`, and the second column width into the shared form file
  as `FormRow`, `FormRowGap`, and `FormSecondColumnWidth`, and use them from
  both screens. Column 2 keeps today's 132dp width, so the log forms and
  the item form line up the same way.
- Once neither screen uses them, remove `FormTable`, `spansExtraColumn`, and
  `FormTextField` from `ui/FormTable.kt`. Rename the file to `ui/FormRow.kt` to
  match what it now holds.
- `log_date` changes from `Date` to `Date of service` for the button text.
- Make `ChooseDateButton`'s `text` required and remove the then unused
  `choose_date` (`Choose`) string so lint stays clean.
- Rename `ItemFormTable` and `LogFormTable` to `ItemForm` and `LogForm`, since
  neither is a table any more.

Typing fix found while verifying:

- `OutlineLabelTextField` (added by the Add item two column fix) dropped
  characters during fast typing: "Test only" became "Tes only". It pushed the
  parent's value back into the field whenever that value changed, and that
  value trails the keyboard, so it overwrote the newest characters. The field
  now owns its text: it starts from `initialValue`, reports every edit, and
  never reads a later value back. This is safe because both forms show their
  fields only after loading, and neither view model rewrites a field's text.

Must not break:

- Saving a service or repair, validation messages under the right input
  (including the date error under the Date of service button), and keyboard order
  (description, odometer, cost, notes, Done on notes).
- The date picker on the log forms (no Clear button, since a log date is
  required).
- `AdHocRepairTest`, which finds the repair placeholder by its text and types
  into the editable fields in order.
- The Add item form, which moves onto the shared `FormRow` with no visible
  change.

### Build steps

1. [x] **Two column log forms.** Add the shared `FormRow`, move the Add item
   form onto it, replace `LogFormTable`'s `FormTable` with the five rows above
   using `OutlineLabelTextField`, and remove the unused table code and string.
   *Done when* `./gradlew assembleDebug lintDebug testDebugUnitTest` passes with
   no new warnings, and the emulator shows the five aligned rows on both log
   forms with the Add item form unchanged.

### Verify

- `./gradlew assembleDebug lintDebug testDebugUnitTest` passes.
- On the emulator, open a vehicle:
  - Tap an item, then Log item as completed: the Log service form shows five
    rows in two columns, `Service` in the description outline and its
    placeholder beneath it.
  - Tap Log additional repairs: the same layout with `Repair` and
    `ex: Replaced alternator`.
  - `miles` and both `optional` texts line up with their inputs' borders; the
    date sits beside the Date of service button.
  - Tap Date of service, pick a day, OK: the date updates in column 2.
  - Type into every field, then cancel. The emulator holds real data, so the
    save path is proven by `AdHocRepairTest` instead of a manual save.
- Add item still matches its two column layout.
- Back up the emulator's `files/vehicle-maintenance.json`, then run
  `AdHocRepairTest` with
  `-Pandroid.injected.androidTest.leaveApksInstalledAfterRun=true`, and confirm
  the store is unchanged afterwards.

Verified on the emulator on 2026-10-02:

- Log repair and Log service show the five aligned rows; Log service fills in
  the item name. Picking October 1 updates the date cell; the picker has no
  Clear button. Both forms were cancelled without saving.
- Fast typing (`adb shell input text`) keeps every character in both log forms
  and Add item, which also looks unchanged.
- `AdHocRepairTest`: 2 tests, 0 failures, run with the APKs left installed; the
  emulator store's SHA-256 matched before and after.
- `./gradlew assembleDebug lintDebug testDebugUnitTest` passes; lint reports
  the same 16 existing issues (dependency versions, unused scaffold colors,
  a redundant manifest label) and none from this change.
