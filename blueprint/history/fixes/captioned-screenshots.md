# Captioned screenshots

**Type:** Fix
**Status:** verified
**Branch:** fix/captioned-screenshots

Change 6 of 6 from `blueprint/context/tester-feedback-plan.md`.

## The problem

Play testers said the store screenshots are plain phone captures that do not
explain what the app does. Captioned feature screenshots convert better because
a browser of the listing can read the benefit without opening the app. The
current captures are also out of date: the vehicle list now has the three-dot
menu instead of Backup data and Privacy buttons.

## The fix

Edit `play-store/graphics/` and `play-store/listing.md` only. No app code.

- **Recapture** fresh raw screens from the current debug build with the
  fictional `play-store/sample-data.json`, at a temporary `wm size 1080x1920`
  (reset afterwards), into `play-store/graphics/raw/`. Never show the owner's
  real data:
  1. Back up the debug app's store with `run-as` to the scratchpad, as a safety
     copy only.
  2. Force-stop the app, write the sample data over its store, and capture.
  3. Leave the sample data in place as the new debug data, at the owner's
     request (2026-10-18). Leave the theme setting on System, as it was.
- **Five captioned screenshots**, 1080 x 1920. The first four keep today's file
  names; the fifth is renamed to `screenshot-5-theme-dark.png` because it now
  shows the theme menu (owner's request, 2026-10-18). The listing table uses
  `screenshot-*.png`, so it stays valid. Each is the brand blue (`#007AFF`, as in the
  feature graphic) with a short white caption at the top and the raw capture
  scaled to about 80% below it, with rounded corners and a soft shadow:

  | File | Screen | Caption |
  | --- | --- | --- |
  | `screenshot-1-vehicles.png` | Vehicle list | Every car, truck, and motorcycle in one place |
  | `screenshot-2-due-status.png` | Vehicle detail with OK, Due, Overdue | See what is due or overdue at a glance |
  | `screenshot-3-history.png` | Service history with cost totals | Service history and costs by year |
  | `screenshot-4-log-service.png` | Log service form | Log every service and repair |
  | `screenshot-5-theme-dark.png` | Vehicle list in dark theme with the three-dot menu open on the theme switch | Light or dark, your choice |

  Captions are 2 lines at most, bold, large enough to read at the Play
  thumbnail size, and make no claim the app does not back up.
- **Reproducible**: one SVG template plus `play-store/graphics/make-screenshots.sh`,
  which renders each final PNG from its raw capture and caption with
  `rsvg-convert` and flattens it with `magick` (no alpha), the same tools the
  feature graphic uses. Re-running the script after a future recapture rebuilds
  all five.
- **listing.md**: the Graphics section notes that screenshots are captioned,
  names the raw folder and the script, and the "fictional data" line stays.

Must not break: the emulator's display size and theme (reset), the icon and
feature graphic (untouched).

## Build steps

- [x] 1. **Recapture raw screens.** Back up the store, load the sample data,
   and capture the five screens to `play-store/graphics/raw/`.
   Done when: five 1080 x 1920 raw PNGs exist showing only sample data and the
   current UI, the debug app's store matches `sample-data.json` byte for byte,
   and `wm size` reports the physical size again.
- [x] 2. **Caption and render.** The SVG template, the script, the five final
   PNGs, and the listing.md note.
   Done when: `magick identify` shows each final screenshot as 1080 x 1920 with
   no alpha channel, each caption is legible when the image is shrunk to about
   a quarter size, and the script rebuilds identical output on a second run.

## Verify

- `magick identify play-store/graphics/screenshot-*.png` prints 1080x1920 and
  an opaque (no alpha) type for all five.
- View each final image: caption fits in two lines, nothing clipped, screen
  content matches the caption.
- No real vehicles or costs appear in any image.
- You upload the five PNGs in the Play Console yourself, replacing the old
  ones; nothing is published from here.
