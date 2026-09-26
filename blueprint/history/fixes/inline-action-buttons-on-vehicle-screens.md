# Inline action buttons on vehicle screens

**Type:** Fix
**Status:** verified
**Branch:** fix/inline-action-buttons-on-vehicle-screens

## The problem

Both vehicle screens still use a floating action button for their primary "add"
action, while the other actions sit elsewhere in a different style:

- **Vehicle detail** (`maintenance/VehicleDetailScreen.kt`): "Add item" is an
  `ExtendedFloatingActionButton`, separate from the extra-small "Service
  history" and "Log repair" buttons in `VehicleActionsRow`. "Log repair" also
  undersells what the button is for.
- **Vehicle list** (`vehicles/VehicleListScreen.kt`): "Add vehicle" is an
  `ExtendedFloatingActionButton`, and backup is an import/export icon in the top
  bar (with a long-press tooltip) whose purpose is not obvious.

## The fix

Replace both FABs with one row of extra-small rounded buttons per screen, in the
same style the detail screen already uses.

**Shared button**

- Move the private `ExtraSmallButton` out of `VehicleDetailScreen.kt` into a
  shared composable (for example `ui/ExtraSmallButton.kt`) so both screens use
  the same 32dp, fully round `FilledTonalButton`.
- Add an optional leading icon parameter. The icon is decorative
  (`contentDescription = null`) because the label already names the action, and
  it is sized to the Expressive extra-small spec (20dp) with a small gap before
  the label.

**Vehicle detail**

- Remove the FAB. `VehicleActionsRow` shows, in order: **Add item** (plus icon),
  **Service history**, **Log additional repairs**.
- Swap the row's `horizontalScroll` for a `FlowRow` so a button that no longer
  fits wraps onto a second line underneath instead of scrolling off screen. Keep
  the existing padding and 8dp spacing, with 8dp between wrapped lines.
- Change the `log_repair` string value to "Log additional repairs". Leave
  `log_repair_title` (the form's screen title) as "Log repair".
- "Add item" keeps its current visibility: the row only appears once the vehicle
  has loaded, which matches when the FAB showed.

**Vehicle list**

- Remove the FAB and the top-bar import/export icon, its tooltip, and the
  explanatory comment. The top bar keeps only its title.
- Add a `FlowRow` of extra-small buttons below the top bar, above the list and
  its empty/loading states, followed by a `HorizontalDivider` to match the
  detail screen: **Add vehicle** (plus icon), then **Backup data** (no icon).
- Change the `backup_action` string value to "Backup data". Leave `backup_title`
  ("Export and import") as the backup screen's title.
- Preserve current visibility: "Add vehicle" hides when the load failed, exactly
  as the FAB did. "Backup data" stays available in every state (including a
  failed load), as the top-bar icon was, since restoring a backup is a recovery
  path.
- Delete `res/drawable/ic_import_export.xml` once nothing references it.

**Must not break**

- The "Add item" and "Add vehicle" labels stay merged text for TalkBack (the
  existing tests find them by text).
- Buttons keep the 48dp minimum touch target.
- Scaffold inner padding and edge-to-edge insets are still respected.
- Light and dark mode both render correctly.

## Build steps

1. [x] **Shared button and vehicle detail row.** Extract `ExtraSmallButton` with an
   optional icon, move "Add item" into a wrapping `VehicleActionsRow` first,
   remove the detail FAB, and relabel "Log additional repairs".
   *Done when:* the detail screen shows no FAB, the row reads Add item, Service
   history, Log additional repairs, and at a large font scale the last button
   wraps onto a second line. Previews and `VehicleDetailScreenTest` /
   `AdHocRepairTest` still compile.
2. [x] **Vehicle list row.** Replace the list FAB and top-bar backup icon with the
   wrapping row, relabel backup to "Backup data", delete the unused drawable,
   and update `VehicleListScreenTest.exportAndImportIconOpensBackup` to find the
   button by text (renamed to match).
   *Done when:* the list screen shows Add vehicle then Backup data under the top
   bar, no FAB, no top-bar icon, and tapping Backup data opens the backup screen.

## Verify

- `./gradlew assembleDebug lintDebug testDebugUnitTest` passes with no new lint
  warnings.
- `./gradlew connectedDebugAndroidTest` passes on an emulator, including the
  updated vehicle list backup test.
- On device, in light and dark mode:
  - Vehicle list: Add vehicle (plus icon) and Backup data sit in a row under the
    title; both work; no FAB or top-bar icon remains.
  - Vehicle detail: Add item (plus icon), Service history, Log additional
    repairs, in that order; all three open the right screen; no FAB.
  - At the largest system font size, buttons that don't fit wrap onto a second
    row instead of clipping or scrolling.
