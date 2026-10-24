# Current Feature

**Branch:** `feature/cost-breakdown-chart`

**Status:** verified

Build-plan item 13, **Cost breakdown chart**, from the tester feedback report
(`blueprint/context/vehicle_feedback.pdf`, item B in
`blueprint/context/tester-feedback-plan.md`).

## Goal

Show where a vehicle's money went. The service history screen and the service history PDF get a
pie chart of all-time spend at the top. Each tracked maintenance item gets its own slice, so every
oil change ever logged adds up to one "Oil change" slice. All ad-hoc repairs are summed into a
single "Additional repairs" slice. A legend gives each slice's name, percent, and amount, so the
chart never relies on color alone.

## In scope

- A pure, JVM-tested breakdown computed alongside the existing cost totals in `serviceHistoryOf`
- A chart color palette for light and dark mode, provided through `VehicleMaintenanceTheme` the
  same way `StatusColors` is
- A "Cost breakdown" section above the All time / Average per year rows on the service history
  screen: a heading, the pie drawn with Compose `Canvas`, and a legend
- The same section in the PDF, after the header and before the cost summary, drawn with
  `android.graphics.Canvas`
- New string resources for the heading, the "Additional repairs" name, percent text, and the
  chart's content description
- One sentence added to the Help screen's service history paragraph

## Out of scope

- Any chart library or new dependency. Both renderers draw their own arcs, and the PDF needed
  Android Canvas regardless
- Per-year charts, bar charts, tapping a slice, animation, or filtering
- Grouping ad-hoc repairs by name. They are always one slice
- Changes to stored data, the JSON schema, backup format, or the log forms
- Recapturing Play Store screenshots or editing the store listing (a later `/fix` if wanted)
- Onboarding (tester item A), photos (item 11), cloud backup (item 12)

## Build loop

`workflow.stepReview` is `feature` and `workflow.checkpointCommits` is `disabled`. Implement
every step below in order, keeping the project building after each one, then present one
review packet for the whole feature. Do not commit between steps. `/complete` creates the
single feature commit after review.

## Build steps

- [x] **1. Breakdown model.** Add `servicelog/CostBreakdown.kt` with the `CostSlice` type and
  a pure `costBreakdownOf(entries)` following the contract under Data / contracts. Add
  `breakdown: List<CostSlice>` to `ServiceHistory` (default `emptyList()` so existing
  constructions such as `ServiceHistoryUiState`'s default keep compiling) and fill it in
  `serviceHistoryOf`. Add `app/src/test/.../servicelog/CostBreakdownTest.kt`.
  **Done when** `./gradlew testDebugUnitTest` passes with new tests covering: name grouping
  that ignores case and extra spaces; display name taken from the newest entry; ad-hoc
  entries summed into one slice; entries whose item was deleted (link cleared) landing in
  Additional repairs; uncosted and zero-cost entries left out; a group whose costs are all
  zero omitted; ordering by total, then name, with Additional repairs last; whole-percent
  half-up rounding and the under-1% case; a single slice at 100%; no costs giving an empty
  list; totals beyond `Int.MAX_VALUE` accumulating as `Long`.

- [x] **2. Chart palette.** Add `ui/theme/ChartColors.kt`: an `@Immutable ChartColors` with
  eight slice colors plus a neutral `additionalRepairs` color, `LightChartColors`,
  `DarkChartColors`, and `LocalChartColors`, provided in `Theme.kt` next to
  `LocalStatusColors`. Define any new tones in `Color.kt`. Add a pure
  `sliceColorIndices(trackedCount: Int, hasAdditionalRepairs: Boolean): List<Int>` (in
  `CostBreakdown.kt` or `ChartColors.kt`) following the color rule under Data / contracts, with
  unit tests. **Done when** unit tests pass and `./gradlew assembleDebug` succeeds.

- [x] **3. Screen chart.** Add `servicelog/CostBreakdownChart.kt`: a stateless
  `CostBreakdownChart(slices, modifier)` composable with the heading, pie, and legend described
  under Data / contracts, plus light and dark `@Preview`s. In `ServiceHistoryContent`, insert
  `item(key = "breakdown")` before `item(key = "summary")`, only when
  `history.breakdown` is not empty. Add the strings. Add the breakdown to the existing
  preview data so the main preview shows it. Extend `ServiceHistoryScreenTest` (reuse its
  existing temp-file seeding; never `filesDir`).
  **Done when** `./gradlew assembleDebug` and `./gradlew lintDebug` pass, and
  `ServiceHistoryScreenTest` passes on the emulator with new tests showing: the heading and a
  legend row per slice, with the right name, percent, and amount, sit above the All time row; ad-hoc
  repairs show as one "Additional repairs" row; a history with nothing costed shows no
  breakdown section (the existing "No costs recorded yet" test still passes).

- [x] **4. PDF chart.** Add `ReportSlice` and `breakdown: List<ReportSlice>` to `HistoryReport`,
  filled by `historyReportOf` (formatted cost and percent; `name` null for Additional
  repairs). Add `costBreakdown` and `additionalRepairs` labels plus a percent formatter to
  `HistoryPdfLabels`. In `HistoryPdfLayout.pages()` call a new `breakdown()` between `header()`
  and `summary()`, following the PDF layout under Data / contracts. Extend `HistoryReportTest`
  and `HistoryPdfTest`.
  **Done when** unit tests pass, and `HistoryPdfTest` passes on the emulator with new tests
  showing: a costed history with a few slices still fits one page; a history with more legend
  rows than fit under the pie on page 1 writes without error and runs onto another page; an
  uncosted history writes with no breakdown section (existing test still passes). Save one
  sample PDF from the debug build and look at it to confirm the pie and legend line up.

- [x] **5. Help text and final checks.** Add one sentence to `help_history_body` mentioning the
  pie chart of spending by service. Check the chart in light, dark, and the largest font size
  on the emulator. **Done when** `./gradlew testDebugUnitTest lintDebug assembleDebug` passes
  and `HelpScreenTest` still passes.

## Files / areas

- `app/src/main/java/com/example/vehiclemaintenance/servicelog/CostBreakdown.kt` (new)
- `app/src/main/java/com/example/vehiclemaintenance/servicelog/CostBreakdownChart.kt` (new)
- `app/src/main/java/com/example/vehiclemaintenance/servicelog/ServiceHistory.kt`
- `app/src/main/java/com/example/vehiclemaintenance/servicelog/ServiceHistoryScreen.kt`
- `app/src/main/java/com/example/vehiclemaintenance/servicelog/HistoryReport.kt`
- `app/src/main/java/com/example/vehiclemaintenance/servicelog/HistoryPdf.kt`
- `app/src/main/java/com/example/vehiclemaintenance/ui/theme/ChartColors.kt` (new),
  `Color.kt`, `Theme.kt`
- `app/src/main/res/values/strings.xml`
- Tests: `app/src/test/.../servicelog/CostBreakdownTest.kt` (new), `HistoryReportTest.kt`,
  `ServiceHistoryTest.kt` (only if its `ServiceHistory` equality assertions need the new field);
  `app/src/androidTest/.../servicelog/ServiceHistoryScreenTest.kt`, `HistoryPdfTest.kt`

`ServiceHistoryViewModel` and `HistoryPdfFiles` need no change: both already pass the whole
`ServiceHistory` through, and `HistoryPdfFiles.reportOf` calls `historyReportOf`.

## Data / contracts

Nothing is stored. The breakdown is derived from `ServiceLogEntry` on every emission, like the
existing totals.

**`CostSlice`**

```kotlin
data class CostSlice(
    /** The newest grouped entry's description, trimmed; null for the Additional repairs slice. */
    val name: String?,
    /** Minor units, always > 0. */
    val total: Long,
    /** Whole percent of the breakdown's sum, rounded half up; 0 means more than 0 but under 1%. */
    val percent: Int,
)
```

A null `name` is the Additional repairs slice. The UI and PDF supply the localized
"Additional repairs" wording, matching how `HistoryReport` keeps labels out of the model.

**`costBreakdownOf(entries: List<ServiceLogEntry>): List<CostSlice>`**

1. Keep only entries with a non-null `cost` greater than 0. A null cost means "not recorded",
   and a $0 entry has no area to draw.
2. Split by `maintenanceItemId`. Entries where it is set are **tracked**. Entries where it is
   null are **additional repairs**. This includes entries whose item was deleted, because
   `MaintenanceItemRepository` clears the link on delete and keeps the history as an ad-hoc
   repair.
3. Group tracked entries by key = `description.trim()`, runs of whitespace collapsed to one
   space, `lowercase(Locale.ROOT)`. The slice `name` is the trimmed description of the group's
   newest entry: latest `date`, with ties going to the entry that comes first in the input, the
   same stable rule `serviceHistoryOf` uses.
4. Sum each group and the additional repairs as `Long`.
5. Order tracked slices by `total` descending, then `name` case-insensitively ascending. Put
   the Additional repairs slice last when it exists.
6. `percent` = `(total * 100 + sum / 2) / sum`, where `sum` is the total of all slices, which
   equals `ServiceHistory.allTime` after the zero-cost filter. If that rounds to 0, keep 0 and
   render it as "<1%". Rounded percents may not add up to exactly 100, and the legend does
   not adjust them.
7. No qualifying entries → empty list → no chart anywhere.

Money stays integer. Only the drawing converts a slice to an angle
(`360f * total / sum`).

**Colors**

- Eight pastel slice colors, shared by light and dark mode (owner's choice at review, replacing
  the original saturated set): SchemeColor's "Light Colored Pie Chart" (Icy Blue `#9BBFE0`, Rose
  Gold `#E8A09A`, Gold Blossom `#FBE29F`, Celery `#C6D68F`), then Lavender `#C3B1E1`, Seafoam
  `#9FD8CB`, Pink `#F2B5D4`, and Peach `#F7C8A0`. Additional repairs uses a soft grey `#C8CDD3`
  so it reads as a catch-all and never matches a tracked color. Pastels trade contrast against a
  light background for softness; the legend's names and amounts carry the meaning.
- `sliceColorIndices`: tracked slice `i` gets `i % 8`. If the last tracked slice would touch the
  first slice around the circle with the same color (no Additional repairs slice between them,
  more than one slice, and `(trackedCount - 1) % 8 == 0`), it gets index 1 instead. Colors
  repeat beyond eight items. The legend's names and amounts carry the meaning.
- Composables read colors from `LocalChartColors`, never hex values. The PDF uses
  `LightChartColors` through `.toArgb()`, since paper is always white.

**Screen layout** (`CostBreakdownChart`)

- Heading: string `cost_breakdown` = "Cost breakdown", `titleMedium`, using the same horizontal
  padding as the existing `AmountRow`s.
- Pie: centered, 160.dp square, slices drawn clockwise from 12 o'clock in slice order. A
  2.dp separator in `MaterialTheme.colorScheme.surface` sits between slices, with none when
  there is only one. The Canvas has `contentDescription` = string `cost_breakdown_chart`
  ("Pie chart of spending by service"). The legend carries the data for screen readers.
- Legend: one row per slice in slice order, with a 12.dp color swatch, the name (`bodyLarge`,
  `maxLines = 2`, ellipsis), the percent, and the amount right-aligned via `formatCost`. A
  null name uses `cost_breakdown_additional_repairs` = "Additional repairs". The percent uses
  `cost_breakdown_percent` = "%1$d%%", or `cost_breakdown_under_one_percent` = "&lt;1%"
  when 0. The swatch is decorative and the row's text says everything.
- The description is user text, so it renders only as plain `Text` (no HTML or markdown
  parsing). In the PDF it goes into a `StaticLayout`.
- The whole section is one `LazyColumn` item followed by a `HorizontalDivider`. It must stay
  usable at the largest system font size: legend rows wrap or ellipsize and never clip the
  amount.

**PDF layout** (`HistoryPdfLayout.breakdown()`, skipped when `report.breakdown` is empty)

- Heading "Cost breakdown" in `YEAR_PAINT`, then a 140pt pie centered in the content width,
  with a white 1pt separator between slices.
- The heading, the pie, and at least the first legend row are kept together. If they don't
  fit in what remains of the page, start a new page. In practice they always fit on page 1
  under the header.
- Legend rows go below the pie at full content width: an 8pt swatch, the name in
  `BODY_PAINT` capped at 2 lines with an ellipsis, then the percent and the amount
  right-aligned. Each row goes through `place()`, and a row that does not fit starts a new
  page. Rows are never split, and no "continued" heading is needed.
- Then `SECTION_GAP`, then the existing summary.

**Report additions**

```kotlin
data class ReportSlice(
    /** Null for the Additional repairs slice; the PDF supplies the wording. */
    val name: String?,
    /** Already formatted with formatCost for the locale. */
    val cost: String,
    /** Whole percent; 0 means under 1%. */
    val percent: Int,
    /** Share of the circle, 0..1, for drawing only. */
    val fraction: Float,
)
```

## Testing

Commands (prefix with `JAVA_HOME=/opt/android-studio/jbr`):

- Unit: `./gradlew testDebugUnitTest`. Baseline before this feature: 214 tests, 0 failures
  (observed 2026-10-24).
- Lint and build: `./gradlew lintDebug assembleDebug`
- Instrumented, one class per run (a comma-separated list runs only the first):
  `./gradlew connectedDebugAndroidTest -Pandroid.injected.androidTest.leaveApksInstalledAfterRun=true -Pandroid.testInstrumentationRunnerArguments.class=com.example.vehiclemaintenance.servicelog.ServiceHistoryScreenTest`
  and the same for `HistoryPdfTest` and `HelpScreenTest`.

**Before any instrumented run**, back up the emulator's real store
(`files/vehicle-maintenance.json` in `com.stevenmcgrew.vehiclemaintenancetrackerapp.debug`) with
`~/Android/Sdk/platform-tools/adb exec-out run-as ... cat ...`. New instrumented tests must use
a temp file in `cacheDir`, never `filesDir` or the application container.

There is no combined Verify command and no CI. Visual checks (light, dark, large font, the
sample PDF) are manual evidence. Report them as observed, or as not run.

## Notes for the AI

- Follow the existing style in `ServiceHistory.kt` and `HistoryPdf.kt`: KDoc on types and
  non-obvious rules, integer money, named constants for PDF geometry.
- `ServiceHistory(null, null, emptyList())` is used positionally in `ServiceHistoryUiState`.
  Put `breakdown` last with a default so that stays valid.
- Keep `CostBreakdownChart` stateless. It takes `List<CostSlice>` and reads nothing else.
- The PDF's `place()` queues draw operations per page. The pie is one operation and each legend
  row is another.
- Do not add a dependency. If a Canvas arc API behaves differently between Compose and Android
  Canvas (sweep direction, start angle), match both to "clockwise from 12 o'clock".
- `sliceColorIndices` and the percent math are where off-by-one bugs hide. Test them directly.
