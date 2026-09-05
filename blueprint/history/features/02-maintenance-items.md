# Current Feature

**Feature 2: Maintenance items**

**Branch:** `feature/maintenance-items`

**Status:** verified

## Goal

Let the owner define the services they track on a vehicle: name, optional
mileage interval, optional recurrence, a required reminder interval, and
optional last-done seeding. This also introduces the vehicle detail screen that
features 3 through 8 hang everything else on, and turns `maintenanceItems` from
an opaque `JsonObject` list into a typed model.

## In scope

- `MaintenanceItem` and the `Interval` value object as typed, serializable
  models, replacing the raw `JsonObject` list in `MaintenanceStore`.
- A `LocalDate` serializer so dates round trip as ISO-8601 strings.
- A single shared in-memory store cache so the vehicle repository and the new
  maintenance repository cannot overwrite each other's writes.
- `MaintenanceItemRepository`: observe per vehicle, add, update, delete.
- Vehicle detail screen as the new destination for a list row tap, showing the
  vehicle header and its maintenance items.
- Add/edit maintenance item form with validation, inline errors, and accessible
  labels.
- Delete a maintenance item with a confirmation dialog.
- Loading, empty, loaded, and load-failure states on the detail screen.
- Routing for detail, add item, and edit item, with correct system back.

## Out of scope

- **Due and overdue status, next reminder date, and the mileage check.** Feature
  6 owns every derived value. This feature stores the inputs and displays them
  literally; it computes nothing.
- **Service log entries.** Features 3 and 4 own them. `serviceLogEntries` stays
  a `List<JsonObject>` here, preserved across writes and handled by the delete
  rules below.
- **Cost totals.** Feature 8.
- **Notifications and `lastNotifiedAt`.** Feature 7 owns the field. This feature
  declares it so it round trips, and never writes it.
- Odometer capture, unit settings, and export/import.
- Rewriting the archived feature 1 spec or the `vehicle_summary` display format.

## Build loop

`workflow.stepReview` is `feature` and `workflow.checkpointCommits` is
`disabled`. Work through every step without stopping for per-step approval and
without checkpoint commits, then present one review packet covering all steps.
`/complete` creates the final feature commit and merges after approval.

Run after every step, from the project root:

```
./gradlew testDebugUnitTest
```

Run before the review packet:

```
./gradlew assembleDebug && ./gradlew testDebugUnitTest && ./gradlew lintDebug && ./gradlew connectedDebugAndroidTest
```

There is still no configured `Verify` command, so those four are the gate. A
device is attached and `connectedDebugAndroidTest` passed during the last fix,
so on-device evidence is expected this time rather than optional. If the device
is unavailable at review, say so plainly instead of claiming it ran. If it
reports `Dozing`, wake it first; a dozing device fails with "No compose
hierarchies found" and that is not a code failure.

## Build steps

- [x] **1. Single shared store cache.** Today `JsonVehicleRepository` owns its
  own `Mutex` and its own `cached: MaintenanceStore`. A second repository over
  the same file with its own cache would silently overwrite the first one's
  writes. Extract the cache into one holder in `data/` that owns the mutex, the
  cached store, and a `StateFlow` of it, exposing a read and a
  read-modify-write that both repositories go through. `JsonVehicleRepository`
  keeps its public interface and behavior.
  - Check: the load-failure rule must survive the refactor. A failed load still
    blocks every write and never overwrites the unreadable file.
  - **Done when:** `./gradlew testDebugUnitTest` passes with the feature 1
    repository tests unchanged and green, plus a new test proving two
    repositories built over the same holder both see a write made through the
    other, and that a write through one does not drop a change made through the
    other.

- [x] **2. Typed maintenance item model.** Add `maintenance/Interval.kt` and
  `maintenance/MaintenanceItem.kt`, plus a `LocalDate` serializer in `data/`.
  Change `MaintenanceStore.maintenanceItems` to `List<MaintenanceItem>` and
  update the vehicle delete cascade to filter typed items. `serviceLogEntries`
  stays `List<JsonObject>`.
  - **Done when:** `./gradlew testDebugUnitTest` passes with tests proving: a
    round trip preserves every field including both intervals and both last-done
    fields; an absent optional field is omitted from the JSON rather than
    written as null; `unit` encodes as its enum name; `lastDoneDate` encodes as
    `YYYY-MM-DD`; an unknown item key decodes without throwing; and a store
    holding items and raw log entries re-encodes with the log entries intact.

- [x] **3. Maintenance item repository.** Add
  `maintenance/MaintenanceItemRepository.kt` (interface plus JSON-backed
  implementation over the shared holder): items for a vehicle, add, update,
  delete. Ids come from an injected generator, production binding
  `UUID.randomUUID().toString()`.
  - **Done when:** `./gradlew testDebugUnitTest` passes with tests proving: add
    appends an item carrying the injected id and the owning `vehicleId`; update
    replaces fields while preserving id and list position; items for a vehicle
    exclude another vehicle's items; deleting an item removes it and leaves
    every service log entry in place with its `maintenanceItemId` cleared, per
    Data / contracts; deleting a vehicle still removes that vehicle's items;
    every mutation is persisted, so a fresh repository over the same file sees
    it.

- [x] **4. Validation and form view model.** Add
  `maintenance/MaintenanceItemFormValidation.kt` (a pure validator, mirroring
  `VehicleFormValidation.kt`) and `maintenance/MaintenanceItemFormViewModel.kt`
  built through `viewModelFactory` with `APPLICATION_KEY`.
  - **Done when:** `./gradlew testDebugUnitTest` passes with tests proving every
    rule in Data / contracts: blank name blocks save; a missing or non-positive
    reminder value blocks save; a partially filled recurrence blocks save while
    a fully empty one is accepted as absent; a non-positive mileage interval
    blocks save while an empty one is absent; a future last-done date is
    rejected; a negative last-done mileage is rejected; whitespace is trimmed;
    and a valid form produces the expected draft.

- [x] **5. Vehicle detail screen and routing.** Add
  `maintenance/VehicleDetailScreen.kt` and `VehicleDetailViewModel.kt`. Change
  the vehicle list row tap to open detail instead of the edit form, and move the
  edit-vehicle action onto the detail screen's top bar. Render loading, empty,
  loaded, and load-failure states, plus the delete-item confirmation dialog. All
  new text goes in `strings.xml`.
  - **Done when:** `./gradlew assembleDebug` and `./gradlew lintDebug` succeed
    with no new warnings, and the detail route is reachable from a list row.

- [x] **6. Add/edit maintenance item form screen.** Add
  `maintenance/MaintenanceItemFormScreen.kt`: name, mileage interval, recurrence
  value plus unit, reminder value plus unit, last-done date, last-done mileage,
  save and cancel, inline errors, and `@Preview`s for the detail and form
  screens.
  - **Done when:** `./gradlew assembleDebug` and `./gradlew lintDebug` succeed
    with no new warnings, and on the device: adding an item returns to detail
    showing it, force-stopping and reopening still shows it, editing changes it
    in place, deleting asks for confirmation and removes it, an empty form shows
    an error under name and reminder without navigating, and system back from
    the form returns to detail without saving.

- [x] **7. Compose UI test.** Add an instrumented test under
  `maintenance/` covering the detail screen empty state and the
  add-item-then-appears path against repositories backed by a temp file, in the
  same shape as `VehicleListScreenTest`.
  - **Done when:** `./gradlew connectedDebugAndroidTest` passes on the attached
    device with the new test included. Record the device and API level in the
    review packet.

## Files / areas

New, under `app/src/main/java/com/example/vehiclemaintenance/`:

- `maintenance/Interval.kt`
- `maintenance/MaintenanceItem.kt`
- `maintenance/MaintenanceItemRepository.kt`
- `maintenance/MaintenanceItemFormValidation.kt`
- `maintenance/MaintenanceItemFormViewModel.kt`
- `maintenance/MaintenanceItemFormScreen.kt`
- `maintenance/VehicleDetailScreen.kt`
- `maintenance/VehicleDetailViewModel.kt`
- `data/LocalDateSerializer.kt`
- `data/MaintenanceStoreHolder.kt`
- Unit tests mirroring these under `app/src/test/`, one Compose test under
  `app/src/androidTest/`

Changed:

- `data/MaintenanceStore.kt` - typed `maintenanceItems`
- `vehicles/VehicleRepository.kt` - delegate to the shared holder, typed cascade
- `vehicles/VehicleListScreen.kt` - row tap opens detail
- `VehicleMaintenanceApp.kt` - detail, add-item, and edit-item routes
- `VehicleMaintenanceApplication.kt` - holder plus the second repository
- `app/src/main/res/values/strings.xml` - all new user-visible text

Untouched: `JsonFileStore.kt`, `ui/theme/`, the archived feature 1 spec.

## Data / contracts

**Interval.** Serialized as an object, both keys always present:

```json
{ "value": 5, "unit": "MONTHS" }
```

`value` is an int of 1 or more. `unit` is one of `DAYS`, `WEEKS`, `MONTHS`,
`YEARS`, encoded as the enum name.

**MaintenanceItem.**

| field | type | rule |
| --- | --- | --- |
| `id` | string | `UUID.randomUUID().toString()`, never rewritten by an edit |
| `vehicleId` | string | owning vehicle, never rewritten by an edit |
| `name` | string | non-blank, trimmed |
| `mileageInterval` | int, optional | 1 or more when present; omitted when absent |
| `recurrence` | Interval, optional | display and status only; omitted when absent |
| `reminder` | Interval, required | always written |
| `lastDoneDate` | string, optional | ISO-8601 `YYYY-MM-DD`. Backfilled with the creation date when seeding is skipped, so a new item always has one |
| `lastDoneMileage` | int, optional | 0 or more; omitted when absent |
| `lastNotifiedAt` | string, optional | ISO-8601 instant. Feature 7 owns it; this feature never writes it |

**Dates.** `java.time.LocalDate` is available at `minSdk 29`. Serialize as
ISO-8601 `YYYY-MM-DD` through one shared serializer so every later feature uses
the same encoding.

**Ordering.** Items display in stored order. A new item is appended and an edit
keeps its position, matching vehicles.

**Deleting a maintenance item.** Remove the item in one write, and keep the
historical record of any service that was actually logged against it. Entries
that pointed at the item stay, with their `maintenanceItemId` cleared so nothing
dangles; each entry keeps its own `description`, so what was done survives. When
the item has no entries there is nothing to preserve and the delete is total.
That list is always empty until feature 3, but the rule is fixed now.

**Deleting a vehicle** still removes its items and its log entries, unchanged
from feature 1.

**Form validation.** Trim `name` before validating and storing.

- `name` required, non-blank.
- `reminder` required: value must parse as an integer of 1 or more, and a unit
  must be selected.
- `recurrence` optional and all-or-nothing: leaving both the value and the unit
  empty stores it as absent; supplying one without the other is an error on the
  missing part; a supplied value must be 1 or more.
- `mileageInterval` optional: empty stores absent, otherwise an integer of 1 or
  more.
- `lastDoneDate` optional: must be a real date and must not be in the future.
- `lastDoneMileage` optional: empty stores absent, otherwise an integer of 0 or
  more.
- Last-done date and mileage are independent. Either may be supplied alone.
- Save is blocked while any field is invalid and every invalid field shows its
  own message.

**Display.** Item name as the primary line. The secondary line lists only what
is set, in this order: mileage interval as `every N miles`, recurrence as
`every N UNIT`, reminder as `remind N UNIT after`. No due date, no status, no
computation. All user-entered text renders as plain Compose `Text` with
`maxLines` and ellipsis overflow, never as a format-string argument.

**Load failure.** Unchanged from feature 1: the detail screen shows the error
state and all writes stay blocked for the session.

## Testing

`./gradlew testDebugUnitTest` is the required gate and every step names what its
tests must prove. Keep the models, serializer, holder, repository, and validator
free of `android.*` imports so they test on the JVM with JUnit 4 and
`TemporaryFolder`. The Compose test in step 7 needs the attached device.

## Notes for the AI

- Follow `blueprint/context/coding-standards.md`: stateless composables with a
  leading `modifier`, state from a view model, no repository access from a
  composable, no hardcoded colors or strings, no disk work on the main thread.
- Put the new code in `maintenance/`, matching the standards' feature-package
  rule and the existing `vehicles/` package.
- Reuse the feature 1 shapes rather than inventing new ones: `StoreResult`, the
  injected id generator, `viewModelFactory` with `APPLICATION_KEY`, and the
  validator-plus-view-model split in `VehicleFormValidation.kt`.
- For the FAB on the detail screen, use the content-slot
  `ExtendedFloatingActionButton(onClick) { Text(...) }` overload. The
  `text`/`icon` overload strips its label from the semantics tree and was fixed
  once already.
- Material 3 `DatePickerDialog` and `ExposedDropdownMenuBox` are both
  `@ExperimentalMaterial3Api`, the same opt-in the existing screens already use.
- Only the store layer touches the four root lists. UI and view models go
  through a repository interface.
- No em dashes in code, comments, or commit messages.

## Decisions

All three open questions were answered at review.

1. **No `createdAt` field.** Skipping the last-done seeding backfills
   `lastDoneDate` with the creation date instead, so the reminder clock has a
   baseline without adding a field to the export format. The known tradeoff,
   accepted: a newly created item is indistinguishable from one the user marked
   as done today, and `lastDoneMileage` stays absent so the mileage check still
   stays inactive until the first logged completion.

2. **Item delete keeps logged history.** Service log entries survive with their
   `maintenanceItemId` cleared. With no entries the delete removes everything.

3. **Edit vehicle moves to the detail screen.** A list row tap opens detail;
   delete stays on the list row.
