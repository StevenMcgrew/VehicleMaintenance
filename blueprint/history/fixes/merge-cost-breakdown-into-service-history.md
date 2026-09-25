# Current Feature

## Merge cost breakdown into service history

**Type:** Fix
**Status:** verified
**Branch:** fix/merge-cost-breakdown-into-service-history

### The problem

The vehicle detail screen now opens two overlapping screens:

- **Service history** (`servicelog/ServiceHistoryScreen.kt`) lists every log
  entry, newest first, with date, odometer, cost, and notes, but no totals.
- **Total spent** (`servicelog/CostBreakdownScreen.kt`) shows the all-time
  total and per-year subtotals, but lists only the entries that have a cost,
  and shows no mileage or notes.

The user wants a single screen that holds both the history and the costs,
reached from one button.

### The fix

Keep the cost breakdown screen as the base, and have it take over the name,
route, and button of Service history. Then delete the old history screen.

**Merged screen content**, top to bottom:

1. **All time** row: the all-time total, as today.
2. **Average per year** row (new), directly under All time, with an info
   icon button after the label. Calculation (revised on user request): the
   all-time total divided by the number of months between the earliest and the
   latest logged entry (costed or not), then multiplied by 12. Months are
   fractional, measured in days (365.25 / 12 days per month), so a partial month
   is not rounded away. This is equivalent to `total x 365.25 / days`. Worked
   out in integer minor units as a `Long` and rounded half up to the nearest
   cent, never through a floating point type, and derived at read time.
   When the logged entries span fewer than 30 days, the value shows `-`
   instead, because projecting a single bill to a whole year would badly
   overstate it.
   The info icon (`Icons.Outlined.Info`, content description "How the average
   is calculated") opens an `AlertDialog` titled "Average per year" that
   explains the calculation in plain words, including the 30-day minimum, with
   an OK button.
3. **One section per calendar year**, newest first. The header shows the year
   and that year's subtotal. Under it, **every** entry logged that year, newest
   first:
   - Description, with the cost aligned to the end when there is one.
   - Date, then mileage, on the line below: for example `Sep 5, 2026 · 48,000 mi`.
   - Notes, when present, on a third line, as the history screen shows them now.

Changes this makes to what the breakdown shows today:

- **Entries with no cost are now listed.** Without this, deleting the history
  screen would hide every service logged without a cost. These entries still
  add nothing to any total.
- **A year whose entries all lack a cost gets a section too.** Its header shows
  `-` (the existing `value_not_set` string) in place of a subtotal.

**States:**

| Situation | What the screen shows |
|---|---|
| No entries logged | The existing history empty state ("Nothing logged yet" plus its body) |
| Entries exist, none has a cost | All time shows "No costs recorded yet", no Average row, then the year sections |
| At least one cost | All time, Average per year (or `-` under a 30-day span), then the year sections |
| Loading, load failed, vehicle not found | Unchanged from both screens today |

**Renames and removals:**

- `CostBreakdownScreen`, `CostBreakdownContent`, `CostBreakdownViewModel`, and
  `CostBreakdownUiState` become `ServiceHistoryScreen`, `ServiceHistoryContent`,
  `ServiceHistoryViewModel`, and `ServiceHistoryUiState`, replacing the old
  files of those names. The top bar title becomes "Service history".
- The derivation moves from `CostTotals.kt` to `ServiceHistory.kt` as a
  `serviceHistoryOf(entries)` function returning the all-time total, the
  average, and the year sections. The average depends only on logged dates, so
  no current date is needed.
- Routes: keep `Routes.VEHICLE_HISTORY` (`vehicles/{vehicleId}/history`) and
  point it at the merged screen. Remove `VEHICLE_COSTS` and `vehicleCosts()`.
- Detail screen: the action row becomes **Service history** then **Log
  repair**, both still extra-small round tonal buttons. The "Total spent"
  button is renamed Service history and opens the merged screen. Remove the
  `onViewCosts` callback everywhere.
- Strings: add `cost_average_per_year` ("Average per year") and a
  date-and-mileage format string, so the separator is not built by
  concatenation in code. Remove strings that become unused
  (`cost_total_label`, `cost_breakdown_empty_body`).

**Must not break:** Log repair, the item actions sheet, the newly overdue
dialog, the reminder deep link into the detail screen, integer-minor-unit
money, and light and dark mode.

**Known drift:** `blueprint/context/project-overview.md` still lists Service
history and cost totals as separate items and screens. This fix leaves the
user-owned plans alone. Rerun `/overview` after completion if the overview
should match.

### Build steps

- [x] **Step 1: History derivation with average.** Add `serviceHistoryOf` in
  `ServiceHistory.kt` and move `CostTotalsTest` to `ServiceHistoryTest`.
  Cover: all entries listed, uncosted ones excluded from sums, a year with only
  uncosted entries has a section and no subtotal, entries and years newest
  first, same-date order preserved, no `Int` overflow, and the average
  (including gap years, the current year counted, rounding half up, and no
  average when nothing is costed).
  *Done when:* `./gradlew testDebugUnitTest` passes with the new tests.

- [x] **Step 2: One merged screen and one button.** Delete the old history
  screen, view model, and test. Rename the breakdown screen, view model, and
  test to Service history. Add the Average per year row, the date and mileage
  line, notes, uncosted entries, and the new states. Repoint the route, remove
  the costs route and `onViewCosts`, and reduce the detail action row to
  Service history and Log repair. Update `VehicleDetailScreenTest` and
  `AdHocRepairTest` callers, and fold the old history test's checks that still
  apply (only this vehicle's entries, newest first, notes shown, empty state)
  into the merged screen test.
  *Done when:* the detail screen shows two buttons, Service history opens the
  merged screen with every entry and the new rows, and `./gradlew build` plus
  `connectedDebugAndroidTest` pass.

- [x] **Step 3: Monthly-based average and info pop-up** (requested change).
  Replace the calendar-year average with the monthly formula above, drop the
  `currentYear` parameter and the view model's `today`, and update the unit
  tests (span from first to last entry, uncosted entries widen the span,
  fractional months, rounding half up, under 30 days gives no average). Add the
  info icon and dialog with its strings, and cover opening the dialog in the
  screen test.
  *Done when:* the Average per year row shows the new figure with an info icon
  that opens the explanation, and `./gradlew build` plus
  `connectedDebugAndroidTest` pass.

### Verify

- `JAVA_HOME=/opt/android-studio/jbr ./gradlew build`
- `JAVA_HOME=/opt/android-studio/jbr ./gradlew connectedDebugAndroidTest` on
  the running emulator.
- On the emulator, open a vehicle with costed entries in 2025 and 2026, one
  entry with no cost, and one entry with notes:
  - The action row shows Service history and Log repair only.
  - Service history shows All time, then Average per year (all-time total x
    365.25 / days from the first to the last logged entry), then 2026 and 2025
    sections. Tapping the info icon explains the calculation.
  - Every entry appears with date and mileage; the uncosted entry has no amount
    and does not change any total; the note appears under its entry.
  - A vehicle with nothing logged shows "Nothing logged yet".
  - Check light and dark mode.
