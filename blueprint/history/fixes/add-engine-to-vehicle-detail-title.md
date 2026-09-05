# Current Feature

**Add engine to the vehicle detail title**

**Type:** Fix
**Status:** verified
**Branch:** `fix/add-engine-to-vehicle-detail-title`

## The problem

`VehicleDetailContent` in
`app/src/main/java/com/example/vehiclemaintenance/maintenance/VehicleDetailScreen.kt:100-102`
builds its top bar title from `R.string.vehicle_summary`, which formats only
year, make, and model. The vehicle's `engine` is missing, so the detail screen
title reads "2014 Toyota Tacoma" instead of "2014 Toyota Tacoma 4.0L V6".

`vehicle_summary` is shared with `VehicleListScreen.kt:223`, where engine is
already rendered on its own line under the summary. Changing that string in
place would break the list card layout, so the detail title needs its own
format string.

## The fix

- Add a new string, `vehicle_summary_with_engine`, formatted as
  `%1$d %2$s %3$s %4$s`, to `app/src/main/res/values/strings.xml`.
- Use it for the `title` value in `VehicleDetailContent`, passing
  `it.year`, `it.make`, `it.model`, `it.engine`.

Must not break:

- `VehicleListScreen` keeps using `vehicle_summary` unchanged.
- The title still falls back to `R.string.maintenance_title` when the vehicle is
  null, and keeps `maxLines = 1` with ellipsis overflow for long engine names.
- `VehicleDetailScreenTest` builds its expected title from the same resource, so
  update its `getString` call to the new string and argument list.

## Build steps

1. [x] Add `vehicle_summary_with_engine` to `strings.xml`, switch the
   `VehicleDetailContent` title to it, and update the expected title in
   `VehicleDetailScreenTest`.
   **Done when:** the detail screen top bar shows year, make, model, and engine,
   the vehicle list card is unchanged, and `./gradlew assembleDebug lintDebug`
   passes with no new warnings.

## Verify

Ran and passing:

| Command | Result |
| --- | --- |
| `./gradlew assembleDebug testDebugUnitTest lintDebug` | exit 0, BUILD SUCCESSFUL, no new lint warnings |

Not run: `./gradlew connectedDebugAndroidTest`. No `adb` on PATH and no device
attached in this session, and `verification.uiEvidence` is `when-available`. The
updated assertion in `VehicleDetailScreenTest` is therefore unexecuted.

Manual path: open a vehicle from the list. The top bar reads
`2014 Toyota Tacoma 4.0L V6` rather than `2014 Toyota Tacoma`. The list card is
unchanged: the summary line, then the engine on its own line below it.

## Notes for the AI

`vehicle_summary` stays as it was because `VehicleListScreen` still uses it for
the card's primary line, with the engine on a separate line beneath. The detail
title needed a fourth argument, so it got its own
`vehicle_summary_with_engine` string rather than a shared format change.

The title keeps `maxLines = 1` with ellipsis overflow, so a long engine name
truncates on a narrow screen instead of wrapping.
