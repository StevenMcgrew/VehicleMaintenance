# Build Plan

> One of the two planning docs you provide. Write it directly, develop it through
> any AI conversation, or optionally run `/discovery`. Keep the items high-level
> even when `project-plan.md` is detailed; later `/feature` specs hold the depth
> for each build item.

The features that make up this project, high level and in rough build order, one
line each, no detail (that comes per feature). Rough is fine at first, but before
`/overview` runs this file should be shaped into a checkbox list the build loop
can track.

Keep it as a checklist. Run `/feature` with no number to spec the **next
unchecked** item, or `/feature 3` / `/feature "login"` to pick a specific one.
Completed features get checked off here, so the build plan doubles as your
progress tracker. A big item gets split into sub-items (4a, 4b, etc.) when you
spec it.

## Continuing after the initial build

This is a living roadmap, not a plan that freezes when the first release is
done. Keep completed items checked, then append new unchecked features as the
project grows. Optional milestone headings such as `## MVP` and `## Post-MVP`
keep a longer plan readable without changing how `/feature` finds the next
unchecked item.

Do not renumber completed features because their archived specs refer back to
those numbers. Continue with the next unused number. If a new feature materially
changes the product direction, users, data, stack, monetization, UI/UX, or
deployment, update the relevant part of `project-plan.md` too. Then re-run
`/overview` before spec'ing the feature.

You can edit this file directly or ask the AI to start a new feature by name. If
`/feature "team workspaces"` does not match an existing item, it will propose the
new build-plan line and any necessary project-plan changes, wait for approval,
refresh the overview, and then write the feature spec.

Scaffolding the app (create-next-app, etc.) and prototyping the look are
pre-build steps, not features (see the README), so don't list them here. Start
with your first real slice of functionality.

A common order that works well: build the core UI with placeholder data first,
then wire up data, auth, and integrations. Add deployment readiness only when
the app is worth shipping or a provider config change is part of the work. Adapt
it to your project.

## MVP

- [x] 1. **Vehicle list and details** - add, edit, and delete vehicles with
  year, make, model, and engine, persisted to the local JSON file with atomic
  writes and a schema version
- [x] 2. **Maintenance items** - add, edit, and delete maintenance items on a
  vehicle with mileage interval, recurrence, and reminder interval, including
  optional last-done seeding
- [ ] 3. **Log a completed service** - mark a maintenance item done with date,
  odometer, and cost, resetting its clocks
- [ ] 4. **Log an ad-hoc repair** - record work that is not a tracked maintenance
  item, with description, date, odometer, and cost
- [ ] 5. **Service history** - per-vehicle list of everything logged, newest first
- [ ] 6. **Due and overdue status** - compute and display next reminder, due date,
  and overdue state for every item, including the mileage check that runs when a
  new odometer reading is captured
- [ ] 7. **Reminders and notifications** - daily background check that notifies
  when an item is due, repeating every two weeks until it is logged, with runtime
  permission handling
- [ ] 8. **Cost totals** - per-vehicle spend, all time and by year
- [ ] 9. **Export and import** - write all data to a JSON file the user chooses
  and restore from one, with schema version handling
- [ ] 10. **Play Store readiness** - app icon, signed release build, data safety
  declaration, and privacy policy

## Post-MVP

- [ ] 11. **Receipt and vehicle photos** - attach images to log entries and
  vehicles
- [ ] 12. **Cloud backup** - paid upgrade pairing photo storage with off-device
  backup
