# Store listing

Copy each block into **Grow users > Store presence > Main store listing** in the
Play Console. Character counts were measured on 2026-10-18 against Play's limits.
Recount after any edit.

## App name

27 of 30 characters. The launcher label on the phone stays
"Vehicle Maintenance Tracker App", which is one character too long for the store
title.

```text
Vehicle Maintenance Tracker
```

## Short description

79 of 80 characters.

```text
Car maintenance log with oil change and service reminders. Offline and private.
```

## Full description

1779 of 4000 characters. Play shows the bullet characters as written.

```text
Car maintenance made simple: get a service reminder when an oil change or tire rotation is due, and keep a maintenance log of every repair and what it cost.

Vehicle Maintenance Tracker works for any car, truck, SUV, or motorcycle. Set up the services you care about, and the app tells you when each one is due or overdue. Every service and repair you log becomes a permanent service history, with the date, mileage, and cost.

WHAT IT DOES
• Service reminders: a notification when a service comes due, repeated every two weeks until you log it
• Oil change reminder and tire rotation tracking: set each service by miles, by time, or both, such as every 5,000 miles or 6 months
• See what is OK, due, or overdue at a glance, with the next due date and miles left
• Mileage log: update the odometer at any time, and every service is checked against the new reading
• Auto repair log: record completed services and one-off repairs with date, odometer, cost, and notes
• Car cost tracker: what each vehicle has cost, all time, by year, and as an average per year
• A service history for each vehicle, grouped by year, newest first
• Turn a vehicle's history into a PDF you can save, print, or share, which is handy when you sell
• Export your records to a file you choose, and import them again on a new phone
• Track as many vehicles as you own

ALSO INCLUDED
• Light, dark, or system theme
• Built-in help that explains reminders, statuses, and mileage

PRIVATE BY DESIGN
• No account and no sign-in
• No ads, no tracking, and no analytics
• The app does not use the internet. Your records stay on your phone unless you export or share them.

Reminders still work if you never open the app, and the due list works on its own if you turn notifications off.

Distances are in miles.
```

### Keywords

The copy above is written around these search phrases. Keep them in when
editing, each used naturally and no more than three times per block, never as a
bare list (Play treats that as keyword stuffing).

- Primary: car maintenance, vehicle maintenance, service reminder, oil change
  reminder, maintenance log, service history
- Secondary: auto repair log, mileage log, car cost tracker, tire rotation, car,
  truck, SUV, motorcycle

## Categorization and details

| Field | Value |
| --- | --- |
| App or game | App |
| Category | Auto & Vehicles |
| Tags | Up to five from the Console list. Pick the closest to car maintenance, vehicle management, and car care that it offers |
| Contains ads | No |
| In-app purchases | No |
| Price | Free |
| Contact email | Required by Play, and shown on the listing. Use the same address as the privacy policy |
| Website | Optional, leave blank |
| Privacy policy URL | The hosted copy of `play-store/privacy-policy.html` |

## Graphics

All files are in `play-store/graphics/`:

| Asset | File | Play requirement |
| --- | --- | --- |
| App icon | `icon-512.png` | 512 x 512 PNG |
| Feature graphic | `feature-graphic.png` | 1024 x 500 PNG or JPEG, no alpha |
| Phone screenshots | `screenshot-*.png` | 2 to 8 images, 1080 x 1920 here, no alpha |

The screenshots show only the fictional data in `play-store/sample-data.json`.

Each screenshot is a raw capture from `raw/` placed under a caption on the
brand blue, using `screenshot-template.svg`. The captions live in
`make-screenshots.sh`. To refresh them after a UI change, recapture the raw
screens at `adb shell wm size 1080x1920` (then `wm size reset`), keep the same
file names in `raw/`, and run `./play-store/graphics/make-screenshots.sh`.
