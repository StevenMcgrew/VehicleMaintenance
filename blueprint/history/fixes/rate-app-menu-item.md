# Rate app menu item

**Type:** Fix
**Status:** verified
**Branch:** fix/rate-app-menu-item

Change 3 of 6 from `blueprint/context/tester-feedback-plan.md`.

## The problem

Play testers noted there is no "Rate this app" option, which limits ratings and
reviews on the Play listing. Ratings matter most while the app is new.

## The fix

- `openPlayStoreListing(context)` in `playstore/PlayStoreListing.kt`, beside
  `shareApp`:
  - First tries `market://details?id=<PLAY_STORE_APP_ID>` as an `ACTION_VIEW`
    intent pinned to the Play Store app (`com.android.vending`), so it opens
    the listing directly where the user can tap the stars.
  - If the Play Store app is missing (`ActivityNotFoundException`), falls back
    to `PLAY_STORE_LISTING_URL` in a browser.
  - If nothing can open either, shows a short toast ("No app found to open the
    Play Store") instead of failing silently.
  - Uses `PLAY_STORE_APP_ID`, never `context.packageName`, so the debug build
    opens the real listing.
- **Rate app** menu item after Share app in `VehicleListMenu`, in the brand
  blue for tappable text. Tapping it closes the menu and opens the listing.
  Same wiring as Share app: the menu gets the context itself, with no new
  parameters through `VehicleListScreen` or navigation.
- No pop-up rating prompt after logging a service and no Play In-App Review
  library. That was decided in the plan: it adds a dependency, Google may
  silently skip the prompt, and an interruption after logging works against the
  app's main task.
- No new permission. Opening the Play Store sends nothing from the user's
  records, so the privacy policy and data safety form are unchanged.

Must not break: Theme, Backup data, Privacy, and Share app items and their order.

## Build steps

- [x] 1. **Rate app item.** `openPlayStoreListing` with the market URI constant,
   the label and toast strings, and the menu item. Add a JVM unit test that the
   market URI uses the release application ID, and a device test that the menu
   shows Rate app after Share app.
   Done when: tapping the three dots, then Rate app, opens this app's Play Store
   listing (or its web page when the Play Store app is absent), and Back
   returns to the vehicle list.

## Verify

- `./gradlew testDebugUnitTest lintDebug` pass with no new lint warnings.
- Menu order is Theme, Backup data, Privacy, Share app, Rate app; Rate app is
  brand blue.
- On the emulator (which has the Play Store), Rate app opens the Play Store on
  `com.stevenmcgrew.vehiclemaintenancetrackerapp`, not the `.debug` package.
  While the listing is limited to testers, the page may say the item is not
  found for an account outside the test track; the right package in the URL is
  what this check confirms.
- Back from the Play Store returns to the vehicle list.
