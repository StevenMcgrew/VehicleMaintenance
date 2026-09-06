# Current Feature

**Feature 6 - Due and overdue status**

**Branch:** `feature/due-and-overdue-status`

**Status:** verified

Feature 6 from `blueprint/build-plan.md`: due and overdue status.

## Goal

Turn the vehicle detail table into the watchdog screen the app exists for. Every
maintenance item gets a computed next reminder date, due date, mileage due, and a
plain OK / Due / Overdue state, all derived at read time and never stored. When a
new odometer reading arrives with a logged service or repair, the items that
reading just pushed overdue are named on screen instead of being left for the user
to spot.

The overview locks this as a standalone view: "The overdue view must stand alone
without notifications. On Android 13+ the user can refuse notification permission,
and the app still has to be useful to them when opened." Nothing in this feature
may depend on feature 7.

## In scope

- A pure status calculator over `MaintenanceItem`, the vehicle's current odometer,
  and today's date.
- The vehicle's current odometer derived from its service log entries.
- The vehicle detail table showing next due date, miles left, and status per item,
  with the mileage and recurrence intervals as a subheading under each name.
- The existing item actions sheet showing the full derived detail for one item:
  next reminder date, due date, mileage due, miles left, and status.
- A dialog on vehicle detail naming the items a newly captured odometer reading
  just pushed overdue by mileage.
- Unit tests for the calculator and the formatting, and Compose tests for the
  table and the dialog.

## Out of scope

- Notifications, the daily background check, and `lastNotifiedAt`. Feature 7 owns
  all of it. This feature reads `lastNotifiedAt` never and writes it never.
- Any due or overdue rollup on the vehicle list screen. The overview describes the
  status table on vehicle detail only, so a list badge would be invented scope.
- Mileage projection, estimated miles per day, or any "due in about N weeks by
  mileage" estimate. Explicit non-goal in the overview.
- A "due soon" warning band ahead of the due point. The feature is named for two
  states and the overview defines no third threshold. The reminder date already
  serves as the early signal.
- Reordering the table so overdue items float to the top. The current order is
  preserved; no ordering rule exists in the plans to implement.
- Changing how `ServiceHistoryScreen` renders dates. See Notes.
- Cost totals on vehicle detail. That is feature 8.

## Build loop

`workflow.stepReview` is `feature` and `workflow.checkpointCommits` is `disabled`.
Work through all build steps, then present one review packet covering the whole
feature. Do not stop for approval after each step and do not create checkpoint
commits. `/complete` creates the single feature commit.

## Build steps

- [x] **1. Status calculation, pure and tested.**
  Add `maintenance/MaintenanceStatus.kt` with:
  - `enum class MaintenanceStatus { OK, DUE, OVERDUE, NONE }`
  - `data class MaintenanceItemStatus(status, nextReminderDate: LocalDate?, dueDate: LocalDate?, mileageDue: Int?, milesLeft: Int?)`
  - `fun LocalDate.plus(interval: Interval): LocalDate` mapping DAYS/WEEKS/MONTHS/YEARS to `plusDays`/`plusWeeks`/`plusMonths`/`plusYears`
  - `fun currentOdometer(entries: List<ServiceLogEntry>): Int?` returning the
    highest odometer among the entries, or null when the list is empty
  - `fun statusOf(item: MaintenanceItem, currentOdometer: Int?, today: LocalDate): MaintenanceItemStatus`

  Rules, exactly as written under Data / contracts below. Add
  `app/src/test/java/com/example/vehiclemaintenance/maintenance/MaintenanceStatusTest.kt`
  covering every branch listed there, including both boundary cases and the
  end-of-month clamp.
  **Done when:** `./gradlew testDebugUnitTest` passes and the new test class covers
  OK, DUE, OVERDUE, and NONE, both boundaries, a null `lastDoneDate`, a missing
  mileage baseline, an empty log, and a back-dated entry that must not lower the
  current odometer.

- [x] **2. Vehicle detail view model exposes the status.**
  `VehicleDetailViewModel` gains a `ServiceLogRepository` (already on
  `AppContainer` as `serviceLogRepository`) and a `today: () -> LocalDate = { LocalDate.now() }`
  seam. Collect `serviceLog.entriesFor(vehicleId)` alongside the existing items
  flow. Replace `items: List<MaintenanceItem>` in `VehicleDetailUiState` with
  `rows: List<MaintenanceItemRow>`, where `MaintenanceItemRow(item, status)`, and
  recompute rows whenever either flow emits. Keep the existing list order.
  Update the actions sheet and delete dialog lookups, which currently read
  `uiState.items`, and the existing previews.
  **Done when:** `./gradlew assembleDebug` succeeds, `./gradlew testDebugUnitTest`
  still passes, and the existing `VehicleDetailScreenTest` still passes.

- [x] **3. Table shows the derived values.**
  Add `maintenance/DateFormat.kt` with `fun formatShortDate(date: LocalDate, locale: Locale = Locale.getDefault()): String`
  using `DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)`, and
  `formatMediumDate` using `FormatStyle.MEDIUM`, with unit tests under a fixed
  locale. Replace the three interval columns in `MaintenanceItemHeader` and the
  table row with **Next due**, **Miles left**, and **Status**, and move the
  mileage interval and the recurrence interval to a subheading line under the
  service name, as described under Data / contracts. Add the new strings to
  `res/values/strings.xml`. Add `@Preview` cases covering an OK item, a Due item,
  an Overdue item, and an item with no computable status.
  **Done when:** `./gradlew assembleDebug` and `./gradlew lintDebug` pass with no
  new warnings, the previews render all four states, and an emulator screenshot in
  light and dark shows the status column reading OK, Due, and Overdue with no
  clipped or ellipsized column at the default font scale.

- [x] **4. Actions sheet shows the full detail.**
  Above the three existing actions in `MaintenanceItemActionsSheet`, add a
  read-only block for the tapped item: status, next reminder date, due date, and
  mileage due with miles left. Every value uses `formatMediumDate` or
  `formatMileage`, and an absent value shows the existing `value_not_set` dash
  with its label still present. The sheet keeps reading the item out of the
  current row list each recomposition, as it does today.
  **Done when:** opening the sheet on an item with a recurrence, a mileage
  interval, and a last-done baseline shows all four values, and opening it on an
  item with a cleared last-done date shows the dash for the reminder and due date
  while still showing the labels.

- [x] **5. New reading callout.**
  In `VehicleDetailViewModel`, track the previous current odometer and the
  previous set of item ids whose status is OVERDUE by mileage. When a recomputation
  shows the current odometer has increased **and** ids have entered that set,
  publish `newlyOverdueByMileage: List<String>` (item names, in table order) on the
  ui state. Do not publish on the first computation after the screen opens, and do
  not publish when the odometer did not increase, so editing an item's mileage
  interval never triggers the dialog. Add `fun dismissNewlyOverdue()` following the
  existing `dismissDeleteError` shape. Render an `AlertDialog` on vehicle detail
  when the list is non-empty, naming the items, with a single confirm action that
  calls the dismiss.
  **Done when:** logging a service or repair with an odometer that crosses an
  item's mileage due returns to vehicle detail showing the dialog naming that item,
  the table underneath already reads Overdue for it, dismissing does not bring the
  dialog back, and logging a reading that crosses nothing shows no dialog.

- [x] **6. Compose coverage.**
  Extend `app/src/androidTest/java/com/example/vehiclemaintenance/maintenance/VehicleDetailScreenTest.kt`
  with cases driving `VehicleDetailContent` directly: an Overdue row renders the
  overdue label, a row with nothing computable renders the dash, and a non-empty
  `newlyOverdueByMileage` renders the dialog with the item name and the confirm
  action invokes the dismiss callback.
  **Done when:** `./gradlew connectedDebugAndroidTest` passes on a running
  emulator.

## Files / areas

New:
- `app/src/main/java/com/example/vehiclemaintenance/maintenance/MaintenanceStatus.kt`
- `app/src/main/java/com/example/vehiclemaintenance/maintenance/DateFormat.kt`
- `app/src/test/java/com/example/vehiclemaintenance/maintenance/MaintenanceStatusTest.kt`
- `app/src/test/java/com/example/vehiclemaintenance/maintenance/DateFormatTest.kt`

Edited:
- `app/src/main/java/com/example/vehiclemaintenance/maintenance/VehicleDetailViewModel.kt`
- `app/src/main/java/com/example/vehiclemaintenance/maintenance/VehicleDetailScreen.kt`
- `app/src/main/res/values/strings.xml`
- `app/src/androidTest/java/com/example/vehiclemaintenance/maintenance/VehicleDetailScreenTest.kt`

Read only, for reference:
- `maintenance/MaintenanceItem.kt`, `maintenance/Interval.kt`, `maintenance/MileageFormat.kt`
- `servicelog/ServiceLogRepository.kt`, `servicelog/ServiceLogEntry.kt`
- `VehicleMaintenanceApplication.kt` for `container.serviceLogRepository`

No data model change. `MaintenanceStore` and its schema version are untouched,
because every value in this feature is derived. No new dependency, so
`gradle/libs.versions.toml` and `app/build.gradle.kts` are untouched.

## Data / contracts

### Current odometer

`currentOdometer(entries)` is the **highest** `odometer` among the vehicle's
service log entries, not the most recent by date. A user can log a repair they
forgot from last year; a back-dated entry with a lower reading must never lower
the vehicle's known mileage. Returns null when the vehicle has no entries, which
matches the overview's rule that "the mileage check stays inactive until the first
logged completion." An item's own `lastDoneMileage` is a per-item baseline and
never contributes to the vehicle's current reading.

### Derived values

Given `item`, `currentOdometer: Int?`, and `today`:

- `nextReminderDate` = `lastDoneDate + reminder`, or null when `lastDoneDate` is
  null. `reminder` is required on the model, so it is never the missing half.
- `dueDate` = `lastDoneDate + recurrence`, or null when either is null.
- `mileageDue` = `lastDoneMileage + mileageInterval`, or null when either is null.
- `milesLeft` = `mileageDue - currentOdometer`, or null when either is null. It
  goes negative once the reading is past the due mileage.

Date arithmetic uses `java.time`, so `Jan 31 + 1 month` clamps to `Feb 28` or
`Feb 29`. That is the intended behavior; assert it in a test so a later refactor
cannot silently change it.

`lastDoneDate` is nullable on the model and reachable in practice: the repository
defaults it to today at creation, but the edit form offers "Clear the last done
date." Every branch must handle null.

### Status

Evaluated in this order, first match wins:

1. **OVERDUE** when `dueDate != null && today >= dueDate`, or when
   `mileageDue != null && currentOdometer != null && currentOdometer >= mileageDue`.
2. **DUE** when `nextReminderDate != null && today >= nextReminderDate`.
3. **NONE** when `nextReminderDate == null && dueDate == null && mileageDue == null`,
   or when the only mileage signal exists but the vehicle has no reading yet and
   there is no reminder date. In short, NONE means no signal at all was computable.
4. **OK** otherwise.

Both thresholds are inclusive (`>=`). An item is treated as reaching its due point
on the exact day or the exact mile, not the day after. Fixing this boundary here
keeps feature 7 from reinterpreting it.

DUE and OVERDUE are distinct because the overview makes `reminder` independent of
`recurrence` and deliberately earlier ("remind me 5 months from last done" for a
six month oil change). DUE means the reminder has arrived. OVERDUE means the
service itself is late.

**Decided this session:** the reminder date drives on-screen status, not only
notifications. A mileage-only item with no recurrence and no logged reading yet
turns DUE when its reminder date arrives. Without this, such an item would show no
status at all until its first logged completion, and the overview's requirement
that the overdue view stand alone without notifications would fail exactly for the
users who refused notification permission. Recurrence still never notifies; that
locked rule is about notifications, not about which signals may reach the screen.

### Table columns and the interval subheading

The three interval columns (Miles, Time, Remind) are settings, and four derived
columns plus a name will not fit a phone. The mileage interval and the recurrence
interval move to a subheading line under the service name, so the row still
carries them without spending a column on each.

Row line 1, the columns:

| Column | Content | Absent value |
| --- | --- | --- |
| Service | `item.name` | n/a |
| Next due | `formatShortDate(dueDate)`, or `formatShortDate(nextReminderDate)` when `dueDate` is null | `value_not_set` dash |
| Miles left | `formatMileage(milesLeft)`, negative when past due | `value_not_set` dash |
| Status | `OK` / `Due` / `Overdue` | `value_not_set` dash |

Row line 2, directly under the name, in `labelSmall` on `onSurfaceVariant`:

- Mileage interval as `"5,000 mi"`, reusing `formatMileage` with a new `mi` short
  label to match the existing `d` / `wk` / `mo` / `yr` labels.
- Recurrence as its existing short label, for example `"6 mo"`.
- Both present joins them: `"5,000 mi · 6 mo"`. One present shows that one
  alone. Neither present omits the line entirely rather than showing a dash, so a
  mileage-only or time-only item does not gain an empty second line.

The reminder interval keeps no place of its own on the row. Its derived next
reminder date is what the user acts on, and that appears in the Next due column
for an item with no recurrence and in the actions sheet for every item. The
setting itself stays editable in the item form.

Column weights become Service `3.5f`, Next due `2f`, Miles left `2f`, Status
`2.5f`. Header cells allow two lines so "Next due" and "Miles left" wrap instead
of ellipsizing in a narrow column.

Status color comes from `MaterialTheme.colorScheme`: `error` for Overdue,
`tertiary` for Due, `onSurfaceVariant` for OK and for the dash. Color is never the
only carrier of meaning; the label text always states the status. Verify both light
and dark, as the standards require.

### Callout

`newlyOverdueByMileage: List<String>` on `VehicleDetailUiState`, empty when there
is nothing to show. It is derived state on a retained view model, not persisted and
not passed through navigation. The vehicle detail entry stays on the back stack
while the log form is open, so its view model is alive and sees the store write the
moment it lands; no navigation result plumbing is needed.

Guarded by a rising current odometer so only a genuinely new reading can raise it,
matching "Mileage is opportunistic. The app cannot read an odometer. A reading is
captured only when work is logged." It is lost on process death, which is
acceptable for a transient notice; the table itself still shows the overdue state.

### Strings

All new user-visible text goes in `res/values/strings.xml`. Needed at minimum:
column headers for Next due, Miles left, and Status; the three status labels; the
`mi` short label and the subheading join format; the sheet detail labels; and the
dialog title, body with the item names, and confirm action. Reuse `value_not_set`, `formatMileage`, `back`, and `cancel` where
they already fit. No hardcoded strings in composables.

## Testing

Unit tests, `./gradlew testDebugUnitTest`:
- `MaintenanceStatusTest` over every branch in the Status rules, both inclusive
  boundaries, null `lastDoneDate`, missing mileage baseline, empty log, back-dated
  lower reading, and the end-of-month clamp.
- `DateFormatTest` under a fixed locale so the assertions do not depend on the
  machine.

Instrumented tests, `./gradlew connectedDebugAndroidTest`: the step 6 cases against
`VehicleDetailContent`, which already takes a `VehicleDetailUiState` directly and
needs no repository.

UI evidence: `verification.uiEvidence` is `when-available`. Capture emulator
screenshots of the vehicle detail table in light and dark showing an OK, a Due, and
an Overdue row, plus the callout dialog. Do not claim any of this evidence until it
has actually been run.

## Notes for the AI

- Nothing here is stored. If a step tempts you to add a field to `MaintenanceItem`
  or `MaintenanceStore`, stop; the value is derived and the schema stays at 1.
- `lastNotifiedAt` belongs to feature 7. Do not read it, write it, or reason about
  it.
- Keep `MaintenanceStatus.kt` free of Android and Compose imports so it stays a
  plain JVM unit test target.
- Inject `today` rather than calling `LocalDate.now()` inside the calculator, so
  every test is deterministic. The view model reads the real clock once per
  `refresh()`; a screen left open past midnight shows a stale day until the next
  refresh, which is acceptable and not worth a ticker in this feature.
- Follow the existing comment discipline. Comment the non-obvious decisions only:
  why the current odometer is the maximum rather than the newest, why the callout
  is guarded by a rising reading, and why the boundary is inclusive.
- No em dashes anywhere, per the writing standard.
- `ServiceHistoryScreen` renders dates as `entry.date.toString()`, so ISO. This
  feature introduces a localized short and medium format on vehicle detail, which
  leaves two date presentations in the app. That is deliberate: an ISO date does
  not fit the narrow table column. Unifying history onto the shared helper is a
  one-line follow-up and belongs in a `/fix`, not in this feature.
