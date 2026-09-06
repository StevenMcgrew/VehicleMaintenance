# Current Feature

**Feature 7. Reminders and notifications**

**Branch:** `feature/reminders-and-notifications`

**Status:** verified

## Goal

A daily background check finds maintenance items whose reminder date has arrived
and posts one notification per vehicle, repeating every two weeks until the
service is logged. On Android 13+ the app asks for notification permission at the
moment the reason is obvious. The app stays fully usable when permission is
refused.

## In scope

- A daily `PeriodicWorkRequest` that survives reboots and app updates, enqueued
  as unique work from `VehicleMaintenanceApplication.onCreate`.
- Pure planning logic that turns the current vehicles and items into a set of
  notifications to post, notifications to cancel, and items to stamp.
- One notification per vehicle listing every item past its reminder date, with a
  stable notification id keyed by vehicle.
- The two-week repeat cadence, driven by `MaintenanceItem.lastNotifiedAt`.
- A notification channel for service reminders.
- `POST_NOTIFICATIONS` in the manifest and a runtime request fired right after a
  maintenance item is saved successfully.
- Tapping a notification opens that vehicle's detail screen.
- JVM unit tests for the planner and the new repository write; one instrumented
  test that runs the worker directly.

## Out of scope

- Any notification driven by `recurrence` or by mileage. Recurrence produces the
  on-screen due date only, and the mileage check stays the opportunistic in-app
  callout feature 6 already ships.
- Per-item notifications, an app-wide summary notification, notification actions
  ("Log it", "Snooze"), or a notification settings screen.
- Exact alarms, `BOOT_COMPLETED` receivers, foreground services.
- Cost totals (feature 8), export and import (feature 9), release readiness
  (feature 10).
- Changing the store `schemaVersion`. `lastNotifiedAt` already exists on
  `MaintenanceItem` and already round trips.

## Build loop

`workflow.stepReview` is `feature`, so implement every step below, then present
one review packet covering the whole feature. `workflow.checkpointCommits` is
`disabled`, so do not commit after a step. `/complete` creates the feature commit.

All four regular quality gates are `manual`. Run `/audit`, `/check`, or `/try`
only if asked.

## Build steps

- [x] **1. Stamp notification times through the repository.**
  Add `suspend fun markNotified(itemIds: Set<String>, at: Instant): StoreResult<Unit>`
  to `MaintenanceItemRepository` and implement it in `JsonMaintenanceItemRepository`
  as a single `holder.update` that writes `lastNotifiedAt` on every matching item.
  Ids that no longer exist are skipped, not rejected: the store can change between
  the planner reading it and the worker writing.
  *Done when:* `./gradlew testDebugUnitTest` passes with a new test in
  `JsonMaintenanceItemRepositoryTest` proving one call stamps several items in one
  write, leaves other items untouched, and succeeds when an id is unknown.

- [x] **2. Plan reminders as pure logic.**
  Add `reminders/ReminderPlanner.kt` with a `ReminderPlan` result and a pure
  `planReminders(vehicles, itemsByVehicle, today, now)` function following the
  rules under **Data / contracts**. No Android imports in this file.
  *Done when:* `./gradlew testDebugUnitTest` passes with
  `app/src/test/java/com/example/vehiclemaintenance/reminders/ReminderPlannerTest.kt`
  covering: an item before its reminder date is not notified and its vehicle is
  cancelled; a first-time due item is notified and stamped; a stamp less than 14
  days old suppresses the notification and leaves the vehicle uncancelled; a stamp
  exactly 14 days old notifies again; a newly due item causes every due item on
  that vehicle to be listed and stamped; a null `lastDoneDate` is never notified;
  an unparsable `lastNotifiedAt` is treated as never notified.

- [x] **3. Run the plan daily and post the notifications.**
  Add `androidx.work:work-runtime-ktx` to `gradle/libs.versions.toml` and
  `app/build.gradle.kts`, plus `androidx.work:work-testing` as
  `androidTestImplementation`. Add `reminders/ReminderNotifier.kt` (creates the
  channel, posts and cancels per-vehicle notifications through
  `NotificationManagerCompat`), `reminders/DueReminderWorker.kt` (a
  `CoroutineWorker` that loads the store, builds the planner input from the
  repositories, applies the plan, then calls `markNotified`), and
  `reminders/ReminderScheduler.kt` (enqueues the unique daily periodic work).
  Call the scheduler and create the channel from
  `VehicleMaintenanceApplication.onCreate`.
  *Done when:* `./gradlew assembleDebug` and `./gradlew lintDebug` pass with no
  new warnings, and an instrumented test in
  `app/src/androidTest/java/com/example/vehiclemaintenance/reminders/DueReminderWorkerTest.kt`
  uses `TestListenableWorkerBuilder` to run the worker against a seeded store and
  asserts it returns `Result.success()` and stamped the due item's
  `lastNotifiedAt`.

- [x] **4. Ask for notification permission where the reason is obvious.**
  Add `<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />`
  to the manifest. In `MaintenanceItemFormScreen`, extend the existing
  `LaunchedEffect(uiState.savedSuccessfully, uiState.deletedSuccessfully)` at
  line 72 so that on a successful *save* (not a delete), when
  `Build.VERSION.SDK_INT >= 33` and the permission is not granted, a
  `rememberLauncherForActivityResult(RequestPermission())` is launched first and
  `onDone()` runs from the launcher callback for either answer. Every other path
  calls `onDone()` unchanged.
  *Done when:* on an Android 13+ emulator, saving a maintenance item with
  permission not yet granted shows the system dialog, and both Allow and Don't
  allow return to the vehicle detail screen with the item saved and visible.
  On an emulator below API 33 the item saves with no dialog.

- [x] **5. Open the right vehicle when the notification is tapped.**
  Add a `navDeepLink { uriPattern = "vehiclemaintenance://vehicles/{vehicleId}" }`
  to the `Routes.VEHICLE_DETAIL` composable in `VehicleMaintenanceApp.kt`, and a
  matching `<intent-filter>` on `MainActivity` with `VIEW`, `DEFAULT`,
  `BROWSABLE`, and `<data android:scheme="vehiclemaintenance" android:host="vehicles" />`.
  Set the notification's `contentIntent` to a `PendingIntent.getActivity` wrapping
  `Intent(ACTION_VIEW, thatUri, context, MainActivity::class.java)` with
  `FLAG_IMMUTABLE or FLAG_UPDATE_CURRENT` and a request code derived from the same
  stable per-vehicle id used for the notification.
  *Done when:* on an emulator,
  `adb shell am start -a android.intent.action.VIEW -d "vehiclemaintenance://vehicles/<id>"`
  opens that vehicle's detail screen, and tapping a real posted notification does
  the same and dismisses it (`setAutoCancel(true)`).

## Files / areas

New, under `app/src/main/java/com/example/vehiclemaintenance/reminders/`:

- `ReminderPlanner.kt` - pure planning, no Android imports
- `ReminderNotifier.kt` - channel, post, cancel
- `DueReminderWorker.kt` - `CoroutineWorker`
- `ReminderScheduler.kt` - unique periodic work enqueue

Changed:

- `maintenance/MaintenanceItemRepository.kt` - add `markNotified`
- `maintenance/MaintenanceItemFormScreen.kt` - permission request at line 72
- `VehicleMaintenanceApplication.kt` - create the channel, schedule the work
- `VehicleMaintenanceApp.kt` - deep link on `Routes.VEHICLE_DETAIL`
- `app/src/main/AndroidManifest.xml` - permission, intent filter
- `app/src/main/res/values/strings.xml` - channel and notification strings
- `gradle/libs.versions.toml`, `app/build.gradle.kts` - WorkManager

New tests:

- `app/src/test/java/com/example/vehiclemaintenance/reminders/ReminderPlannerTest.kt`
- `app/src/androidTest/java/com/example/vehiclemaintenance/reminders/DueReminderWorkerTest.kt`
- additions to `app/src/test/java/com/example/vehiclemaintenance/maintenance/JsonMaintenanceItemRepositoryTest.kt`

Read but not changed: `maintenance/MaintenanceStatus.kt` (`plusInterval` is reused
by the planner), `maintenance/MaintenanceItem.kt`, `vehicles/VehicleRepository.kt`.

## Data / contracts

**`lastNotifiedAt` format (locked, because feature 9 exports it).**
An ISO-8601 instant in UTC, exactly `Instant.toString()`, for example
`2026-09-06T14:03:11Z`. Written only by `markNotified`. Read with
`Instant.parse`; a value that fails to parse is treated as `null`, meaning the
item is notified once and the field is rewritten in the correct format. `null`
means "never notified since the last completion", which is what
`JsonServiceLogRepository.add` already writes when a service is logged.

**What makes an item notifiable.** Only the reminder clock:
`nextReminderDate = lastDoneDate + reminder`. An item is *due for notification*
when `lastDoneDate != null` and `!today.isBefore(nextReminderDate)`. `recurrence`
and mileage never trigger a notification.

**Per-vehicle planning**, run for each vehicle independently:

1. `dueItems` = every item on the vehicle that is due for notification.
2. If `dueItems` is empty, cancel that vehicle's notification and stop.
3. `ready` = items in `dueItems` whose `lastNotifiedAt` is null or is at least 14
   days before `today`, measured as whole days between the stamp's date in the
   device's default zone and `today`.
4. If `ready` is empty, post nothing and cancel nothing. An already visible
   notification stays up.
5. Otherwise post one notification for the vehicle listing **all** of `dueItems`,
   and stamp **all** of `dueItems` with `now`.

Step 5 stamps every due item, not just `ready`, on purpose. The notification the
user sees names all of them, so all of their cadences restart together. Stamping
only `ready` would let a vehicle with several items drift into a notification
every few days.

**Notification shape.**

- Channel id `service_reminders`, `IMPORTANCE_DEFAULT`, name and description from
  `strings.xml`, created before the first post.
- Id: a stable integer derived from `vehicleId` (`vehicleId.hashCode()`), so a
  repeat replaces the previous notification for that vehicle rather than stacking.
- Title: `context.getString(R.string.vehicle_summary, year, make, model)`, the
  same format `VehicleListScreen.kt:223` uses.
- Body: a new `notification_due_count` plurals string ("1 service due" /
  "%1$d services due") with the item names joined by ", " as the expanded text.
- `setAutoCancel(true)`, `setSmallIcon` using the existing launcher drawable.
- All user-visible text comes from `strings.xml`. Item names are user-controlled
  text; `NotificationCompat` renders them as plain text, so pass them through
  unchanged with no HTML or `Html.fromHtml`.

**Work scheduling.** `PeriodicWorkRequestBuilder<DueReminderWorker>(1, TimeUnit.DAYS)`,
enqueued with `enqueueUniquePeriodicWork("daily-due-check", ExistingPeriodicWorkPolicy.KEEP, request)`.
`KEEP` so an app restart never resets the period. WorkManager persists its own
queue across reboots and updates, so no `BOOT_COMPLETED` receiver is needed. No
constraints: the check is local and needs neither network nor charging.

**Worker inputs and failure.** The worker calls
`vehicleRepository.load()` first, because it may run in a process where nothing
has read the store yet. On `StoreResult.Failure` it returns `Result.retry()` and
posts nothing, so a store it could not parse is never acted on. It builds
`itemsByVehicle` from `maintenanceItemRepository.itemsFor(vehicle.id).value` for
each vehicle. A `markNotified` failure returns `Result.retry()`. Notifications are
posted before the stamp is written; a crash between the two costs at most one
duplicate notification, which is better than a silent missed reminder.

**Permission.** `POST_NOTIFICATIONS` is requested at runtime only on API 33+.
No new persisted "already asked" flag: Android itself stops showing the dialog
after the user has denied twice, and the request then returns denied with no UI.
When permission is absent, `NotificationManagerCompat.notify` is a no-op; the
worker still runs and still stamps `lastNotifiedAt`, and the on-screen due and
overdue status from feature 6 remains the user's full picture, as the overview
requires.

## Testing

`./gradlew testDebugUnitTest` for the planner and the repository write.
`./gradlew connectedDebugAndroidTest` for the worker test, which needs a device or
emulator. `./gradlew lintDebug` must gain no new warnings.

The planner takes `today: LocalDate` and `now: Instant` as parameters rather than
calling `LocalDate.now()`, matching the `today: () -> LocalDate` seam
`JsonMaintenanceItemRepository` and `VehicleDetailViewModel` already use. That is
what makes every cadence case testable without waiting.

Emulator screenshots are the evidence for the notification's appearance, the
permission dialog, and the tap-through, as `AGENTS.md` describes. Do not claim
that evidence unless it was actually captured.

## Notes for the AI

- Read `blueprint/context/coding-standards.md` before writing code. No em dashes
  anywhere. Comment the why, not the what.
- Declare WorkManager in `gradle/libs.versions.toml` first, then reference it as
  `libs.androidx.work.runtime.ktx`. Never hardcode a version in a build file. Pick
  the current stable `androidx.work` version and confirm it resolves with
  `./gradlew assembleDebug`; the catalog has no `work` entry today.
- Gradle configuration cache is on. Keep build logic configuration-cache safe.
- Do not touch `CURRENT_SCHEMA_VERSION`. Nothing about this feature changes the
  store's shape.
- `AppContainer` keeps `storeHolder` private. Keep it that way: the worker goes
  through the repository interfaces, which is the seam the standards ask for.
- WorkManager's default `androidx.startup` initializer is fine. Do not add a
  custom `Configuration.Provider` or disable the initializer.
- Verify the notification and the permission dialog in both light and dark mode.

## Open questions

None. The overview's open question about batching was resolved during this spec:
one notification per vehicle, listing every item that is due. The permission
prompt fires after a successful maintenance item save. Both are recorded above
and should be folded into `blueprint/context/project-overview.md` when it is next
regenerated.
