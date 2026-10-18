# Share app menu item

**Type:** Fix
**Status:** verified
**Branch:** fix/share-app-menu-item

Change 2 of 6 from `blueprint/context/tester-feedback-plan.md`.

## The problem

Play testers noted there is no way to share the app with someone else. Word of
mouth is the cheapest way for a new app to get installs, and the vehicle list
menu (built in change 1) is where they expect to find it.

## The fix

- New `playstore/PlayStoreListing.kt` holding the release application ID and
  the listing's web URL
  (`https://play.google.com/store/apps/details?id=com.stevenmcgrew.vehiclemaintenancetrackerapp`).
  It is a constant, not `context.packageName`, because debug builds carry the
  `.debug` suffix and would share a link to a listing that does not exist.
  Change 3 (Rate app) reuses it.
- A `shareApp(context)` helper next to it, following the `shareHistoryPdf`
  pattern: an `ACTION_SEND` `text/plain` intent wrapped in
  `Intent.createChooser`, with a pre-written message as `EXTRA_TEXT` and the
  app name as `EXTRA_SUBJECT` (used by email apps).
- Pre-written message, as a string resource with the URL as an argument:
  "I use Vehicle Maintenance Tracker to get service reminders and keep a log of
  every repair and what it cost. It's free, works offline, and needs no account:
  %1$s"
- **Share app** menu item after Privacy in `VehicleListMenu`, in the brand blue
  used for tappable text. Tapping it closes the menu and opens the share sheet.
  The menu gets the context itself, the same way `ServiceHistoryScreen` calls
  `shareHistoryPdf`, so no new parameters go through `VehicleListScreen` or
  navigation.
- No new permission or dependency. Nothing from the user's records goes into
  the message, so the privacy policy and data safety form are unchanged.

Must not break: Theme, Backup data, and Privacy menu items and their order.

## Build steps

- [x] 1. **Share app item.** `PlayStoreListing.kt` with the constants and
   `shareApp`, the message and label strings, and the menu item. Add a JVM unit
   test that the listing URL is built from the release application ID, and a
   device test that the menu shows Share app after Privacy.
   Done when: tapping the three dots, then Share app, opens the Android share
   sheet with the pre-written message and the release Play link, even from the
   debug build.

## Verify

- `./gradlew testDebugUnitTest lintDebug` pass with no new lint warnings.
- Menu order is Theme, Backup data, Privacy, Share app; Share app is brand blue
  in light and dark.
- Share app opens the share sheet. Picking Messages or Gmail pre-fills the
  message ending in the Play link, which points at the release package (no
  `.debug`).
- Backing out of the share sheet returns to the vehicle list with no error.
