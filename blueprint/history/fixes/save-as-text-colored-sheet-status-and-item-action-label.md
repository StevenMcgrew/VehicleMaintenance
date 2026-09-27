# Current Feature

## Save as text, colored sheet status, and item action label

**Type:** Fix
**Status:** verified
**Branch:** fix/save-as-text-colored-sheet-status-and-item-action-label

### The problem

- **Save is an icon.** Three form top bars show the `ic_save` drawable in an
  `IconButton`, labelled only by its "Save" content description:
  - Add or edit vehicle (`vehicles/VehicleFormScreen.kt`)
  - Add or edit item (`maintenance/MaintenanceItemFormScreen.kt`)
  - Log service or repair (`servicelog/ServiceLogFormScreen.kt`)
- **The sheet's status isn't color coded.** In the item actions sheet
  (`maintenance/VehicleDetailScreen.kt`, `ItemStatusDetail`), the Status value is a
  plain `DetailLine` in the default text color. The table's `StatusCell` colors the
  same label with `LocalStatusColors`:
  - Overdue is red.
  - Due is amber.
  - OK is green.
  - No schedule uses `onSurfaceVariant`.
- **The sheet's first action says "Log a completed service".** The string is
  `item_action_log_service` in `res/values/strings.xml`.

### The fix

1. **Save as text.**
   - In all three forms, replace the save `IconButton` with a `TextButton` that
     reads "Save" (`R.string.save`) and uses `brandTextButtonColors()`, so it stays
     primary.
   - Keep each button's existing `onClick` and `enabled` condition, so it still
     dims while disabled.
   - Delete `res/drawable/ic_save.xml`, which nothing else uses.
   - Close, Delete on the item form, and every other icon stay icons.
2. **Status color in the sheet.**
   - Move the status-to-color `when` out of `StatusCell` into one shared
     `@Composable statusColor(status)` in `VehicleDetailScreen.kt`. The table and
     the sheet then can't drift apart.
   - Give `DetailLine` an optional `valueColor`, defaulting to the current text
     color. Only the Status line passes `statusColor(status.status)`.
   - The colors come from `LocalStatusColors`, so the light and dark shades match
     the table automatically:
     - Light mode: red `Red45`, amber `Amber45`, green `Green45`.
     - Dark mode: red `Red60`, amber `Amber70`, green `Green70`.
   - No schedule stays `onSurfaceVariant`, as in the table.
3. **Label.** Change the `item_action_log_service` string to "Log item as
   completed". The string key and the action it runs stay the same.

Must not break:

- **Save behavior.** Save's click, its disabled rules, and the "Save" name screen
  readers hear stay the same. A text button's label is its accessible name, so
  it needs no separate content description.
- **Instrumented tests.** Several tests find Save by content description:
  `VehicleDetailScreenTest`, `AdHocRepairTest`, and `VehicleListScreenTest`. Update
  those lookups to find it by text instead. The update mileage dialog also has a
  Save text button, but it is never on screen with a form's Save, so a text lookup
  stays unambiguous.
- **Other copy.** The service history empty state still says "Log a completed
  service or a repair…". That's separate copy on another screen, so it stays
  unless you ask.

### Build steps

- [x] 1. **Save text, sheet status color, and label.** Make the three changes
  above, and update the instrumented test lookups for Save.
  *Done when:* `./gradlew assembleDebug lintDebug testDebugUnitTest` and
  `./gradlew compileDebugAndroidTestKotlin` pass, and `ic_save` has no remaining
  references.

### Verify

Run `./gradlew installDebug`, then check each screen in light mode and again in dark
mode:

- **Add or edit vehicle, add or edit item, and log service or repair forms**
  - The top bar shows a blue "Save" text button where the save icon was.
  - Save is dimmed while the form can't be saved, and tapping it still saves.
- **Item sheet on vehicle detail**
  - The Status value is colored like the table's Status column for the same item:
    Overdue red, Due amber, OK green.
  - The first action reads "Log item as completed", and tapping it opens the log
    service form.
- **Instrumented tests** (optional, overwrites emulator app data): back up the
  app data first, then run `./gradlew connectedDebugAndroidTest`.
