# Pie chart in Play screenshots

**Type:** Fix
**Status:** verified
**Branch:** fix/pie-chart-in-play-screenshots

Follows feature 13 (cost breakdown chart) and builds on the screenshot pipeline from
`blueprint/history/fixes/captioned-screenshots.md`.

## The problem

The Play Store screenshots predate the cost breakdown pie chart. Screenshot 3 shows the
service history without the chart, and screenshot 5 shows the vehicle list with the theme menu.
The owner wants the chart in the listing:

- **Screenshot 3:** the service history in light mode, with the chart at the top.
- **Screenshot 5:** the service history in dark mode, as the dark theme example.

## The fix

Edit `play-store/graphics/` only. No app code. Reuse the existing pipeline: the raw captures
in `raw/`, `screenshot-template.svg`, and `make-screenshots.sh`.

**Recapture only `raw/3-history.png` and `raw/5-theme-dark.png`** from the current debug build,
using the fictional `play-store/sample-data.json` at a temporary `wm size 1080x1920`.

| File | Screen | Caption (proposed) |
| --- | --- | --- |
| `screenshot-3-history.png` | 2018 Toyota RAV4 service history, light theme, top of screen: chart, legend, All time | Service history and / a cost breakdown |
| `screenshot-5-theme-dark.png` | The same 2018 Toyota RAV4 service history in the dark theme, top of screen | Light or dark, / your choice (unchanged) |

- The RAV4 has six slices, which shows the chart best. Screenshot 5 uses the same vehicle and
  data as screenshot 3, so the pair shows one screen in both themes (owner's choice at review,
  replacing an F-150 capture).
- File names stay the same, so `make-screenshots.sh` keeps its five entries and the
  `listing.md` table (`screenshot-*.png`) stays valid. Only caption line 2 of screenshot 3
  changes in the script. `listing.md` describes no screen by content, so it needs no edit.

**Device state is restored exactly.** The emulator's debug store is currently 4,523 bytes and
does not match `sample-data.json`. The app's theme setting is `LIGHT`, which the owner set
on 2026-10-24.

1. Force-stop the app. Back up `files/vehicle-maintenance.json` and
   `shared_prefs/settings.xml` with `run-as` to the scratchpad, and record their SHA-256.
2. Write `sample-data.json` over the store, then capture: light theme for 3. For 5, use the
   app's own Theme menu set to Dark, since the setting is not System, so the system night
   mode would be ignored.
3. Force-stop, restore both backed-up files, and run `wm size reset`. Confirm both hashes
   match the backup and `wm size` reports only the physical size.

Must not break:
- Screenshots 1, 2, and 4 must stay byte-identical. The script strips dates, so a rerun with
  unchanged raws reproduces them.
- The icon, the feature graphic, the owner's emulator data, the theme setting, and the display
  size must all end up as they were.

Out of scope: rewording the store description to mention the chart, and screenshots 1, 2,
and 4.

## Build steps

- [x] 1. **Recapture the two raw screens.** Back up the store and settings, load the
   sample data, capture `raw/3-history.png` (light) and `raw/5-theme-dark.png` (dark), then
   restore. **Done when:** both raws are 1080 x 1920 and show only sample data with the chart
   at the top. The store and settings hashes match the backup, `wm size` is reset, and the
   app opens in light mode with the owner's data.
- [x] 2. **Re-render.** Update caption line 2 for screenshot 3 in `make-screenshots.sh`
   and run it. **Done when:** `git status` shows only `raw/3-history.png`,
   `raw/5-theme-dark.png`, `screenshot-3-history.png`, `screenshot-5-theme-dark.png`, and
   `make-screenshots.sh` changed under `play-store/`. `magick identify` reports both new
   screenshots as 1080 x 1920 with no alpha channel. A second run of the script changes nothing.

## Verify

- `git status` and `git diff --stat` show screenshots 1, 2, and 4 untouched.
- View screenshots 3 and 5. The caption fits in two lines, nothing is clipped, the chart and
  legend are fully visible, and 5 is clearly dark.
- No real vehicles or costs appear, only `sample-data.json` content.
- You upload the two new PNGs in the Play Console yourself, replacing the old 3 and 5.
  Nothing is published from here.
