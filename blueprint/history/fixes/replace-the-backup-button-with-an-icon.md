# Current Feature

**Replace the Backup button with an icon**

**Type:** Fix

**Status:** verified

**Branch:** `fix/replace-the-backup-button-with-an-icon`

## The problem

The vehicle list top bar (`VehicleListScreen.kt`, `actions` slot) shows a text
button labelled **Backup**. It opens the "Export and import" screen. The rest of
the top bars now use icons, so this text button looks out of place. The word
"Backup" also only describes half of what the screen does, because the screen
imports data too.

A save icon has the same problem: it suggests saving, not restoring. Also,
`material-icons-core` has no save, backup, or import/export icon.

## The fix

**Icon choice: `import_export` (↑↓).** This Material Symbol shows an up arrow
and a down arrow side by side. It is the standard Material icon for "move data
in and out". It also matches the destination screen's title, "Export and
import".

| Candidate | Verdict |
|---|---|
| `import_export` ↑↓ | **Recommended.** Covers both directions and matches the screen title. |
| `save` 💾 | Rejected. Suggests export only, and reads like "save this form". |
| `backup` / `cloud_upload` | Rejected. The cloud suggests an online sync, but this app writes a local file. |
| `settings_backup_restore` ↺ | Possible. Android Settings uses it for "Backup & restore", but most people read it as "undo" or "reset". |
| ⋮ overflow menu with an "Export and import" item | Possible. Clearest text, but it takes two taps and the menu would hold only one item. |

**Icon source.** Following the `add-material-icons` fix, use a Material Symbols
vector drawable instead of adding `material-icons-extended`, which would add
thousands of icons to the APK because R8 is off. Add
`app/src/main/res/drawable/ic_import_export.xml`. Use Material Symbols Outlined
`import_export` at weight 400, fill 0, and 24dp, with a 24×24 viewport. It is
Apache 2.0 licensed. No new dependency is needed.

**Top-bar change.** In `VehicleListScreen.kt`, replace the `TextButton` with an
`IconButton(onClick = onOpenBackup)`. Inside it, add
`Icon(painterResource(R.drawable.ic_import_export), contentDescription = …)`.

**Label.** Change the value of `backup_action` in `strings.xml` from `Backup` to
`Export and import`. This string becomes the icon's content description, so
TalkBack reads what the button does.

**Tooltip.** Because an icon is less obvious than a word, wrap the `IconButton`
in a Material 3 `TooltipBox` with a `PlainTooltip` that shows the same
`backup_action` text. Long-pressing the icon then shows "Export and import".
The screen already opts in to `ExperimentalMaterial3Api`.

**Must not break.**

- Tapping the icon still opens the Export and import screen.
- `VehicleListScreenTest` does not find the Backup button by its text, so no
  existing test depends on the word "Backup".
- Leave the FAB, the delete controls, and the Backup screen itself unchanged.
- The icon must follow the theme color. `Icon` tints the drawable, so the vector
  needs no hardcoded color that clashes in dark mode.

## Build steps

- [x] **1. Swap the text button for the icon button.** Add the drawable, update
   the string, and replace the top-bar action with the `IconButton` inside a
   `TooltipBox`. In `VehicleListScreenTest`, add a test that finds the node by
   the `backup_action` content description, clicks it, and asserts that
   `onOpenBackup` ran.
   *Done when:* `./gradlew assembleDebug lintDebug testDebugUnitTest` passes,
   and the vehicle list top bar shows the ↑↓ icon with no "Backup" text.

## Verify

- `./gradlew assembleDebug lintDebug testDebugUnitTest` passes.
- `./gradlew connectedDebugAndroidTest` passes on a device or emulator, including
  the new top-bar test.
- On the device:
  - The vehicle list's top-right control is a ↑↓ icon, not "Backup".
  - Tapping it opens **Export and import**. The back arrow returns to the list.
  - Long-pressing it shows the tooltip "Export and import".
  - In dark mode, the icon matches the other top-bar icons.
  - With TalkBack on, the icon is read as "Export and import".

## Verification results

- `./gradlew assembleDebug lintDebug testDebugUnitTest` passed. Lint reported
  0 errors and 18 warnings. None of the warnings are in the changed files.
- `./gradlew connectedDebugAndroidTest` passed on the Pixel 10a emulator
  (API 37): 37 tests, 0 failures. That includes the new
  `VehicleListScreenTest.exportAndImportIconOpensBackup`.
- Checked on the emulator:
  - The vehicle list's top-right control is the ↑↓ icon, with no "Backup" text.
  - Long-pressing it shows the "Export and import" tooltip below the icon.
  - In dark mode, the icon uses the theme's light tint.
  - Tapping it opens **Export and import**.
  - TalkBack was not turned on. The content description is covered by the
    instrumented test, which finds the button by that description.
- `./gradlew` needs `JAVA_HOME=/opt/android-studio/jbr` on this machine,
  because no `java` is on `PATH`.
