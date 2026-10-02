# Current Feature

**Branch:** `feature/play-store-readiness`
**Status:** verified

## Goal

Feature 10, **Play Store readiness**. Make the app something a stranger can install
cold from Google Play. That means a permanent application ID, a real launcher icon
and label, an R8-optimized release build that is signed when the owner's upload
key exists, a privacy policy both inside the app and as a hostable file, and the
data safety answers, listing text, and graphics the Play Console asks for. Nothing
here touches the Play Console itself.

## In scope

- **Application ID** `com.stevenmcgrew.vehiclemaintenancetrackerapp`, which is
  permanent once uploaded. The Kotlin `namespace` and the source packages stay
  `com.example.vehiclemaintenance`.
- **Debug build** gets `applicationIdSuffix = ".debug"` and a debug-only launcher
  label, so debug and release builds can sit on one device without either
  uninstalling the other.
- **Launcher label** `Vehicle Maintenance Tracker App`, set through `app_name`.
- **New adaptive launcher icon** with background, foreground, and monochrome
  layers, replacing the Android template robot.
- **Release signing** read from a gitignored `keystore.properties` when that file
  exists. Without it, the release build is unsigned and still succeeds.
- **R8** code optimization and resource shrinking for the release build type.
- **Android auto-backup stays on.** The template TODO comments in the two backup
  XML files are replaced with a short statement that a full backup is
  intentional. Backup behavior does not change.
- **In-app Privacy screen.** It is read-only and offline, shows the policy text
  from string resources, and opens from a new `Privacy` button in the vehicle
  list actions row.
- **`play-store/` folder** holding:
  - `privacy-policy.html`, a self-contained page with a contact placeholder
  - `data-safety.md`, the Data safety form answers
  - `listing.md`, the title, short description, full description, and category
  - `release.md`, covering upload key creation, `keystore.properties`, building
    the AAB, and a submission checklist
  - `sample-data.json`, fictional data used for screenshots
  - `graphics/`, holding a 512x512 hi-res icon, a 1024x500 feature graphic, and
    phone screenshots
- **AGENTS.md Commands section**, updated for the new IDs and the release build.

## Out of scope

- Any Play Console action: creating the app, enrolling in Play App Signing,
  uploading the AAB, or filling in forms.
- Hosting the privacy policy or filling in its contact address. The owner does
  both.
- Generating the owner's real upload key or handling its passwords.
- Renaming the Kotlin `namespace` or moving source packages.
- Changing `versionCode` (1) or `versionName` ("1.0").
- Changing what auto-backup includes, adding device-transfer rules, or any other
  backup behavior.
- Tablet, Wear, or TV screenshots, promo video, and localized listings.
- Migrating the user's emulator data from the old `com.example.vehiclemaintenance`
  install. The owner can do this with the app's own Export and Import (see Notes).
- Pushing, tagging, or publishing.

## Build loop

`workflow.stepReview` is `feature`, so build all steps one after another, verify
each step's Done when as you go, then present one review packet for the whole
feature. `workflow.checkpointCommits` is `disabled`, so make no commits. `/complete`
creates the single feature commit after approval.

## Build steps

- [x] **1. App identity.** In `app/build.gradle.kts`:
  - Set `applicationId = "com.stevenmcgrew.vehiclemaintenancetrackerapp"`.
  - Add a `debug` build type with `applicationIdSuffix = ".debug"`.

  Elsewhere:
  - Set `app_name` to `Vehicle Maintenance Tracker App` in
    `app/src/main/res/values/strings.xml`.
  - Add `app/src/debug/res/values/strings.xml`, overriding `app_name` with
    `Vehicle Maintenance (debug)`.
  - Add `keystore.properties` to `.gitignore`.
  - Update the package assertion in `ExampleInstrumentedTest` to
    `com.stevenmcgrew.vehiclemaintenancetrackerapp.debug`.
  - Update the AGENTS.md Commands section: change the `adb shell am start`
    target to
    `com.stevenmcgrew.vehiclemaintenancetrackerapp.debug/com.example.vehiclemaintenance.MainActivity`
    and add `./gradlew bundleRelease` with a pointer to `play-store/release.md`.

  **Done when:**
  - `./gradlew assembleDebug testDebugUnitTest` passes.
  - `installDebug` puts a second app labeled "Vehicle Maintenance (debug)" on
    the emulator.
  - The old `com.example.vehiclemaintenance` install and its data are still
    present and untouched.
  - The new app's FileProvider authority resolves. Check this by sharing a
    history PDF.

- [x] **2. Launcher icon.**
  - Replace `ic_launcher_background.xml` and `ic_launcher_foreground.xml` with a
    new vector design (default below).
  - Add a dedicated `ic_launcher_monochrome.xml` and point the `<monochrome>`
    layer of both `mipmap-anydpi` files at it.
  - Delete the template `mipmap-*dpi/*.webp` files. With minSdk 29, `mipmap-anydpi`
    always wins, so they are dead weight that still shows the robot.
  - Keep the artwork inside the 66dp safe zone of the 108dp canvas.

  Default design: a solid `#007AFF` background (the app's light primary) with a
  white wrench laid diagonally across a simple car front silhouette. The owner
  can change this at review.

  **Done when:**
  - `./gradlew lintDebug` reports no new icon or resource errors.
  - Emulator screenshots of the launcher show the new icon with circle and
    squircle masks in light mode and with themed icons on (monochrome).

- [x] **3. Release build: R8 and signing.** In `app/build.gradle.kts`:
  - Turn on optimization for `release`. Use the AGP 9 `optimization {}` DSL
    already in the file, and confirm against AGP 9.4 whether resource shrinking
    needs its own flag.
  - Add a `release` signing config that loads `keystore.properties` from the
    project root. Its keys are `storeFile`, `storePassword`, `keyAlias`, and
    `keyPassword`. Apply it only when the file exists.
  - Add `app/proguard-rules.pro` only if the smoke test shows a needed keep rule,
    with a comment explaining why for each rule.

  Verify signing:
  - Never read, print, or overwrite an existing `keystore.properties`.
  - To test signing, make a throwaway keystore and properties file in the
    scratchpad and point Gradle at them for that one run (for example a
    temporary root file, removed straight after), but only when no real file
    exists.

  **Done when:**
  - `./gradlew bundleRelease assembleRelease` succeeds without the file, giving
    unsigned output.
  - It also succeeds with the throwaway key. `apksigner verify --print-certs`
    on the APK shows the throwaway cert.
  - The release merged manifest has no `INTERNET` permission.
  - A smoke test of the signed release APK on the emulator passes, using the
    new release ID with fictional data only:
    - add, edit, and delete a vehicle
    - add a maintenance item with last-done seeding
    - log a service and a repair
    - view history and cost totals
    - save and share the PDF
    - export, then import the export
    - kill and relaunch the app, and the data is still there
    - trigger a run of the daily worker with
      `adb shell cmd jobscheduler run` or the WorkManager diagnostics broadcast,
      and no crash appears in `adb logcat` for the app.

- [x] **4. Backup rules cleanup.** In `app/src/main/res/xml/backup_rules.xml` and
  `data_extraction_rules.xml`, replace the template comments and TODOs. Each file
  should say that a full backup of app data is intentional, so the store follows
  the user to a new phone. No `<include>` or `<exclude>` changes.

  **Done when:**
  - `./gradlew lintDebug` passes.
  - A diff shows only comment changes in those two files.

- [x] **5. In-app Privacy screen.**
  - Add `Routes.PRIVACY = "privacy"` and a `composable` in
    `VehicleMaintenanceApp.kt`.
  - Add `privacy/PrivacyScreen.kt`. It is a `Scaffold` with a `TopAppBar` that
    has a back arrow (`brandIconButtonColors()`, like `BackupScreen`), and a
    scrolling column of headed paragraphs.
  - Add an `ExtraSmallButton(stringResource(R.string.privacy_action), onOpenPrivacy)`
    after Backup in `VehicleListActionsRow`. Like Backup, it stays visible when
    the list fails to load.
  - Thread `onOpenPrivacy` through `VehicleListScreen`, its stateless overload,
    and the previews.
  - Put all policy text in `strings.xml` (see Data / contracts). The screen
    loads nothing and cannot fail.
  - Add `app/src/androidTest/.../privacy/PrivacyScreenTest.kt`. It opens the
    screen from the list, asserts the title and one policy heading, and asserts
    that Back returns to the list. Use the temp-file container pattern so the
    real store is never touched.

  **Done when:**
  - `./gradlew assembleDebug testDebugUnitTest lintDebug` passes.
  - On the emulator, tapping Privacy shows the full text in light and dark mode
    and at 200% font scale with no clipping. TalkBack reads the headings as
    headings.
  - `PrivacyScreenTest` passes under `connectedDebugAndroidTest`, run with the
    memory's backup-first and leave-installed rules.

- [x] **6. Play documents.**
  - Write `play-store/privacy-policy.html`: self-contained, inline CSS, no
    external resources, readable in light and dark. Its wording must match the
    in-app text. Include an effective date (the day it is written) and a
    clearly marked `[CONTACT EMAIL]` placeholder.
  - Write `data-safety.md`. Before writing the auto-backup answer, check the
    current Play Console Help guidance on Android backup and Data safety, and
    cite the URL.
  - Write `listing.md` and `release.md`.

  **Done when:**
  - All four files exist.
  - The title is 30 characters or fewer, the short description 80 or fewer, and
    the full description 4000 or fewer. Record the counts in `listing.md`.
  - Every claim in the policy and data safety doc matches the release merged
    manifest and the code. Check these:
    - no network permission
    - auto-backup on
    - notifications local
    - export, PDF, and share go only where the user picks
  - `release.md`'s checklist includes replacing `[CONTACT EMAIL]` and hosting
    the policy before submission.

- [x] **7. Store graphics.**
  - Add `play-store/sample-data.json`: fictional vehicles, items, and logs with
    a valid `schemaVersion` of 1, enough to show OK, Due, and Overdue rows and
    a history with costs across two years.
  - Push it to the emulator's Download folder and import it into the release
    app through Export/Import.
  - Capture 4 to 6 phone screenshots with `adb exec-out screencap`: vehicle
    list, vehicle detail with status, log service form, service history with
    totals, and optionally the PDF or dark mode. Temporarily set
    `adb shell wm size 1080x1920` so each shot is 9:16, then reset it with
    `wm size reset`.
  - Render the 512x512 icon and the 1024x500 feature graphic from SVG sources
    kept in `play-store/graphics/` using `rsvg-convert` or `magick`. The feature
    graphic shows the icon artwork plus the title on the brand blue, and is
    flattened to no alpha.

  **Done when:**
  - `magick identify` shows these exact dimensions:
    - icon: 512x512 PNG
    - feature graphic: 1024x500 with no alpha channel
    - each screenshot: 1080x1920 PNG
  - No screenshot shows the owner's real data.

## Files / areas

- `app/build.gradle.kts`, which holds the application ID, debug suffix, release
  optimization, and signing config
- `.gitignore`
- `app/proguard-rules.pro`, created only if needed
- `app/src/main/res/values/strings.xml` and `app/src/debug/res/values/strings.xml`
  (new)
- `app/src/main/res/drawable/ic_launcher_{background,foreground,monochrome}.xml`,
  `app/src/main/res/mipmap-anydpi/ic_launcher{,_round}.xml`, and the deleted
  `mipmap-*dpi/*.webp` files
- `app/src/main/res/xml/backup_rules.xml` and `data_extraction_rules.xml`, comment
  changes only
- `app/src/main/java/com/example/vehiclemaintenance/VehicleMaintenanceApp.kt`, for
  the route
- `.../vehicles/VehicleListScreen.kt`, for the Privacy button
- `.../privacy/PrivacyScreen.kt` (new)
- `app/src/androidTest/.../ExampleInstrumentedTest.kt` and
  `.../privacy/PrivacyScreenTest.kt` (new)
- `play-store/` (new). Holds `privacy-policy.html`, `data-safety.md`,
  `listing.md`, `release.md`, `sample-data.json`, and `graphics/` with its SVG
  sources and PNG outputs.
- `AGENTS.md`, Commands section only

## Data / contracts

- **IDs.** Release is `com.stevenmcgrew.vehiclemaintenancetrackerapp`. Debug is
  that ID plus `.debug`. `FileProvider` authorities already use `${applicationId}`
  and need no change. The `vehiclemaintenance://vehicles` deep link stays the
  same. The notification `PendingIntent` is explicit (context plus
  `MainActivity::class.java`), so a device with both builds installed shows no
  chooser.
- **`keystore.properties`** is a Java properties file at the project root with
  `storeFile` (absolute or root-relative path), `storePassword`, `keyAlias`, and
  `keyPassword`. It is gitignored and never committed, printed, or logged. The
  keystore file itself lives outside the repo, and `*.jks` and `*.keystore` are
  already ignored.
- **Stored data.** There is no schema change. `CURRENT_SCHEMA_VERSION` stays 1.
  `sample-data.json` must parse with the existing `BackupParser`.
- **Privacy text contract.** The HTML and the in-app strings say the same
  things:
  - The app stores vehicles, maintenance items, service and repair records,
    costs, mileage, and notes only in its private storage on the device.
  - No account. No ads. No analytics. No crash reporting. No network access,
    because the app does not request the internet permission.
  - Nothing is collected by or sent to the developer.
  - Android's own backup may copy app data to the user's Google account when
    the user has device backup turned on. Google controls that, and the user can
    turn it off in system settings.
  - Export files, PDFs, and shares go only to the place or app the user chooses.
  - Reminders are local notifications.
  - Deleting a vehicle or uninstalling the app removes the data from the
    device.
  - No children-specific collection.
  - Policy changes are posted at the same URL with a new effective date.

  The in-app version has no contact line. It says contact details are on the
  app's Google Play listing, so no placeholder ever ships in the APK. Strings use
  a `privacy_` prefix with one string per heading and paragraph, and no HTML
  markup in resources.
- **Listing.** Title `Vehicle Maintenance Tracker`, 27 characters. Category Auto
  & Vehicles. Contains ads: No. In-app purchases: No.

## Testing

- **JVM.** `./gradlew testDebugUnitTest` must stay green after the ID change.
  This feature adds no new pure logic, so it adds no new JVM tests.
- **Instrumented.** Add `PrivacyScreenTest` and update the
  `ExampleInstrumentedTest` package assertion. Run
  `connectedDebugAndroidTest -Pandroid.injected.androidTest.leaveApksInstalledAfterRun=true`
  only after backing up any real store, following the memory rule.
- **Build gates.** These must all pass:
  - `./gradlew lintDebug`
  - `./gradlew assembleRelease bundleRelease`, which also runs `lintVitalRelease`,
    in both the unsigned and the throwaway-signed configuration
- **Manual or emulator evidence.** Launcher icon screenshots, the Privacy
  screen in light and dark, the release smoke test from step 3, and
  `magick identify` output for the graphics. Report only what was actually run.
- Prefix every Gradle command with `JAVA_HOME=/opt/android-studio/jbr`.

## Notes for the AI

- **Emulator data.** The user's real data lives in the old
  `com.example.vehiclemaintenance` install on emulator-5554. Never uninstall it
  and never write into it. After this feature, debug builds install as a new
  app. If the user wants their data there, they Export from the old app and
  Import into the new one. Mention this in the review handoff, and do not do it
  for them. The memory's `run-as` backup commands name the old package, so use
  the new debug ID for anything this feature installs.
- **Release smoke testing** uses only the release ID and fictional
  `sample-data.json`. Installing a throwaway-signed release blocks a later
  install signed with the owner's real key. Tell the user the release app must
  be uninstalled before that, which is safe because it holds only sample data.
- **R8 risks to watch.**
  - kotlinx.serialization store load and save. Bundled rules should cover
    `@Serializable` classes.
  - WorkManager worker instantiation by class name. WorkManager ships its own
    rules.
  - Navigation string routes need no special rules.

  If the smoke test shows a class stripped by R8, add a narrow keep rule with a
  comment rather than turning optimization off.
- **Launcher label.** "Vehicle Maintenance Tracker App" will be cut off on most
  home screens. The owner chose it knowingly, so don't shorten it.
- Keep `Privacy` as a plain `ExtraSmallButton` in the existing actions row. It
  sits just under the top bar, matches the Backup button, and survives load
  failure. That is why it isn't a new top-bar icon.
- Use string resources for every user-visible string, with no concatenation.
  Use the existing `brandIconButtonColors()` for the back arrow.
- If Play Help says auto-backup does or does not count as collection, write what
  it says and cite it. If the guidance is ambiguous, say so in `data-safety.md`
  and flag it in the review handoff. Don't guess.
