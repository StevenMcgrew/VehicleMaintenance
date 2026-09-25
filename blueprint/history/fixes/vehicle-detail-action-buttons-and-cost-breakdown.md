# Current Feature

## Vehicle detail action buttons and cost breakdown screen

**Type:** Fix
**Status:** verified
**Branch:** fix/vehicle-detail-action-buttons-and-cost-breakdown

### The problem

The vehicle detail screen (`maintenance/VehicleDetailScreen.kt`) opens with a
row of plain `TextButton`s ("Service history", "Log repair") followed by a
separate full-width "Total spent" row that opens a bottom sheet of per-year
totals. The user wants:

- The two text buttons restyled as extra-small, fully round Material buttons.
- The "Total spent" row and its bottom sheet removed.
- A third extra-small round button, "Total spent", in the same row, that opens
  a dedicated screen breaking down the vehicle's costs.

What is stored today: each `ServiceLogEntry` carries its own optional `cost`
(integer minor units) plus `date`, `description`, and whether it was a tracked
service or an ad-hoc repair (`maintenanceItemId` null). Nothing is stored as a
total. `costTotalsOf` in `servicelog/CostTotals.kt` derives the all-time total
and per-calendar-year totals at read time; entries without a cost are left out
of every sum.

### The fix

**Buttons.** The project resolves Material 3 `1.4.0` from the Compose BOM, which
does not ship the Expressive `ButtonDefaults.ExtraSmall*` size tokens. Build the
extra-small size from stable APIs instead of pinning an alpha:
`FilledTonalButton` (default shape is already fully round) with a 32dp container
height, 12dp horizontal content padding, and `labelLarge` text, matching the
M3 Expressive XS spec. Wrap it in one private `ExtraSmallButton` helper in the
detail screen so the three buttons stay identical. Labels only, no icons: the
project ships `material-icons-core`, which has no money or history glyph, and
adding `material-icons-extended` is not worth it for this. The row keeps its
horizontal scroll so large font scales cannot clip a button. The 48dp touch
target is preserved by the button's own minimum interactive size.

**Cost breakdown screen.** New route `vehicles/{vehicleId}/costs` opening a
`CostBreakdownScreen` in `servicelog/`, backed by a `CostBreakdownViewModel`
that follows the `ServiceHistoryViewModel` pattern (vehicle lookup, loading,
load-failed with retry, vehicle-not-found). Top bar title "Total spent" with a
back arrow. Content:

- All-time total at the top.
- One section per calendar year, newest first: the year and its subtotal as the
  header, then each costed entry in that year (date, description, cost),
  newest first.
- Entries with no cost are left out, as they are from the totals today.
- Empty state "No costs recorded yet" when nothing has a cost. The button is
  always enabled, so this state is reachable.

Extend `YearCost` with the year's costed entries (newest first) so
`costTotalsOf` stays the single place that groups and sums; the screen only
formats. Money still flows through `formatCost`, never a floating point type.

**Cleanup.** Remove `CostTotalsRow`, `CostTotalsSheet`, the
`showingCostTotals` state, and `costTotals` from `VehicleDetailUiState` and
`VehicleDetailViewModel`. Remove the `cost_totals_title` string once unused.
Reuse `cost_total_label` ("Total spent") for the button and screen title.

**Must not break:** Service history and Log repair navigation, the newly
overdue dialog, item actions sheet, the FAB, and the reminder deep link into
the detail screen. Light and dark mode both render correctly.

### Build steps

- [x] **Step 1: Cost breakdown screen and route.** Extend `YearCost` with its entries and
   update `CostTotalsTest`. Add `CostBreakdownViewModel`, `CostBreakdownScreen`
   (with `@Preview`), the `Routes.VEHICLE_COSTS` route and
   `Routes.vehicleCosts(id)` helper, and an `onViewCosts` callback on
   `VehicleDetailScreen`/`VehicleDetailContent` wired in `VehicleMaintenanceApp`.
   Add a Compose UI test for the screen (years, subtotals, entries, all-time,
   empty state).
   *Done when:* unit tests pass and the new screen test passes, with the screen
   reachable through the new callback.

- [x] **Step 2: Extra-small round action row; remove the totals row.** Add the
   `ExtraSmallButton` helper, render Service history, Log repair, and Total
   spent with it, and delete the totals row, sheet, and view model field. Update
   `VehicleDetailScreenTest`: replace the totals-row and sheet tests with "the
   action row offers all three buttons" and "tapping Total spent asks to open
   the cost breakdown"; drop the no-cost test from this screen (it now lives in
   the breakdown screen test).
   *Done when:* the detail screen shows three compact pill buttons in one row,
   no "Total spent" row below them, and `./gradlew testDebugUnitTest lintDebug`
   plus the instrumented tests pass.

### Verify

- `JAVA_HOME=/opt/android-studio/jbr ./gradlew testDebugUnitTest lintDebug`
- `JAVA_HOME=/opt/android-studio/jbr ./gradlew connectedDebugAndroidTest` on a
  running emulator.
- On the emulator, open a vehicle with costed services in two years plus one
  uncosted entry:
  - Three small round tonal buttons sit in one row at the top; no totals row.
  - Service history and Log repair open their screens as before.
  - Total spent opens the breakdown: all-time total, each year's subtotal, and
    that year's costed entries; the uncosted entry is absent.
  - A vehicle with no costs shows "No costs recorded yet".
  - Check light and dark mode, and a large font scale (row scrolls, no clipping).
