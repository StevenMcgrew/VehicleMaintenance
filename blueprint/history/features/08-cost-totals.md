# Current Feature

**Feature 8 - Cost totals**

**Branch:** `feature/cost-totals`

**Status:** verified

## Goal

Show what each vehicle has cost: an all-time total and a per calendar year
breakdown, summed from that vehicle's service log entries. Totals are derived on
read, never stored.

## In scope

- A pure totals calculation over `ServiceLogEntry`, summing `cost` (integer
  minor units) for one vehicle, producing an all-time total plus one total per
  calendar year taken from `entry.date.year`.
- Per-year rows ordered newest year first, matching the newest-first order the
  history already uses.
- Entries with `cost == null` contribute nothing and do not create a year row.
  A year appears only when at least one of its entries has a cost.
- Cost totals surfaced on the **Vehicle detail** screen, which the overview
  already names as their home ("maintenance items as a table with due status,
  plus cost totals").
- Empty state: when the vehicle has no entry with a cost, the totals area says
  so rather than showing `$0.00`.
- Money is formatted through `formatCost`, never through a floating point type.

## Out of scope

- Editing or deleting service log entries (not a planned feature).
- Cost totals across all vehicles, averages, cost per mile, per-item cost
  rollups, charts, or date-range filters. None are in the plans.
- Export/import of totals (feature 9).
- Any change to how `cost` is captured, validated, or stored.
- Marking or counting entries that were logged without a cost.

## Build loop

`workflow.stepReview` is `feature` and `workflow.checkpointCommits` is
`disabled`. Implement all build steps in one pass without stopping for approval
between them and without checkpoint commits, then present one review packet
covering the whole feature. `/complete` creates the feature commit and merges.

## Build steps

- [x] **1. Pure cost totals calculation, unit tested**
  Add `app/src/main/java/com/example/vehiclemaintenance/servicelog/CostTotals.kt`
  with a `VehicleCostTotals` result type (`allTime: Long`, `byYear: List<YearCost>`
  where `YearCost(year: Int, total: Long)`) and a pure
  `fun costTotalsOf(entries: List<ServiceLogEntry>): VehicleCostTotals`. It sums
  into `Long` so a long history cannot overflow the `Int` a single cost uses,
  skips null costs, groups by `entry.date.year`, and returns `byYear` sorted by
  year descending. It does not filter by vehicle; the caller passes one
  vehicle's entries, which is what `ServiceLogRepository.entriesFor` already
  returns.
  Add a `formatCost(minorUnits: Long, locale: Locale = Locale.getDefault())`
  overload in `CostFormat.kt` alongside the existing `Int` one, keeping the
  `BigDecimal.valueOf(value, 2)` path so no float is involved. Have the `Int`
  overload delegate to the `Long` one rather than duplicating the body.
  Add `app/src/test/java/com/example/vehiclemaintenance/servicelog/CostTotalsTest.kt`
  covering: no entries; all entries with null cost; a mix of costed and
  null-cost entries; entries spanning three calendar years including one year
  whose only entry has no cost; year ordering newest first; and a sum large
  enough to exceed `Int.MAX_VALUE` minor units. Extend `CostFormatTest` with one
  case for the `Long` overload.
  **Done when** `./gradlew testDebugUnitTest` passes with the new tests present.

- [x] **2. Expose totals on the vehicle detail state**
  Add `costTotals: VehicleCostTotals = VehicleCostTotals(0L, emptyList())` to
  `VehicleDetailUiState`. In `VehicleDetailViewModel`, the existing
  `serviceLog.entriesFor(vehicleId)` collector already receives the vehicle's
  entries for the odometer logic; compute the totals from that same emission and
  publish them in the same `_uiState.update`. Do not add a second repository
  collector and do not recompute totals from `recompute(...)`, which also runs
  on maintenance item changes that cannot alter cost.
  **Done when** the project compiles (`./gradlew assembleDebug`) and logging a
  service with a cost updates the state's totals through the existing flow, with
  no new subscription to the store.

- [x] **3. Cost totals UI on the vehicle detail screen**
  In `VehicleDetailScreen.kt`, add a totals summary row to the populated branch,
  placed between `VehicleActionsRow` and the maintenance item table so it does
  not compete with the `LazyColumn` for vertical space. The row shows a "Total
  spent" label and the all-time amount, and is clickable to open a
  `ModalBottomSheet` listing one line per year (year label, amount) plus the
  all-time total, reusing the existing sheet and `DetailLine` idioms in this
  file.
  The row renders in every populated case, including a vehicle with no
  maintenance items, so it must sit outside the `rows.isEmpty()` branch. When
  `byYear` is empty, the row shows the "no costs recorded yet" message instead of
  an amount and is not clickable.
  Give the clickable row `heightIn(min = 48.dp)` like `SheetAction`, and never
  let color alone carry meaning. All new user-visible text goes in
  `app/src/main/res/values/strings.xml`; year numbers are formatted through a
  string resource, not string concatenation.
  Add `@Preview` coverage: a vehicle with multi-year totals and a vehicle with no
  recorded costs.
  **Done when** `./gradlew assembleDebug lintDebug` passes with no new lint
  warnings, the two new previews render, and the totals row appears on the
  detail screen in both the has-costs and no-costs cases.

- [x] **4. Instrumented coverage on the detail screen**
  Extend
  `app/src/androidTest/java/com/example/vehiclemaintenance/maintenance/VehicleDetailScreenTest.kt`
  following its existing temp-file, real-repository setup: seed a vehicle with
  service log entries in two calendar years (one of them without a cost), assert
  the all-time total is displayed, open the totals sheet, and assert both year
  rows are shown with their amounts. Add one case asserting the no-costs message
  for a vehicle whose entries all have a null cost.
  **Done when** `./gradlew connectedDebugAndroidTest` passes on a running
  emulator or device. If no device is available, say so in the review packet and
  do not claim the run.

## Files / areas

- `app/src/main/java/com/example/vehiclemaintenance/servicelog/CostTotals.kt` (new)
- `app/src/main/java/com/example/vehiclemaintenance/servicelog/CostFormat.kt` (Long overload)
- `app/src/main/java/com/example/vehiclemaintenance/maintenance/VehicleDetailViewModel.kt`
- `app/src/main/java/com/example/vehiclemaintenance/maintenance/VehicleDetailScreen.kt`
- `app/src/main/res/values/strings.xml`
- `app/src/test/java/com/example/vehiclemaintenance/servicelog/CostTotalsTest.kt` (new)
- `app/src/test/java/com/example/vehiclemaintenance/servicelog/CostFormatTest.kt`
- `app/src/androidTest/java/com/example/vehiclemaintenance/maintenance/VehicleDetailScreenTest.kt`

## Data / contracts

- **No stored data changes.** No new fields, no schema version bump, no
  migration. `MaintenanceStore` and `ServiceLogEntry` are untouched, so
  feature 9's export format is unaffected.
- `ServiceLogEntry.cost` stays `Int?` in minor units. Totals accumulate in
  `Long`; formatting stays on the `BigDecimal.valueOf(long, 2)` path so money
  never passes through `Float` or `Double`.
- Year is the calendar year of `entry.date` (`LocalDate.year`), the date the
  user recorded for the work. There is no timezone conversion because the stored
  value is already a local date.
- Null cost means "not recorded", not zero: it is excluded from every sum and
  cannot by itself create a year row.
- `VehicleCostTotals.byYear` is sorted by year descending and contains no
  duplicate years. `allTime` equals the sum of `byYear` totals.
- Totals are derived per the overview's "Derived, never stored" rule and are
  recomputed from the flow on every store change, so a newly logged service
  updates them without a manual refresh.

## Testing

- JVM unit tests (`./gradlew testDebugUnitTest`) carry the calculation: empty
  input, all-null costs, mixed costs, multi-year grouping, ordering, and an
  over-`Int.MAX_VALUE` sum. This is where the correctness of the feature lives.
- Instrumented Compose tests (`./gradlew connectedDebugAndroidTest`) cover the
  displayed total, the year breakdown sheet, and the no-costs message. These
  need a device or emulator.
- `./gradlew lintDebug` must gain no new warnings.
- There is no combined Verify command and no browser test harness in this
  project, so the final gate is `assembleDebug`, `testDebugUnitTest`, and
  `lintDebug`, plus the instrumented run when a device is available.

## Notes for the AI

- Reuse what exists. `formatCost`, `formatMileage`, `DetailLine`, `SheetAction`,
  `ModalBottomSheet`, and `CenteredColumn` are already in these files; do not add
  parallel versions.
- Keep the calculation a pure top-level function with no Android dependency so
  it stays in `app/src/test/`, the same shape as `MaintenanceStatus.kt` and
  `ReminderPlanner.kt`.
- Follow the existing comment style: comments explain why a rule exists (why
  null cost is not zero, why the accumulator is `Long`), not what the line does.
- Light and dark mode must both work; take colors from `MaterialTheme` only.
- Respect large font scales: the totals row must wrap or ellipsize rather than
  clip, consistent with the horizontally scrolling actions row above it.
- The placement decision (summary row on vehicle detail, year breakdown in a
  bottom sheet) is an internal UI choice made to match the screen's existing
  idioms. It changes no stored data or contract and can be revised in review.
