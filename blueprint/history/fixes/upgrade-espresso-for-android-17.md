# Current Feature

**Upgrade Espresso for Android 17**

**Type:** Fix

**Status:** verified

**Branch:** `fix/upgrade-espresso-for-android-17`

## The problem

`./gradlew connectedDebugAndroidTest` cannot run on the Android 17 (API 37)
emulator. 30 of 36 instrumented tests fail before any assertion runs, all with
the same error:

    NoSuchMethodException: android.hardware.input.InputManager.getInstance []

The project pins `espressoCore = "3.5.1"` in `gradle/libs.versions.toml`.
Espresso 3.5.1's `InputManagerEventInjectionStrategy.initialize()` calls
`InputManager.getInstance()` through reflection on every API level, and API 37
no longer has that hidden method. Every Compose UI test goes through Espresso's
idle and input machinery, so every UI test fails. This also happens on `master`
(confirmed while verifying `add-material-icons`). The 6 passing tests are the
ones that inject no input.

## The fix

Bump the AndroidX Test versions in `gradle/libs.versions.toml` to the current
stable releases:

| Catalog key | From | To | Library |
|---|---|---|---|
| `espressoCore` | `3.5.1` | `3.7.0` | `androidx.test.espresso:espresso-core` |
| `junitVersion` | `1.1.5` | `1.3.0` | `androidx.test.ext:junit` |

Why this works: Espresso 3.7.0's `getInputManager()` calls
`InputManager.getInstance()` only below API 23. On API 23 and up it uses
`Context.getSystemService(InputManager::class)`, so the missing method is never
touched. (Checked by disassembling both AARs.)

Upgrade `androidx.test.ext:junit` alongside it. 3.7.0 pulls in
`androidx.test:core 1.7.0`, `runner 1.7.0`, and `monitor 1.8.0`, and ext-junit
1.3.0 targets that same set. Leaving ext-junit at 1.1.5 would mix old and new
test artifacts. `runner` and `core` stay transitive, so no new catalog entries
are needed.

**Must not break.**

- **Minimal test code changes.** The only test edit is step 2's permission
  grant, which the user approved after step 1 exposed the permission dialog.
  Assertions and flows stay as they are.
- **Compose UI test.** `ui-test-junit4` from the Compose BOM pulls in its own
  Espresso. Gradle resolves to the higher 3.7.0, which it supports.
- **Other tests unaffected.** `work-testing` and the JUnit 4 unit tests in
  `app/src/test` are unchanged.
- **Scope.** Leave the other outdated-version lint warnings
  (`GradleDependency`, `NewerVersionAvailable`) for later. This fix only
  unblocks instrumented tests.

**Risk.** Espresso still calls the hidden `InputManager.injectInputEvent`
through reflection. Instrumentation normally has access to it, but if API 37
blocks it, the error will change rather than disappear. In that case, stop and
revise instead of working around it in test code.

## Build steps

- [x] **1. Bump the AndroidX Test versions.** Change the two catalog versions above.
   *Done when:* `./gradlew connectedDebugAndroidTest` runs on the API 37 emulator
   with no `InputManager.getInstance` failure.
- [x] **2. Grant the notification permission in `VehicleDetailScreenTest`.** In
   `setUp()`, on API 33 and up (`Build.VERSION_CODES.TIRAMISU`, the same check
   the app uses), call `uiAutomation.grantRuntimePermission(context.packageName,
   Manifest.permission.POST_NOTIFICATIONS)`. The item form's permission gate
   then continues straight to `onDone`. No new dependency.
   *Done when:* all 36 instrumented tests pass on the API 37 emulator.

### Step 1 result (led to step 2)

The version bump is on the branch, and the Espresso failure is gone. The API 37
run now reports 36 tests, 6 failures, and no `InputManager.getInstance`.

The 6 remaining failures are all in `VehicleDetailScreenTest`, with
`No compose hierarchies found in the app`:

- `anAddedItemAppearsOnTheDetailScreen`
- `anAddedItemIsPersistedToTheStoreFile`
- `theTableLabelsEveryColumnAndShowsTheStatus`
- `deletingFromTheItemFormRemovesTheRowAndTheStoredItem`
- `tappingARowOffersLogEditAndDelete`
- `deletingFromTheActionsSheetRemovesTheRowAndTheStoredItem`

Each one fails in `waitForText("Oil change")`, right after Save on the item
form. Saving calls `rememberNotificationPermissionGate`
(`MaintenanceItemFormScreen.kt:117`), which on API 33+ opens the system
`POST_NOTIFICATIONS` dialog over the test activity. These tests were already
broken this way, but the Espresso crash hid it. Making them pass requires a
test change, which the original spec ruled out. The user chose the step 2
grant.

## Verify

- `./gradlew assembleDebug lintDebug testDebugUnitTest` passes.
- `./gradlew connectedDebugAndroidTest` passes on the Android 17 emulator.
- `./gradlew :app:dependencies --configuration debugAndroidTestRuntimeClasspath`
  resolves `espresso-core` to `3.7.0`, with no leftover `3.5.x`.
- Note: the connected run uninstalls the app afterwards, which clears its data
  on that emulator.

## Verification results

- `./gradlew connectedDebugAndroidTest` on the Pixel 10a AVD (API 37): 36 tests,
  0 failures.
- `./gradlew assembleDebug lintDebug testDebugUnitTest --rerun-tasks` passed.
  An earlier cached run printed a stray Gradle stack trace but still exited 0;
  the forced rerun of all 51 tasks was clean. Lint shows the same warnings as
  before (8 `GradleDependency`, 7 `UnusedResources`, 2 `AndroidGradlePluginVersion`,
  2 `NewerVersionAvailable`, 1 `RedundantLabel`), and none of them are new.
- `debugAndroidTestRuntimeClasspath` resolves to `espresso-core` 3.7.0 (the
  3.5.0 that Compose UI test asks for is raised to 3.7.0), `ext:junit` 1.3.0,
  `core` 1.7.0, `runner` 1.7.0, and `monitor` 1.8.0. No older AndroidX Test
  version remains.
