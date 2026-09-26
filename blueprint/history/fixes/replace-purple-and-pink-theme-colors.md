# Current Feature

## Replace purple and pink theme colors with blue and grey

**Type:** Fix

**Status:** verified

**Branch:** `fix/replace-purple-and-pink-theme-colors`

## The problem

The app still uses the purple and pink starter palette from the Android Studio
template. The user removed `Purple80` and `PurpleGrey80` from
`ui/theme/Color.kt` and added two blues of their own:

- `PrimaryColor_RoundedButton` = `#0062CC`
- `PrimaryColor_TextButtonAndIconButton` = `#33A2FF`

They want every purple, violet, and pink color replaced with blue, grey,
white, or black shades that fit those two blues and keep good contrast. The
color variables should be renamed to match. The maintenance status labels
should also use traffic-light colors: red for **Overdue**, amber for **Due**,
and green for **OK**.

This leaves six problems:

1. **The build is broken.** `ui/theme/Theme.kt` still references the removed
   `Purple80` and `PurpleGrey80`.
2. **The purple and pink variables are still named and colored purple and
   pink.** These are `Purple40`, `PurpleGrey40`, `Pink40`, and `Pink80`.
3. **Many purple, pink, and violet roles never came from `Color.kt`.**
   `lightColorScheme()` and `darkColorScheme()` fill every role not set in
   `Theme.kt` with Material's baseline colors:
   - Purple: `onPrimary` in dark mode, `primaryContainer`, `secondaryContainer`
     (the lavender fill on the `ExtraSmallButton` pills), and `inversePrimary`.
   - Pink: the `tertiaryContainer` family, and the dark-mode `error` color
     (`#F2B8B5`, a pale pink).
   - Violet-tinted greys: every neutral role (`background`, `surface`, the
     `surfaceContainer*` family behind dialogs, sheets, and the date picker,
     `surfaceVariant`, `outline`, and `outlineVariant`).
4. **The status colors don't read as statuses.** `StatusCell` in
   `VehicleDetailScreen` colors **Overdue** with `colorScheme.error` (pale pink
   in dark mode), **Due** with `colorScheme.tertiary` (pink), and **OK** with
   the same grey as an unset value.
5. **Unused purples in `res/values/colors.xml`.** `purple_200`, `purple_500`,
   and `purple_700` are template leftovers that nothing references.
6. **On Android 12+ none of this shows anyway.** `VehicleMaintenanceTheme`
   defaults to `dynamicColor = true`, so it uses colors taken from the
   wallpaper and ignores `Color.kt`.

## The fix

All colors are generated at fixed Material 3 tone numbers (10 is darkest, 100
is white). The names use the template's convention: a family name plus the
tone number.

### Theme palettes

- **`Blue`** (primary). Uses the hue of `#0062CC`, and `#0062CC` itself is
  `Blue40`. The `#33A2FF` hue is within about 8 degrees, so both user colors
  fit this family.
- **`BlueGrey`** (secondary). A muted version of the same hue.
- **`Azure`** (tertiary, replaces the pinks). A lighter, slightly cyan blue
  that is clearly separate from `Blue`. After this fix no screen reads
  tertiary directly, but Material components may use it, so it still gets a
  full blue family.
- **`Grey`** (neutral). An almost pure grey with the faintest blue cast, in
  place of Material's violet cast.
- **`SlateGrey`** (neutral variant). A slightly stronger blue-grey for
  outlines and secondary text.

| Family | Tones used |
| --- | --- |
| `Blue` | 10 `#001A40`, 20 `#012F67`, 30 `#004492`, 40 `#0062CC`, 80 `#A3C9FE`, 90 `#D1E4FF` |
| `BlueGrey` | 10 `#111C2C`, 20 `#253142`, 30 `#3B485A`, 40 `#525F72`, 80 `#B8C8DE`, 90 `#D4E4FA` |
| `Azure` | 10 `#011E2C`, 20 `#013449`, 30 `#004C69`, 40 `#056589`, 80 `#76D1FF`, 90 `#C0E8FF` |
| `Grey` | 4 `#0C0E11`, 6 `#121316`, 10 `#1A1C1E`, 12 `#1E2022`, 17 `#282A2D`, 20 `#2E3133`, 22 `#333538`, 24 `#37393C`, 87 `#D7DADE`, 90 `#E0E3E6`, 92 `#E6E8EC`, 94 `#EBEEF2`, 95 `#EEF1F5`, 96 `#F1F4F8`, 98 `#F7FAFD` |
| `SlateGrey` | 30 `#41474F`, 50 `#717881`, 60 `#8B919B`, 80 `#C0C7D1`, 90 `#DCE3ED` |

**Renames**

| Old | New |
| --- | --- |
| `Purple40` | `Blue40` |
| `Purple80` (removed) | `Blue80` |
| `PurpleGrey40` | `BlueGrey40` |
| `PurpleGrey80` (removed) | `BlueGrey80` |
| `Pink40` | `Azure40` |
| `Pink80` | `Azure80` |

### Status palettes

Status text sits on the screen background, so each color must reach 4.5:1
there. In light mode that means deeper shades, and the amber reads as a dark
gold. Bright yellow on white cannot reach readable contrast. All three light
shades share one tone (45), and the dark shades are matched as closely as
the hues allow, so no status looks heavier than another. The label still
names the status, so color is never the only signal (important for
red-green color blindness).

| Family | Tones used |
| --- | --- |
| `Red` | 10 `#410002`, 30 `#93000C`, 45 `#D50F1B`, 60 `#FF544C`, 90 `#FEDBD6` |
| `Amber` | 45 `#965E04`, 70 `#EE9803` |
| `Green` | 45 `#017C1F`, 70 `#4DC258` |

| Status | Light | Dark |
| --- | --- | --- |
| **Overdue** | `Red45` | `Red60` |
| **Due** | `Amber45` | `Amber70` |
| **OK** | `Green45` | `Green70` |
| Not set (dash) | `onSurfaceVariant` grey, unchanged | same |

Dark-mode red uses tone 60 rather than the usual 80, because red at 70 and
above turns salmon-pink.

**Wiring.** Material 3 has no success or warning roles, so add a small
`StatusColors` holder (`overdue`, `due`, `ok`) in a new
`ui/theme/StatusColors.kt`. It is exposed through a `LocalStatusColors`
composition local that `VehicleMaintenanceTheme` provides, picking light or
dark from its `darkTheme` parameter. `StatusCell` reads
`LocalStatusColors.current` instead of `error` and `tertiary`. No hex values
go into the screen (per `coding-standards.md`).

The theme's `error` family also moves to `Red`, so that form field errors
match **Overdue** and no longer turn pale pink in dark mode.

### Scheme wiring in `Theme.kt`

Set every color role explicitly in both schemes (standard Material 3 tone
mapping) so no baseline purple, pink, or violet leaks through.

| Role | Light | Dark |
| --- | --- | --- |
| `primary` / `onPrimary` | `Blue40` / white | `Blue80` / `Blue20` |
| `primaryContainer` / `onPrimaryContainer` | `Blue90` / `Blue10` | `Blue30` / `Blue90` |
| `inversePrimary` | `Blue80` | `Blue40` |
| `secondary` / `onSecondary` | `BlueGrey40` / white | `BlueGrey80` / `BlueGrey20` |
| `secondaryContainer` / `onSecondaryContainer` | `BlueGrey90` / `BlueGrey10` | `BlueGrey30` / `BlueGrey90` |
| `tertiary` / `onTertiary` | `Azure40` / white | `Azure80` / `Azure20` |
| `tertiaryContainer` / `onTertiaryContainer` | `Azure90` / `Azure10` | `Azure30` / `Azure90` |
| `error` / `onError` | `Red45` / white | `Red60` / `Red10` |
| `errorContainer` / `onErrorContainer` | `Red90` / `Red10` | `Red30` / `Red90` |
| `background`, `surface`, `surfaceBright` | `Grey98` | `Grey6`, `Grey6`, `Grey24` |
| `onBackground`, `onSurface` | `Grey10` | `Grey90` |
| `surfaceDim` | `Grey87` | `Grey6` |
| `surfaceContainerLowest` / `Low` | white / `Grey96` | `Grey4` / `Grey10` |
| `surfaceContainer` / `High` / `Highest` | `Grey94` / `Grey92` / `Grey90` | `Grey12` / `Grey17` / `Grey22` |
| `inverseSurface` / `inverseOnSurface` | `Grey20` / `Grey95` | `Grey90` / `Grey20` |
| `surfaceVariant` / `onSurfaceVariant` | `SlateGrey90` / `SlateGrey30` | `SlateGrey30` / `SlateGrey80` |
| `outline` / `outlineVariant` | `SlateGrey50` / `SlateGrey80` | `SlateGrey60` / `SlateGrey30` |
| `surfaceTint` | `Blue40` | `Blue80` |

### Contrast

WCAG ratio; 4.5:1 is the minimum for text, 3:1 for outlines.

| Pair | Light | Dark |
| --- | --- | --- |
| primary vs onPrimary | 5.8 | 7.7 |
| primaryContainer vs onPrimaryContainer | 13.3 | 7.3 |
| secondary vs onSecondary | 6.5 | 7.7 |
| secondaryContainer vs onSecondaryContainer | 13.3 | 7.2 |
| tertiary vs onTertiary | 6.5 | 7.8 |
| tertiaryContainer vs onTertiaryContainer | 13.3 | 7.3 |
| error vs onError | 5.4 | 5.4 |
| errorContainer vs onErrorContainer | 13.3 | 7.3 |
| onSurface on surface | 16.3 | 14.4 |
| onSurfaceVariant on surface | 9.0 | 10.9 |
| **Overdue** text on background | 5.1 | 5.9 |
| **Due** text on background | 5.1 | 8.1 |
| **OK** text on background | 5.1 | 8.1 |
| Primary text button in a dialog (`surfaceContainerHigh`) | 4.7 | 8.5 |
| outline vs surface | 4.3 | 5.9 |

### Other changes

- **Dynamic color.** Change the `dynamicColor` default in
  `VehicleMaintenanceTheme` to `false` so the new palette is what users
  actually see on Android 12+. Keep the parameter and the dynamic branch so it
  can be switched back on in one place. Status colors are not affected by
  dynamic color either way.
- **`colors.xml`.** Delete the unused `purple_200`, `purple_500`, and
  `purple_700` entries.

### Leave alone

- The user's `PrimaryColor_RoundedButton` and
  `PrimaryColor_TextButtonAndIconButton`, kept unchanged as the reference
  colors. `Blue40` has the same value as `PrimaryColor_RoundedButton`.
- `scrim`, which is already black.
- The unused `teal_*`, `black`, and `white` entries in `colors.xml`. They are
  not purple or pink.
- Button call sites and other `colorScheme.*` reads in screens. They pick up
  the new colors through the theme.
- The status label text and the status logic in `MaintenanceStatus.kt`.

**Must not break:** the build, light and dark mode, existing previews, and
the status labels.

## Build steps

1. [x] **Blue primary and secondary, build fix**
   - Replace `Purple40` and `PurpleGrey40` in `Color.kt` with the `Blue*` and
     `BlueGrey*` tones.
   - Wire the primary, secondary, and inverse-primary roles in both schemes.
     Keep `tertiary = Pink*` for now.
   - Default `dynamicColor` to `false`.
   - **Done when:** `./gradlew assembleDebug` succeeds and no `Purple`
     identifier remains in `app/src/main/java`.
2. [x] **Azure tertiary**
   - Replace `Pink40` and `Pink80` with the `Azure*` tones.
   - Wire the tertiary and tertiary-container roles in both schemes.
   - **Done when:** it builds and no `Pink` identifier remains in
     `app/src/main/java`.
3. [x] **Grey neutrals and `colors.xml` cleanup**
   - Add the `Grey*` and `SlateGrey*` tones.
   - Wire every neutral and surface role in both schemes.
   - Delete the three `purple_*` entries from `colors.xml`.
   - **Done when:** it builds, `grep -rni "purple\|pink" app/src/main` returns
     nothing, and `lintDebug` gains no new warnings (it should lose the three
     `purple_*` unused-resource warnings).
4. [x] **Traffic-light status colors and red error family**
   - Add the `Red*`, `Amber*`, and `Green*` tones.
   - Add `StatusColors`, the light and dark instances, and
     `LocalStatusColors`. Provide it from `VehicleMaintenanceTheme`.
   - Point `StatusCell` at it.
   - Wire the `error` family in both schemes.
   - **Done when:** it builds, `StatusCell` no longer reads `error` or
     `tertiary`, and the `VehicleDetailScreen` preview shows the three status
     colors.

## Verify

- `./gradlew assembleDebug lintDebug testDebugUnitTest` passes.
- `grep -rni "purple\|pink" app/src/main` returns nothing.
- On the emulator in **light** mode:
  - Text buttons (dialog OK/Cancel, Back, Update mileage) and the Retry and
    Export buttons are blue `#0062CC`.
  - The pill buttons on the vehicle list and detail screens have a pale blue
    fill with dark navy text.
  - Screens and the top bar are a clean off-white. Dialogs, the date picker,
    and the bottom sheet are light grey with no lavender cast.
  - On the vehicle detail screen, **Overdue** is red, **Due** is dark amber,
    **OK** is green, and an unset status dash is grey.
  - A form validation error (for example, saving a vehicle with no name) is
    the same red.
- In **dark** mode:
  - Buttons are light sky blue (`#A3C9FE`), and filled buttons have dark navy
    text.
  - Surfaces are neutral near-black greys.
  - **Overdue** is a clear red (`#FF544C`, not pink), **Due** is orange-amber
    (`#EE9803`), and **OK** is green (`#4DC258`).
  - Nothing is purple, violet, or pink.
