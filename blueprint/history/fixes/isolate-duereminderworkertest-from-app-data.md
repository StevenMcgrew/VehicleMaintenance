# Current Feature

## Isolate DueReminderWorkerTest from app data

**Type:** Fix
**Status:** verified
**Branch:** fix/isolate-duereminderworkertest-from-app-data

## The problem

`DueReminderWorkerTest` writes its fixture straight into the app's real store,
`File(application.filesDir, STORE_FILE_NAME)`, then runs the worker against it.
On a device or emulator that also holds real app data (the Android Studio
emulator does), every `connectedDebugAndroidTest` run replaces the user's
vehicles, items, and service history with one test Tacoma. The run also posts a
real "Oil change" reminder notification that is never cleared.

The cause is in `DueReminderWorker.doWork()`: it always takes its repositories
from `(applicationContext as VehicleMaintenanceApplication).container`, which is
bound to the real store file. A test has no way to hand it anything else.

It is the only instrumented test that touches the application container or
`filesDir`; every other test already uses a temp file in `cacheDir`.

## The fix

**Worker.** Let `DueReminderWorker` take its `AppContainer` as a constructor
parameter that defaults to the application's container, with `@JvmOverloads`
so the `(Context, WorkerParameters)` constructor WorkManager instantiates by
reflection still exists. Production scheduling and behavior are unchanged.

**Test.**

- Seed the fixture into a unique temp file under `cacheDir`, as the other
  instrumented tests do, and build an `AppContainer` over it.
- Build the worker through `TestListenableWorkerBuilder.setWorkerFactory(...)`
  with a small `WorkerFactory` that returns `DueReminderWorker(context,
  parameters, testContainer)`.
- Assert against the temp file, as the test does today.
- `@After`: delete the temp file and its `.tmp` sibling, and cancel the test
  vehicle's reminder notification with `ReminderNotifier.cancel("v-1")`.
- Add an assertion that the real store file is byte-for-byte unchanged (or still
  absent) after the run, so a regression is caught by the test itself.

**Must not break:**

- The daily due check scheduled by `scheduleDailyDueCheck` still runs:
  WorkManager must still find the two-argument constructor.
- Worker behavior (load, plan, notify, stamp, retry on store failure) is
  unchanged.
- No new dependencies: `androidx.work.testing` is already on the androidTest
  classpath.

## Build steps

- [x] 1. **Injectable container and isolated test** - add the defaulted
   `AppContainer` constructor parameter to `DueReminderWorker`, rewrite
   `DueReminderWorkerTest` to use a temp-file container through a worker
   factory, clean up the file and notification, and assert the real store is
   untouched.
   Done when `./gradlew connectedDebugAndroidTest` passes and the emulator's
   real `files/vehicle-maintenance.json` is byte-for-byte identical before and
   after the run.

## Verify

- `./gradlew testDebugUnitTest`, `./gradlew lintDebug`, and
  `./gradlew assembleDebug` pass with no new warnings.
- Before the instrumented run, back up the emulator's store:
  `adb exec-out run-as com.example.vehiclemaintenance cat files/vehicle-maintenance.json`.
- Run `./gradlew connectedDebugAndroidTest
  -Pandroid.injected.androidTest.leaveApksInstalledAfterRun=true`; all tests
  pass, including `dueItemIsStampedAfterTheWorkerRuns`.
- Compare the store after the run with the backup: identical.
- No test "Oil change" reminder notification is left in the shade.
- Launch the app: your own vehicles are still there.
