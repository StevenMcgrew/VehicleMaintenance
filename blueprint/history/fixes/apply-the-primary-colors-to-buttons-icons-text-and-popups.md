# Current Feature

## Apply the primary colors to buttons, icons, text, and popups

**Type:** Fix
**Status:** verified
**Branch:** fix/apply-the-primary-colors-to-buttons-icons-text-and-popups

### The problem

The user defined two brand colors in `ui/theme/Color.kt`: `PrimaryColorLightMode`
(`#007AFF`) and `PrimaryColorDarkMode` (`#0A84FF`). This rename is still uncommitted
and belongs to this fix. Nothing uses the colors yet. The app still draws with the
Material scheme defaults:

- **Extra small buttons** (`ui/ExtraSmallButton.kt`) are a `FilledTonalButton`, so
  they use `secondaryContainer` for the background and `onSecondaryContainer` for the
  text. This covers Add vehicle, Backup, Add item, Service history, and Log repair.
- **Vehicle list** year, make, model, and engine headline
  (`vehicles/VehicleListScreen.kt`, `VehicleRow`) uses the default `onSurface`.
- **Icons in icon buttons** use the default content color (`onSurfaceVariant`).
- **Popups** use Material's `surfaceContainerHigh` (dialogs) and `surfaceContainerLow`
  (bottom sheet), which are blue-tinted greys. Affected:
  - Seven `AlertDialog`s: delete vehicle, delete item, update mileage, lower mileage
    warning, newly overdue, confirm import, and average info.
  - The `DatePickerDialog` (`FormDatePickerDialog`).
  - The item actions `ModalBottomSheet`.
- **"Update" mileage** `TextButton` (`maintenance/LastRecordedMileage.kt`) uses the
  scheme's `primary` (`#0062CC` light, `#A3C9FE` dark).
- **Maintenance item names** in the vehicle detail table (`MaintenanceTableRow`)
  use the default `onSurface`.
- **"Choose" date button** (`ui/FormTable.kt`, `ChooseDateButton`) is an
  `OutlinedButton`.
- **Item actions sheet**: "Log a completed service", "Edit item", and "Delete item"
  are plain `Text` rows (`SheetAction`) in the default `onSurface`.
- **Dialog text buttons** (OK, Cancel, Delete, Save, Import, Clear) use the scheme's
  `primary`, not the brand color. This covers every `AlertDialog` and the date picker.
- **Unit dropdown arrow** (`MaintenanceItemFormScreen.kt`, `UnitDropdown`) uses the
  text field's default trailing icon color.
- **Retry button** on the vehicle list load error screen is a default `Button`.
- **Backup screen**: the Export `Button` and the Import `OutlinedButton` use the
  scheme's `primary`.
- **Reminder notification** (`reminders/ReminderNotifier.kt`) sets no accent color,
  so the system draws its small icon in its default grey.

### The fix

Add theme-aware brand colors the same way `StatusColors` works, so no screen
hardcodes a hex value. This follows the coding standard that colors come from the
theme.

1. **`ui/theme/Color.kt`**: keep `PrimaryColorLightMode` and `PrimaryColorDarkMode`.
   Add two popup shades:
   - `PopupColorLightMode = Color.White`
   - `PopupColorDarkMode = Color(0xFF1C1C1E)`, a neutral near-black that pairs with
     the iOS-style blues

   Fix the stale comment that still points at `PrimaryColor_RoundedButton`.
2. **New `ui/theme/BrandColors.kt`**: add an `@Immutable data class BrandColors(primary,
   onPrimary, popupContainer)`, a `LightBrandColors` and a `DarkBrandColors` value,
   and a `LocalBrandColors`. `onPrimary` is `Color.White` in both modes.
   `VehicleMaintenanceTheme` provides it next to `LocalStatusColors`, based on
   `darkTheme`.
3. **Apply the colors at each named spot:**

   | Where | Change |
   | --- | --- |
   | `ExtraSmallButton` | Background is `primary` and content is white, set through `ButtonDefaults.filledTonalButtonColors`. The Add icon turns white with the label. |
   | `ChooseDateButton` | Stays an `OutlinedButton`, with `primary` text and border. The error semantics and the error text stay unchanged. |
   | `VehicleRow` headline | Text color is `primary`. |
   | `MaintenanceTableRow` item name | Text color is `primary`. The interval summary under it stays `onSurfaceVariant`. |
   | `LastRecordedMileageRow` "Update" | `TextButton` content color is `primary`. |
   | `SheetAction` (Log a completed service, Edit item, Delete item) | Text color is `primary`. |
   | Every `IconButton` icon | Tint is `primary`. This covers edit and delete on the vehicle list, back arrows, close and cancel, save, delete on the item form, and the info icon on service history. |
   | All `AlertDialog`s, `DatePickerDialog`, the item actions `ModalBottomSheet`, and the unit `ExposedDropdownMenu` | Set `containerColor` to `popupContainer`. The bottom sheet also sets `contentColor` to `onSurface`, because Material cannot infer a text color for a color outside its scheme and falls back to black. For the date picker, set it through `DatePickerDefaults.colors` on both the dialog and the `DatePicker`, so the calendar body matches. |
   | Every dialog `TextButton` (OK, Cancel, Delete, Save, Import, Clear) | Content color is `primary`. Add one small shared helper in `ui/` for the brand text button colors, so the seven dialogs and the date picker don't repeat the setup. |
   | Unit dropdown arrow | Set `focusedTrailingIconColor` and `unfocusedTrailingIconColor` to `primary` through `OutlinedTextFieldDefaults.colors`. The error trailing color stays red. |
   | Vehicle list Retry `Button` | Background is `primary` and text is white. |
   | Backup Export `Button` | Stays filled. Background is `primary` and text is white. |
   | Backup Import `OutlinedButton` | Stays outlined. Text and border are `primary`. The border drops to the disabled alpha while a backup runs. |
   | Reminder notification | Call `setColor` with `PrimaryColorLightMode` or `PrimaryColorDarkMode`, picked from the device's night mode (`Configuration.UI_MODE_NIGHT_MASK`) and converted with `toArgb()`. There is no Compose theme in a worker, so it reads the `Color.kt` values directly. |

Must not break:

- **Disabled state.** The save icon and the other icons disabled by `actionsEnabled`
  must still look disabled. Tint through `IconButtonDefaults.iconButtonColors(contentColor
  = primary)`, not `Icon(tint = ...)`, so the disabled alpha still applies. The same
  goes for disabled button colors: the mileage dialog Save while saving, and Export
  and Import while a backup runs.
- **Semantics.** Content descriptions, error semantics, and the 48dp touch targets stay
  the same.
- **Status colors.** The status column keeps its red, amber, and green colors.
- **Material scheme.** The scheme itself does not change. The Back fallback
  `TextButton`s on the not-found screens are not in dialogs, so they keep the Material
  `primary`.
- **Notification content.** The notification title, text, tap target, and
  auto-cancel stay the same.

Notification limits:

- Android decides how much the accent color shows. On most versions it tints the
  small icon and app name in the header. Some launchers and OEM skins ignore it.
- The color is chosen when the reminder is posted. A notification already in the
  shade does not recolor if the device switches between light and dark mode.

### Contrast note

White text on `#007AFF` has about a 4.0:1 contrast ratio, and on `#0A84FF` about
3.6:1. Both are below the WCAG AA 4.5:1 target for 14sp labels. The same applies to
`#007AFF` text on a white background. These are the colors you chose, so the fix uses
them as given. If contrast matters, a darker light-mode blue such as `#0066D6` would
pass.

### Build steps

- [x] 1. **Brand color tokens and filled or background surfaces.** Add the popup colors,
   `BrandColors`, and the theme provider. Restyle `ExtraSmallButton`,
   `ChooseDateButton`, the Retry button, and the Backup Export and Import buttons.
   Set the popup container on every dialog, the date picker, the bottom sheet, and the
   unit dropdown menu.
   *Done when:* `./gradlew assembleDebug lintDebug testDebugUnitTest` passes. All
   filled buttons listed above are blue with white text in both modes, Import is a
   blue outlined button, and every popup is white in light mode and near-black in
   dark mode.
- [x] 2. **Primary text and icon colors.** Color the following with `primary`, and keep
   the disabled appearance:
   - The vehicle list headline and the maintenance item names.
   - The "Update" mileage button and the three sheet actions.
   - Every dialog text button.
   - Every `IconButton` icon and the unit dropdown arrow.

   *Done when:* the same Gradle checks pass. Each listed text and icon shows the
   primary blue in both modes, and the disabled save icon on an unchanged form still
   looks dimmed.
- [x] 3. **Notification accent color.** Set the reminder notification's color from the
   primary color for the current night mode.
   *Done when:* the same Gradle checks pass. `DueReminderWorkerTest` is an
   instrumented test, so run it on the emulator only after backing up the app data.

### Verify

Run `./gradlew installDebug` and check each screen in light mode, then again with
the system dark theme on:

- **Vehicle list**
  - Add vehicle and Backup are blue pills with white text.
  - The year, make, model, and engine line is blue.
  - The edit and delete icons are blue.
- **Delete vehicle dialog**
  - The background is white in light mode and near-black in dark mode.
- **Vehicle detail**
  - Add item, Service history, and Log repair are blue pills.
  - The back arrow and "Update" are blue.
  - The item names are blue.
- **Update mileage and newly overdue dialogs**
  - Both use the popup background.
- **Item action sheet**
  - The sheet uses the popup background.
  - "Log a completed service", "Edit item", and "Delete item" are blue.
  - In dark mode, the sheet title and the detail values are near-white, not black.
- **Add or edit item form, and the log service or repair form**
  - Close, save, and delete icons are blue.
  - Save is dimmed while disabled.
  - "Choose" is an outlined button with blue text and border.
  - The date picker uses the popup background.
- **Service history and Backup**
  - The back and info icons are blue.
  - The average info dialog and the import confirm dialog use the popup background.
- **Dialog buttons**
  - OK, Cancel, Delete, Save, Import, and Clear are blue in every dialog and in the date picker.
- **Add or edit item form**
  - The unit dropdown arrow is blue, and turns red with a unit error.
  - The open dropdown menu uses the popup background.
- **Backup screen**
  - Export is a solid blue button with white text.
  - Import is an outlined button with blue text and a blue border.
- **Vehicle list error**
  - Retry is blue with white text. To see it, you'd need to force a load failure, so a preview or code review is enough.
- **Reminder notification**
  - Trigger a due reminder. The small icon and app name in the notification header show the primary blue for the current mode, where the device supports it.
