# Project Plan

> One of the two planning docs you provide. Use as much detail as the project
> needs, including rationale, constraints, examples, edge cases, and explicit
> exclusions that should guide later feature work. Draft it directly, develop it
> through any AI conversation, or optionally run `/discovery` for a guided deep
> planning session. The content is always yours to direct. When it is filled in,
> run `/overview` to generate the project overview from this plus `build-plan.md`.

## 1. Problem - What problem are we solving?

Vehicle maintenance gets missed because nothing reliably reminds you it is due.
Service records live on paper in a glovebox, in a shop's system, or nowhere at
all. The cost is real: skipped services shorten vehicle life, and a vehicle with
no documented history is worth less at resale.

This app is a watchdog first and a logbook second. It tells the owner what is
coming due before it is late, and it keeps a permanent record of what was done,
when, at what mileage, and what it cost.

The record compounds. A three year history is worth far more than a three week
one, which is why export exists in v1 rather than being deferred.

## 2. Users - Who is this for?

A single vehicle owner tracking their own vehicles. Typically two or three,
though the app places no limit.

They are not fleet managers or mechanics. They are people who want to keep their
vehicles running and have no system for it beyond memory. The app is being
published to the Play Store, so it must work for someone who installs it with no
explanation and no onboarding call.

The owner is the only user. There is no sharing, no multi-user access, and no
account.

## 3. Features - What does the MVP need?

- Add, edit, and delete vehicles
- Define maintenance items per vehicle with mileage, recurrence, and reminder intervals
- Log a completed maintenance item with date, odometer, and cost
- Log an ad-hoc repair that is not a tracked maintenance item
- View service history per vehicle
- See due and overdue status for every maintenance item
- Receive notifications when a service is due, repeating until it is logged
- See cost totals per vehicle, all time and per year
- Export and import all data as a JSON file

## 4. Data - What are we storing?

All data is stored locally on the device in a single JSON file. Nothing leaves
the device unless the user explicitly exports it.

**Vehicle**

- id, year, make, model, engine

**Maintenance item** (belongs to a vehicle)

- id, vehicle id, name
- mileage interval (optional)
- recurrence interval (optional): number plus days, weeks, months, or years
- reminder interval (required): number plus days, weeks, months, or years
- last done date and last done mileage, both optional
- last notified timestamp, used to drive the repeat cadence

**Service log entry** (belongs to a vehicle)

- id, vehicle id
- maintenance item id, or empty for an ad-hoc repair
- description, date, odometer, cost, optional notes

**File root**

- schema version, vehicles, maintenance items, service log entries

### Derived values

Nothing below is stored. All of it is computed from the fields above.

- Next reminder = last done date plus reminder interval
- Due date = last done date plus recurrence interval, when recurrence is set
- Mileage due = last done mileage plus mileage interval, when both are set
- Cost totals = sums over service log entries

### Rules that matter

**Reminder timing.** The reminder interval counts forward from the last
completion, read as "remind me 5 months from last done." It is required and does
not depend on recurrence being set.

**Recurrence is display only.** It produces the due date and the due or overdue
status shown in the app. It never triggers a notification.

**Notification cadence.** One notification at the reminder time, then every two
weeks from that first reminder until the service is logged. Logging the service
resets the clock and cancels the repeats.

**Mileage is opportunistic.** The app cannot read an odometer, so mileage is
captured only when the user logs work. At that moment the new reading is checked
against every maintenance item for that vehicle, and anything overdue by mileage
is surfaced immediately. There is no background mileage tracking and no
projection of future mileage.

**Seeding a new item.** Creating a maintenance item asks for last done date and
mileage so the clocks start correctly. It can be skipped, in which case the
clock starts at creation and the mileage check stays inactive until the first
logged completion.

**File safety.** Writes go to a temporary file and are renamed over the original,
so an interrupted write cannot destroy the user's history. Every file carries a
schema version so the data shape can change without stranding existing installs.

## 5. Tech - What stack are we using?

- Native Android, Kotlin, Jetpack Compose with Material 3
- Single `:app` module, minSdk 29, targetSdk and compileSdk 37
- Gradle Kotlin DSL with the version catalog at `gradle/libs.versions.toml`
- Persistence: a single JSON file in app-private storage, read and written
  through a repository interface so the storage choice can change later
- Serialization: kotlinx.serialization
- Background work: a daily check that evaluates which items are due and posts
  notifications

Reminders land months into the future, which is too far out for a reliable
per-item scheduled alarm, and exact alarms are a restricted permission on modern
Android. A daily background evaluation survives reboots and app updates, needs no
special permission, and loses no useful precision because "due this week" does
not require a to-the-minute alarm.

JSON is a deliberate fit for this scale. A few vehicles and a few hundred records
is a file measured in kilobytes, and the storage format doubles as the export
format, which makes backup nearly free. If the data outgrows it, the repository
boundary is where a real database would go.

## 6. Monetize - How will this make money?

Nothing in v1. The app ships free with no ads, no accounts, and no tracking.

The intended v2 model is a paid upgrade combining receipt and vehicle photos with
cloud backup. Photos are the feature people want and are also what makes local
storage insufficient, so the feature and the reason to charge for it arrive
together.

## 7. UI/UX - How should this look and feel?

Plain and utilitarian. Standard form inputs, lists, and tables. This is a
reference tool used a few times a year, not something to linger in, so clarity
beats visual ambition.

- Vehicle list as the home screen
- Vehicle detail showing its maintenance items as a table with due status
- Service history as a list or table per vehicle
- Standard forms for adding and editing vehicles, items, and log entries

Light and dark mode are both required, driven by Material 3 theming.

The overdue view must stand on its own without notifications. On Android 13 and
later the user can refuse notification permission outright, and the app still has
to be useful to them when they open it.

## 8. Deployment - Where and how will this ship?

Google Play Store.

- Signed release build produced by Gradle
- App icon and store listing assets
- Data safety declaration: no data collected, nothing leaves the device
- A short privacy policy
- Notification runtime permission requested at a point where the reason is
  obvious, not on first launch with no context

No server, no backend, no environment variables, no scheduled jobs off device.

## 9. Non-goals

Explicitly out of scope for v1, recorded so they do not get built by accident:

- Accounts, login, or any user identity
- Cloud sync or a backend of any kind
- Receipt and vehicle photos, deferred to v2
- Multi-user access, sharing, or fleet management
- Automatic odometer capture, mileage projection, or OBD integration
- Predefined maintenance schedules by year, make, and model
- Shop or parts lookup, price comparison, or recall data
- Ads or any monetization

## 10. Assumptions and open questions

Working assumptions, confirmed at review or corrected later:

- Vehicles have no nickname. A vehicle is identified by its year, make, and
  model
- An ad-hoc repair captures description, date, odometer, and cost, the same
  shape as a completion log without a linked maintenance item
- Cost totals are all time and per calendar year, per vehicle
- Distance is in miles, with no unit setting in v1

Open TODOs:

- Whether a maintenance item with no recurrence should show any status other than
  its next reminder date
- Whether the daily check should batch multiple due items into one notification
  per vehicle or post one per item
