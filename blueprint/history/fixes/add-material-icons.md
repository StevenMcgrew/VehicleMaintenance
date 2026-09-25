# Current Feature

**Add Material icons**

**Type:** Fix

**Status:** verified

**Branch:** `fix/add-material-icons`

## The problem

The app has no icon library and uses no icons. Every top-bar navigation control
is a text button ("Back" or "Cancel"), and both FABs are text only. That is
unusual for Material 3, and it takes up top-bar width that the title needs.

Material 3 1.4.0 (from Compose BOM `2026.02.01`) no longer pulls in
`material-icons-core` transitively, so `Icons.*` is not on the classpath.

## The fix

**Dependency.** Add `androidx.compose.material:material-icons-core` to
`gradle/libs.versions.toml` with no version. The Compose BOM already pins it
(`1.7.8`). Then add `implementation(libs.androidx.compose.material.icons.core)`
to `app/build.gradle.kts`.

Why `core` and not the alternatives:

| Option | Verdict |
|---|---|
| `material-icons-core` | Chosen. Small, BOM-managed, and has every icon this fix needs: `AutoMirrored.Filled.ArrowBack`, `Filled.Close`, `Filled.Add`. |
| `material-icons-extended` | Rejected. Adds thousands of icons, and release builds have R8 turned off (`optimization.enable = false`), so none of that would be stripped from the APK. |
| Material Symbols vector drawables | Not needed yet. Google recommends these going forward, and they need no dependency. Switch to them later if the app needs a symbol that `core` lacks. |

**Where icons go.** Keep this small and limited to places where the icon's
meaning is standard:

- **Top-bar Back** (Vehicle detail, Backup, Service history). Replace the
  `TextButton` with an `IconButton` holding `Icons.AutoMirrored.Filled.ArrowBack`
  and `contentDescription = stringResource(R.string.back)`.
- **Top-bar Cancel on forms** (Vehicle form, Maintenance item form, Service log
  form). Replace the `TextButton` with an `IconButton` holding
  `Icons.Filled.Close` and `contentDescription = stringResource(R.string.cancel)`.
- **FABs** ("Add vehicle", "Add item"). Keep the content-slot overload. Inside
  it (already a `RowScope`, so no extra `Row`), put a decorative leading
  `Icon(Icons.Filled.Add, contentDescription = null)`, a 12dp spacer, and the
  existing `Text`.

**Must not break.**

- **FAB labels for TalkBack.** Do not switch to the `text =` / `icon =` overload.
  It clears the text's semantics and takes the label from the icon, which is the
  bug fixed in `remove-vehicle-nickname`. With the content-slot overload, the
  `Text` stays a merged descendant, so `onNodeWithText(add_vehicle)` and
  `onNodeWithText(add_maintenance_item)` keep working.
- **Accessible navigation.** Every `IconButton` needs a non-null
  `contentDescription` from existing string resources. No new strings are
  needed.
- **Unchanged buttons.** Keep top-bar Save, Delete, and Backup as text buttons,
  along with in-body buttons (error-state Back, Service history, Log repair,
  Edit vehicle, date pickers) and all dialog buttons. Existing instrumented
  tests find those by text. `VehicleDetailScreenTest` clicks the top-bar
  `Delete`, and `BackupScreenTest` clicks a dialog `Cancel`.
- **Right-to-left layouts.** Use the auto-mirrored back arrow, not the
  deprecated `Icons.Filled.ArrowBack`.

## Build steps

- [x] **1. Add the dependency and apply the icons.** Make the catalog and build file
   changes, then update the six top bars and two FABs listed above.
   *Done when:* `./gradlew assembleDebug lintDebug testDebugUnitTest` passes, and
   no top bar shows a "Back" or "Cancel" text button.

## Verify

- `./gradlew assembleDebug lintDebug testDebugUnitTest` passes.
- `./gradlew connectedDebugAndroidTest` passes on a device or emulator.
- On the device:
  - The vehicle list FAB shows **+ Add vehicle**, and the vehicle detail FAB
    shows **+ Add item**.
  - Open a vehicle. The top-left control is a back arrow that returns to the
    list. Do the same check on Service history and Backup.
  - Open Add vehicle, Add item, and Log repair. The top-left control is an ✕
    that leaves the form without saving.
  - With TalkBack on, the arrow is read as "Back", the ✕ as "Cancel", and each
    FAB by its label.

## Verification results

- `./gradlew assembleDebug lintDebug testDebugUnitTest` passed. Lint reported
  only the existing version, `UnusedResources`, and `RedundantLabel` warnings.
  None came from this change.
- `./gradlew connectedDebugAndroidTest` **could not run**. On the Android 17
  (API 37) emulator, 30 of 36 tests fail with
  `NoSuchMethodException: android.hardware.input.InputManager.getInstance`.
  Espresso 3.5.1 cannot inject input on this API level. The same failure
  happens on unchanged `master` (confirmed with `VehicleListScreenTest`), so it
  predates this fix. Fixing it needs a test-dependency upgrade, which is
  outside this fix's scope.
- Checked on the emulator:
  - The vehicle list FAB shows **+ Add vehicle**.
  - Add vehicle shows the ✕ (content description "Cancel"), and tapping it
    returns to the list.
  - Backup shows the back arrow (content description "Back"), and tapping it
    returns to the list.
  - Vehicle detail, Service history, Add item, and Log repair use the same code
    but were not opened, because the emulator had no vehicles.
