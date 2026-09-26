# Vehicle Maintenance - Project Overview

<!-- blueprint:source-hash 3fe9179ee0cc1d7e6728817a9c0aa246cb9181c2e95fe7e3e262fc3c0df6b526 -->

> An offline Android app that reminds a vehicle owner when service is due and
> keeps a permanent, exportable record of what was done and what it cost.

## Problem

Vehicle maintenance gets missed because nothing reliably reminds the owner it is
due. Records live on paper, in a shop's system, or nowhere. Skipped services
shorten vehicle life, and a vehicle with no documented history is worth less at
resale.

The app is a watchdog first and a logbook second: it warns before a service is
late, and it keeps the history that makes the warning worth acting on.

## Users

A single vehicle owner tracking their own vehicles. Typically two or three, with
no enforced limit.

- Not mechanics, not fleet managers. People with no system beyond memory.
- One user per install. No accounts, no sharing, no multi-user access, no tiers.
- Published to the Play Store, so it must work for someone who installs it cold
  with no explanation.

## Features

MVP, in build-plan order. Items 6 and 7 together are the headline: they are the
reason someone installs a watchdog rather than keeping a spreadsheet.

1. **Vehicle list and details** - create and manage the vehicles being tracked, backed by the local JSON store.
2. **Maintenance items** - define per-vehicle services with their mileage, recurrence, and reminder intervals.
3. **Log a completed service** - mark a tracked item done and reset its clocks.
4. **Log an ad-hoc repair** - record work that was never a tracked item.
5. **Service history** - per-vehicle record of everything logged, grouped by year and newest first, with date, mileage, cost, and notes.
6. **Due and overdue status** - compute and show what is due, including the mileage check triggered by a new odometer reading.
7. **Reminders and notifications** - reach the user when the app is closed, repeating until the service is logged.
8. **Cost totals** - what each vehicle has cost: all time, per calendar year, and an average per year, shown on the service history screen.
9. **Export and import** - move the whole history to and from a JSON file.
10. **Play Store readiness** - icon, signed release build, data safety declaration, privacy policy.

Post-MVP, listed so the direction is visible but explicitly outside v1:

11. **Receipt and vehicle photos** - images on log entries and vehicles.
12. **Cloud backup** - paid upgrade pairing photo storage with off-device backup.

## Data model

One JSON file in app-private storage holds everything. Nothing leaves the device
unless the user exports it.

### File root

- `schemaVersion` (int) - present from the first release so the shape can change without stranding installs
- `vehicles` (list of Vehicle)
- `maintenanceItems` (list of MaintenanceItem)
- `serviceLogEntries` (list of ServiceLogEntry)

> Locked: every write goes to a temp file and is renamed over the original. A
> partial overwrite would destroy the user's entire history, which is the app's
> only copy until they export.

### Interval

A value object used by both time fields on a maintenance item.

- `value` (int)
- `unit` (enum: DAYS, WEEKS, MONTHS, YEARS)

### Vehicle

- `id` (string)
- `year` (int)
- `make` (string)
- `model` (string)
- `engine` (string)
- `recordedMileage` (int, optional) - last recorded mileage; entered by hand or
  raised by a higher logged `odometer`. Absent on vehicles saved before it
  existed, which fall back to their highest logged `odometer`

Owns many MaintenanceItem and many ServiceLogEntry.

### MaintenanceItem

- `id` (string)
- `vehicleId` (string) - owning Vehicle
- `name` (string) - "Oil change"
- `mileageInterval` (int, optional) - miles between services
- `recurrence` (Interval, optional) - how often it is due; display and status only
- `reminder` (Interval, required) - when to notify, counted from last done
- `lastDoneDate` (date, optional) - seeded at creation or set by a completion log
- `lastDoneMileage` (int, optional) - baseline for the mileage check
- `lastNotifiedAt` (timestamp, optional) - drives the two-week repeat cadence

> Locked: `reminder` is required and independent of `recurrence`. It counts
> forward from `lastDoneDate`, read as "remind me 5 months from last done." This
> is what lets a mileage-only item (tire rotation, no sensible time interval)
> exist without inventing a fake due date.

### ServiceLogEntry

- `id` (string)
- `vehicleId` (string) - owning Vehicle
- `maintenanceItemId` (string, optional) - empty means an ad-hoc repair
- `description` (string)
- `date` (date)
- `odometer` (int) - miles; also raises the vehicle's `recordedMileage` when higher
- `cost` (money) - store as integer minor units, never a float, so totals do not drift
- `notes` (string, optional)

> Locked: one entry type covers both cases. A completion and an ad-hoc repair
> differ only by whether `maintenanceItemId` is set, so history, cost totals, and
> the mileage check all read a single list.

### Derived, never stored

- Next reminder = `lastDoneDate` + `reminder`
- Due date = `lastDoneDate` + `recurrence`, when recurrence is set
- Mileage due = `lastDoneMileage` + `mileageInterval`, when both are set
- Cost totals = sums over `serviceLogEntries`
- Average per year = all-time cost ÷ months between the first and last logged entry × 12, shown only once entries span at least 30 days

### Behavioral rules

- **Recurrence never notifies.** It produces the due date and the due/overdue
  status shown on screen. Nothing more.
- **Notification cadence.** One notification at the reminder time, then every two
  weeks from that first reminder until the service is logged. Logging resets the
  clock and cancels repeats.
- **Mileage is opportunistic.** The app cannot read an odometer. A reading is
  captured when work is logged, when a current mileage is entered on the vehicle
  form, or through Update on the vehicle detail screen. Every maintenance item
  for that vehicle is then checked, and anything overdue by mileage surfaces
  immediately. A reading below one recorded before saves only after the user
  accepts a warning; logged service never lowers it. No background mileage
  tracking, no projection.
- **Seeding.** Creating an item asks for last done date and mileage. Skipping is
  allowed: the clock then starts at creation and the mileage check stays inactive
  until the first logged completion.

## Tech stack

- **Kotlin + Jetpack Compose (Material 3)** - all UI, no XML layouts
- **Single `:app` module** - minSdk 29, target/compile SDK 37
- **Gradle Kotlin DSL + version catalog** - `gradle/libs.versions.toml` is the only place versions live
- **JSON file in app-private storage** - the store and the export format are the same thing, which makes backup nearly free
- **Repository interface over persistence** - the seam where a real database would go if the data outgrows a file
- **kotlinx.serialization** - reading and writing the store
- **Daily background check** - evaluates what is due and posts notifications

> Reminders land months out, too far for a reliable per-item alarm, and exact
> alarms are a restricted permission on modern Android. A daily evaluation
> survives reboots and updates, needs no special permission, and loses no useful
> precision.

## Monetization

Not in v1. Ships free with no ads, no accounts, no tracking.

v2 intent: a paid upgrade combining receipt/vehicle photos with cloud backup.
Photos are both the feature people want and the reason local-only storage stops
being enough, so the feature and the reason to charge arrive together.

## UI/UX

Plain and utilitarian: standard form inputs, lists, and tables. A reference tool
used a few times a year, so clarity beats visual ambition. Light and dark mode
are both required, driven by Material 3 theming.

Screens:

- **Vehicle list** - home screen; each row shows the vehicle and its last recorded mileage
- **Vehicle detail** - maintenance items as a table with due status, under the last recorded mileage and its Update action
- **Add/edit vehicle** - form, including an optional current mileage
- **Add/edit maintenance item** - form, including optional last-done seeding
- **Log service** - completion or ad-hoc repair; captures date, odometer, cost
- **Service history** - per-vehicle log grouped by year, combined with its cost totals
- **Export/import** - file out, file in

> The overdue view must stand alone without notifications. On Android 13+ the
> user can refuse notification permission, and the app still has to be useful to
> them when opened.

## Deployment

Google Play Store. No server, no backend, no env vars, no off-device jobs.

- Signed release build produced by Gradle
- App icon and store listing assets
- Data safety declaration: no data collected, nothing leaves the device
- A short privacy policy
- Notification runtime permission requested where the reason is obvious, not
  cold on first launch

## Non-goals

Out of scope for v1, recorded so they are not built by accident:

- Accounts, login, any user identity
- Cloud sync or a backend
- Receipt and vehicle photos (v2)
- Multi-user access, sharing, fleet management
- Automatic odometer capture, mileage projection, OBD integration
- Predefined schedules by year/make/model
- Shop or parts lookup, price comparison, recall data
- Ads or any monetization

## Open questions

> Resolve these in the plans, then re-run `/overview`.

- Should a maintenance item with no recurrence show any status beyond its next
  reminder date?
- Should the daily check batch multiple due items into one notification per
  vehicle, or post one per item?
- Assumed and correctable: ad-hoc repairs carry the same fields as a completion
  minus the item link; cost totals are all-time, per calendar year, and an
  average per year; distance is miles with no unit setting in v1.
