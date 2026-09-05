# Current Feature

**Pin maintenance item form labels to the outline and add placeholders**

**Type:** Fix
**Status:** verified
**Branch:** `fix/pin-item-form-labels-and-add-placeholders`

## The problem

In `MaintenanceItemFormScreen.kt`, every field uses the classic Material 3
`OutlinedTextField(value, onValueChange, label = ...)`. That overload starts the
label centred inside the input box and animates it up into the outline notch
only once the field is focused or non-empty. Two consequences:

- The form's fields visually jump around as the user tabs through them.
- There is nowhere to show an example value. The `placeholder` slot on that
  overload is only revealed while the label is floating, so a hint added there
  would be invisible in the exact state where a new user needs it (unfocused and
  empty).

## The fix

Move the screen's text fields to the Material 3 `TextFieldState` overload of
`OutlinedTextField`, which is the only one that accepts `labelPosition`. Pass
`TextFieldLabelPosition.Attached(alwaysMinimize = true)` so the label is drawn in
the outline from first frame, and use the now always-visible `placeholder` slot
for the example value.

- The API exists in the pinned Compose BOM. `composeBom = "2026.02.01"` resolves
  `androidx.compose.material3` to 1.4.0, which ships `TextFieldLabelPosition`,
  `TextFieldLabelPosition.Attached`, and the `TextFieldState` overloads of
  `OutlinedTextField` and `OutlinedTextFieldDefaults.decorator`. No new
  dependency and no version catalog change.
- Keep `MaintenanceItemFormViewModel` and its `String` based
  `MaintenanceItemFormFields` exactly as they are. The private field composables
  own a `TextFieldState` internally and bridge it back to the existing
  `(String) -> Unit` callbacks, so validation, saving, and
  `MaintenanceItemFormValidatorTest` are untouched.
- The bridge is safe here because the ViewModel never rewrites field text after
  load: the editing values arrive once in `init`, and the form body is only
  composed after `isLoading` flips to false.
- Apply the same treatment to the read-only fields (the unit dropdown and the
  last done date) so the whole form reads as one set of controls, not two styles.
- Placeholder strings go in `app/src/main/res/values/strings.xml`, never inline
  in a composable.

**Must not break**

- Error state, `supportingText`, and the `semantics { error(...) }` accessibility
  wiring on every field.
- The numeric keyboard on the mileage, interval, and odometer fields, and the
  `ImeAction.Next` / `ImeAction.Done` chain.
- `ExposedDropdownMenuBox` behaviour: the unit field stays read-only, keeps its
  trailing chevron, and keeps opening the menu from `menuAnchor`.
- The date picker dialog, its "Not set" display, and the clear-date button.
- Both `@Preview`s still render, including the errors preview.

**Confirmed at review:** "Time between services (optional)" and "Remind me every"
both use `5`, paired with a `Months` unit placeholder. "Miles between services
(optional)" keeps `5000`, since a mileage interval reads better that way.

## Placeholders

| Field | String key | Placeholder |
| --- | --- | --- |
| Name of service | `item_name_placeholder` | `Oil change` |
| Miles between services (optional) | `item_mileage_interval_placeholder` | `5000` |
| Time between services (optional) | `item_recurrence_value_placeholder` | `5` |
| Remind me every | `item_reminder_value_placeholder` | `5` |
| Unit | `item_unit_placeholder` | `Months` |
| Date last done (optional) | reuse `item_date_not_set` | `Not set` |
| Odometer at last done (optional) | `item_last_done_mileage_placeholder` | `42000` |

## Build steps

1. [x] **Add the placeholder strings.** Add the six new keys above to
   `app/src/main/res/values/strings.xml`, next to the existing `item_*` entries.
   *Done when* `./gradlew assembleDebug` succeeds and each key resolves from
   `R.string`.

2. [x] **Convert the editable fields.** Rework the private `ItemTextField` and the
   value field inside `IntervalRow` to the `TextFieldState` overload with
   `labelPosition = TextFieldLabelPosition.Attached(alwaysMinimize = true)` and a
   `placeholder` parameter. Each helper holds its state with
   `rememberTextFieldState(initialValue)` and forwards edits to the existing
   `onValueChange` via `snapshotFlow { state.text.toString() }` in a
   `LaunchedEffect`, so the ViewModel contract is unchanged. Carry over
   `isError`, `supportingText`, `singleLine` (as
   `lineLimits = TextFieldLineLimits.SingleLine`), the numeric keyboard, the IME
   action, and the error semantics.
   *Done when* `./gradlew assembleDebug lintDebug` is clean and both previews in
   the file render with labels in the outline and placeholders visible in the
   empty fields.

3. [x] **Convert the read-only fields.** Apply the same overload and
   `alwaysMinimize` label to the unit dropdown in `IntervalRow` and to the
   date field in `LastDoneDateField`, keeping them `readOnly` and re-syncing
   their `TextFieldState` from the selected unit or date. Give the unit field
   the `Months` placeholder and the date field the `Not set` placeholder instead
   of forcing "Not set" in as a value.
   *Done when* `./gradlew assembleDebug lintDebug` is clean, the dropdown still
   opens and selects, and the date picker still sets and clears the date.

4. [x] **Lay the form out in a single column.** Not in the original spec, added
   after on-device evidence. Once the labels stopped floating, "Time between
   services (optional)" and "Date last done (optional)" no longer fit the notch
   of a half-width field at the default 360dp width and font scale 1.0, so they
   wrapped out of the outline. `IntervalRow` became `IntervalFields`, a `Column`
   of a full-width value field over a full-width unit dropdown, and
   `LastDoneDateField` puts its field full width with the picker and clear
   buttons on the line below. Layout only, no copy change.
   *Done when* every label renders on one line inside its outline on device.

## Verify

Ran and passing:

| Command | Result |
| --- | --- |
| `./gradlew assembleDebug lintDebug testDebugUnitTest` | exit 0, BUILD SUCCESSFUL, 73 tests, 0 failures, 16 lint results all pre-existing and none in the changed files |

On-device evidence, physical device `FCAE637F` at 1080x2340, density 480
(360dp wide), font scale 1.0, `verification.uiEvidence` is `when-available`:

| Check | Result |
| --- | --- |
| Add item, untouched | Every label sits in its outline notch on one line; every empty box shows its placeholder |
| Type into Name of service | Placeholder clears, label does not move, the `Required` error clears live, proving the `TextFieldState` bridge reaches the ViewModel |
| Save while empty | `Required` shows under Name of service and Remind me every; labels stay put |
| Unit dropdown | Opens, lists Days/Weeks/Months/Years with no `Not set` on the required reminder unit, and the read-only field re-syncs to the selection |
| Date picker | Sets `2026-09-01` into the field and the clear button appears |
| Round trip | Saved `Brake fluid` / 3 Years / 2026-09-01, reopened in Edit item with every value loaded and no placeholder on a filled field, then deleted |
| Rotation | Typed `Coolant flush` survived a portrait to landscape rotation with the label still pinned |
| Themes | Correct in both dark and light mode |

Not run: `./gradlew connectedDebugAndroidTest`. The project has only scaffold
instrumented tests, and this change added no logic to cover.

Manual path: open a vehicle, tap **Add item**, and confirm every label sits in
the outline with an example value greyed inside each empty box.

## Known consideration

On the Unit field the placeholder `Months` is distinguishable from a real
selection only by text color. The same holds in **Edit item**, where an unset
optional field shows a grey `5000` or `42000` that reads as a value at a glance.
This is the standard Material placeholder treatment and is what was asked for,
but it is worth revisiting if it ever misleads.
