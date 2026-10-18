# Vehicle list menu with a theme choice

**Type:** Fix
**Status:** verified
**Branch:** fix/vehicle-list-menu-with-a-theme-choice

Change 1 of 6 from `blueprint/context/tester-feedback-plan.md`.

## The problem

Play testers reported "the app lacks a dark mode option." The app already has a
complete dark color scheme and follows the phone's system setting, but there is
no in-app control, so anyone on a light-mode phone never sees it.

The vehicle list also carries Backup data and Privacy as buttons in its actions
row. With Theme, Share app, Rate app, and Help coming, those belong together in
one overflow menu instead of a growing row of buttons.

Separately, `res/values/themes.xml` parents `android:Theme.Material.Light.NoActionBar`
with no night variant, so on a dark-mode phone the launch window shows white
before Compose draws.

## The fix

- `ThemeMode` enum: `SYSTEM` (default), `LIGHT`, `DARK`.
- `SettingsRepository` interface plus an implementation over a private
  `SharedPreferences` file, exposing the mode as a `StateFlow` and loading it
  off the main thread. Kept out of the JSON store on purpose: it is a device
  preference, not vehicle history, and must not travel in export files.
- `MainActivity` collects the mode and passes `darkTheme` to
  `VehicleMaintenanceTheme` (`SYSTEM` keeps `isSystemInDarkTheme()`). Status
  and nav bar icon contrast follow the chosen mode.
- A three-dot (`Icons.Filled.MoreVert`) button in the vehicle list `TopAppBar`
  actions, top right, opening a `DropdownMenu`. Final order once all four
  changes land:
  1. **Theme** - a label with a Material 3 `SingleChoiceSegmentedButtonRow`
     under it: `System | Light | Dark`. This is the three-way switch. Tapping a
     segment applies the theme at once and leaves the menu open so the change
     is visible; tapping outside closes it.
  2. **Backup data** - opens the existing Backup screen.
  3. **Privacy** - opens the existing Privacy screen.
  4. Share app, Rate app, Help - added by changes 2 to 4.
- Remove the Backup data and Privacy buttons from `VehicleListActionsRow`; it
  keeps only Add vehicle. The menu lives in the top bar, so Backup and Privacy
  stay reachable when the list fails to load (the reason they were always shown).
- Add `values-night/themes.xml` parenting `android:Theme.Material.NoActionBar`
  so the launch window matches a dark system.
- Brand colors, matching the rest of the app: the selected theme segment is
  filled like the Add vehicle button and the others are outlined in the brand
  primary; Backup data and Privacy use the brand primary text that marks
  tappable items. The Theme label stays plain because it is not tappable.
- The menu button has a content description ("More options"); new labels go in
  `strings.xml`, reusing `backup_action` and `privacy_action`.

Fallback if the segmented row clips at the largest font scale: three
`DropdownMenuItem`s with a `RadioButton` leading icon (System default, Light,
Dark) under the Theme label. Same behavior, taller menu.

Must not break: status and brand colors in either mode, the reminder deep link
into vehicle detail, export/import file contents (no new fields).

## Build steps

- [x] 1. **Preference and theme wiring.** `ThemeMode`, `SettingsRepository`, its
   SharedPreferences implementation, wiring in the application and
   `MainActivity`, and the night window theme.
   Done when: the app still follows the system by default, a unit test covers
   `ThemeMode` -> dark/light resolution, and a dark-mode phone no longer flashes
   white on launch.
- [x] 2. **Overflow menu.** The three-dot button and dropdown with Theme, Backup data,
   and Privacy; remove the two old buttons; update `VehicleListScreenTest` and
   `PrivacyScreenTest` to open the menu before tapping Backup data or Privacy,
   and add a test that selecting a theme segment reports the new mode.
   Done when: the actions row shows only Add vehicle, the menu opens from the
   top right, Backup data and Privacy navigate as before, and choosing System,
   Light, or Dark recolors every screen immediately and survives force-stopping
   and reopening the app.

## Verify

- `./gradlew testDebugUnitTest lintDebug` pass with no new lint warnings.
- On an emulator in light mode: tap the three dots, choose Dark. The whole app
  turns dark; vehicle detail, forms, dialogs, history, backup, and privacy
  screens all read correctly.
- Force-stop and reopen: still dark. Choose System: back to light.
- System in dark mode with the app on System: app follows; cold start shows no
  white flash.
- With Android's Display size and text set to the largest font: the Theme row fits.
- With the list in its load-failed state, the menu still opens Backup data.
- Export a backup and confirm the file has no theme field.
