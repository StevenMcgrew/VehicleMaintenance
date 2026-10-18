# Store listing keywords

**Type:** Fix
**Status:** verified
**Branch:** fix/store-listing-keywords

Change 5 of 6 from `blueprint/context/tester-feedback-plan.md`.

## The problem

Play testers reported that the store description does not work in the search
terms people actually type, which limits how often the app shows up in Play
search. The current copy in `play-store/listing.md` says "vehicle" and
"service" throughout, while people mostly search for "car maintenance", "oil
change reminder", or "car service log". It also predates the theme switch and
Help screen.

## The fix

Edit `play-store/listing.md` only. No app code, no strings, no release build.

- **App name** stays `Vehicle Maintenance Tracker` (27 of 30). It matches the
  launcher label, the share message, and the privacy policy. Swapping in "Car"
  would rank better for car searches but split the brand; that is a separate
  decision if you want it.
- **Short description** (80 max) carries the strongest phrases, because Play
  weighs it heavily and shows it under the title: car maintenance, oil change,
  service reminders, and the offline/private point.
- **Full description** (4000 max) is rewritten around this keyword set, each
  phrase used naturally, at most two or three times, never as a list of bare
  keywords:

  | Primary | Secondary |
  | --- | --- |
  | car maintenance | auto repair log |
  | vehicle maintenance | mileage log |
  | service reminder | car expense / cost tracker |
  | oil change reminder | tire rotation |
  | maintenance log, service history | car, truck, SUV, or motorcycle |

  Structure:
  1. An opening two sentences with the primary phrases, since Play shows only
     the first lines before "More".
  2. **WHAT IT DOES** bullets, one feature each, phrased around a keyword
     (service reminders, oil change and tire rotation tracking, OK / due /
     overdue status, maintenance log of repairs and costs, yearly cost totals,
     PDF service history for resale, backup export and import).
  3. A short bullet for the newer extras: light, dark, or system theme, and
     built-in help.
  4. **PRIVATE BY DESIGN**, kept as it is today (no account, no ads, no
     tracking, no internet use).
  5. The existing closing lines about reminders without opening the app and
     miles as the unit.
- Play rules respected: no "best", "#1", "free" in the title, no prices or
  promotions, no testimonials, no other apps named, no keyword stuffing, and
  every claim true of the current build.
- Re-measure every block with a script (Unicode code points, as Play counts)
  and update the "N of M characters" lines and the measured date.
- Add a short **Keywords** note under the full description listing the target
  phrases, so later edits keep them in.
- **Tags** row: name the Console tags to pick (Car maintenance, Vehicle
  management, or the closest the Console offers).

Must not break: everything else in `listing.md` (categorization table, contact
and privacy URL rows, graphics table).

## Build steps

- [x] 1. **Rewrite the listing copy.** New short and full descriptions, the
   Keywords note, the Tags row, and re-measured counts.
   Done when: each block is within Play's limit by a scripted count, every
   primary phrase appears in the short or full description, no phrase appears
   more than three times, and every feature named exists in the app today.

## Verify

- Scripted counts printed for app name, short description, and full
  description, all within 30 / 80 / 4000.
- A scripted phrase count shows each primary keyword present and none over
  three uses.
- Read-through: every bullet matches a real screen or behavior in the app.
- You paste the new text into **Grow users > Store presence > Main store
  listing** in the Play Console yourself; nothing is published from here.
