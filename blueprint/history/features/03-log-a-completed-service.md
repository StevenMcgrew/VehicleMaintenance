# Current Feature

**Feature 3: Log a completed service**

**Branch:** `feature/log-a-completed-service`

**Status:** verified

## Goal

Mark a maintenance item done. Capture the date, the odometer reading, and
optionally the cost and notes, write it to the store as the app's first real
`ServiceLogEntry`, and reset that item's clocks in the same atomic write.

This is the feature that turns the store's placeholder `serviceLogEntries`
field into a typed contract. Features 4, 5, and 8 all read that list, so the
shape defined here is the one they inherit.

## In scope

- A typed `ServiceLogEntry` replacing the raw `JsonObject` placeholder in
  `MaintenanceStore`, plus the existing tests that assert the raw shape.
- A `ServiceLogRepository` whose `add` appends the entry and resets the linked
  item's `lastDoneDate`, `lastDoneMileage`, and `lastNotifiedAt` in one
  `MaintenanceStoreHolder.update` transaction.
- A Log service form screen and ViewModel: description, date, odometer, cost,
  notes, with validation, loading, item-not-found, and save-failure states.
- A route `vehicles/{vehicleId}/items/{itemId}/log`.
- A bottom sheet on the vehicle detail row tap offering **Log a completed
  service**, **Edit item**, and **Delete item**.

## Out of scope

- **Ad-hoc repairs** (feature 4). `add` already accepts a null
  `maintenanceItemId` so feature 4 needs no new repository method, but nothing
  in this feature creates an unlinked entry.
- **Service history** (feature 5). Nothing lists entries back to the user. The
  repository intentionally exposes no read flow yet.
- **Due and overdue status** (feature 6), including the "check every item for
  this vehicle when a new odometer reading arrives" rule. This feature captures
  the reading; feature 6 computes and surfaces what it means.
- **Notifications** (feature 7). Clearing `lastNotifiedAt` on a completion is
  the locked "logging cancels repeats" rule and is done here, but nothing
  schedules or posts anything.
- **Cost totals** (feature 8). No cost is displayed or summed, so this feature
  needs no currency formatter.

## Build loop

`workflow.stepReview` is `feature` and `checkpointCommits` is `disabled`, so
`/implement` runs all five steps and presents one review packet at the end. No
per-step approval pauses and no checkpoint commits. `/complete` makes the single
feature commit.

`verification.logicTests` is `when-configured` and JUnit is wired up, so every
logic step ships passing focused tests. `verification.uiEvidence` is
`when-available` and a physical device is attached, so the UI steps carry
screenshots and an on-device walkthrough.

## Build steps

1. [x] **Type the service log entry.** Add
   `servicelog/ServiceLogEntry.kt` and change
   `MaintenanceStore.serviceLogEntries` to `List<ServiceLogEntry>`, dropping the
   placeholder comment. Retype `JsonMaintenanceItemRepository.delete`'s unlink
   to `it.copy(maintenanceItemId = null)` and delete the now unused
   `JsonObject` helper and `MAINTENANCE_ITEM_ID` constant. Update the five test
   files that build raw `JsonObject` entries so their fixtures are complete
   `ServiceLogEntry` values.
   *Done when* `./gradlew testDebugUnitTest` passes, a round-trip test proves an
   entry with a null `cost`, `notes`, and `maintenanceItemId` omits those keys
   on write and reads back equal, and the unlink test still proves a deleted
   item leaves its entries in place with the link cleared.

2. [x] **Add the service log repository.** Add
   `servicelog/ServiceLogRepository.kt` with `ServiceLogDraft`, the interface,
   and `JsonServiceLogRepository`. `add` runs one `holder.update`: append the
   entry with a generated id, and when `maintenanceItemId` is set, replace that
   item with `lastDoneDate = draft.date`, `lastDoneMileage = draft.odometer`,
   `lastNotifiedAt = null`. Reject with `IllegalArgumentException` when the
   draft names an item id that is not in the store. Inject `newId` and expose it
   from `AppContainer` next to the other two repositories.
   *Done when* `./gradlew testDebugUnitTest` passes with tests proving: the
   entry and the reset item land together in one write; a rejected draft writes
   nothing; a draft with a null `maintenanceItemId` appends without touching any
   item; and `lastNotifiedAt` is cleared.

3. [x] **Add the form validator.** Add
   `servicelog/ServiceLogFormValidation.kt` mirroring
   `MaintenanceItemFormValidation.kt`: `ServiceLogFormFields`, `LogFieldError`,
   `ServiceLogFormErrors`, `ServiceLogFormValidation`, and
   `ServiceLogFormValidator.validate(fields, vehicleId, itemId, today)`. Rules
   are in **Data / contracts** below.
   *Done when* `./gradlew testDebugUnitTest` passes with tests covering: an
   empty description; a future date; a missing, negative, and non-numeric
   odometer; an empty cost producing a null; `45`, `45.5`, and `45.50` producing
   `4500`, `4550`, and `4550`; and `45.555`, `-1`, and `abc` rejected.

4. [x] **Build the Log service screen.** Add
   `servicelog/ServiceLogFormScreen.kt` and `ServiceLogFormViewModel.kt`, and
   register `Routes.LOG_SERVICE` in `VehicleMaintenanceApp.kt`. Follow
   `MaintenanceItemFormScreen.kt` exactly: the `TextFieldState` overload with
   `TextFieldLabelPosition.Attached(alwaysMinimize = true)`, placeholders, a
   single column of full-width fields, `supportingText` errors with
   `semantics { error(...) }`, a `Cancel` / title / `Save` top bar, the loading
   spinner, the not-found column, and a snackbar on save failure. Description
   prefills from the item's name and the date defaults to today. Add both
   previews, filled and errored.
   *Done when* `./gradlew assembleDebug lintDebug` is clean with no new
   warnings, and on device the form opens prefilled, refuses an empty odometer,
   and saves.

5. [x] **Open the actions sheet from a row.** Replace the vehicle detail row's
   direct `onEditItem` click with a `ModalBottomSheet` titled with the item name
   offering **Log a completed service**, **Edit item**, and **Delete item**.
   Delete reuses the existing `DeleteItemDialog` and
   `MaintenanceItemRepository.delete` through `VehicleDetailViewModel`, showing
   a snackbar on failure. The edit screen keeps its own Delete action.
   *Done when* `./gradlew assembleDebug lintDebug testDebugUnitTest` passes and,
   on device, a row tap opens the sheet, each of the three actions does what it
   says, and logging a service returns to the detail screen with that item's
   Miles and Time columns unchanged and its stored last-done values updated.

## Files / areas

| Path | Change |
| --- | --- |
| `servicelog/ServiceLogEntry.kt` | New. The typed entry. |
| `servicelog/ServiceLogRepository.kt` | New. Draft, interface, JSON implementation. |
| `servicelog/ServiceLogFormValidation.kt` | New. Fields, errors, validator. |
| `servicelog/ServiceLogFormViewModel.kt` | New. |
| `servicelog/ServiceLogFormScreen.kt` | New. |
| `data/MaintenanceStore.kt` | `serviceLogEntries` becomes `List<ServiceLogEntry>`. |
| `maintenance/MaintenanceItemRepository.kt` | Typed unlink on delete. |
| `maintenance/VehicleDetailScreen.kt` | Row tap opens the actions sheet. |
| `maintenance/VehicleDetailViewModel.kt` | Delete action and its failure state. |
| `VehicleMaintenanceApp.kt` | `LOG_SERVICE` route and builder. |
| `VehicleMaintenanceApplication.kt` | `serviceLogRepository` on `AppContainer`. |
| `res/values/strings.xml` | Screen title, five field labels, placeholders, sheet actions, errors. |
| `app/src/test/.../data`, `.../maintenance`, `.../vehicles` | Five existing files build raw `JsonObject` entries and must move to typed fixtures. |
| `app/src/test/.../servicelog` | New validator, repository, and serialization tests. |

## Data / contracts

`ServiceLogEntry`, serialized by the existing `storeJson`
(`explicitNulls = false`, so null optional fields are absent from the file):

| Field | Type | Rule |
| --- | --- | --- |
| `id` | `String` | Generated by the repository, `UUID.randomUUID()`, same as the other two repositories |
| `vehicleId` | `String` | Required, owning vehicle |
| `maintenanceItemId` | `String?` | Null means an ad-hoc repair. Set for every entry this feature creates |
| `description` | `String` | Required, trimmed. Prefilled from the item name and editable, so an entry still reads correctly after its item is deleted and the link is cleared |
| `date` | `LocalDate` | Required, `LocalDateSerializer`, ISO-8601 |
| `odometer` | `Int` | Required, `>= 0`. The only way mileage enters the app |
| `cost` | `Int?` | Optional, minor units (cents), `>= 0`. Never a float |
| `notes` | `String?` | Optional, trimmed, null when blank |

Validation rules for `ServiceLogFormValidator`:

- **Description** - `REQUIRED` when blank after trimming.
- **Date** - defaults to today, `DATE_IN_FUTURE` when after today. Matches the
  existing `lastDoneDate` rule in `MaintenanceItemFormValidator`.
- **Odometer** - `REQUIRED` when blank, `NOT_A_NON_NEGATIVE_NUMBER` when it does
  not parse to an `Int` of 0 or more.
- **Cost** - blank produces null. Otherwise parsed with `BigDecimal`, never a
  `Double`, and `NOT_A_VALID_AMOUNT` when it is negative, unparseable, or has
  more than two decimal places. Reject rather than round, so no entered money is
  silently altered. Convert with `movePointRight(2).intValueExact()`.
- **Notes** - trimmed, null when blank.

Clock reset, applied only when `maintenanceItemId` is set:

- `lastDoneDate = draft.date`
- `lastDoneMileage = draft.odometer`
- `lastNotifiedAt = null` - the locked "logging resets the clock and cancels
  repeats" rule. Feature 7 owns setting it; this feature only clears it.

Atomicity: the appended entry and the reset item go into a single
`MaintenanceStoreHolder.update` transform, so a failed write leaves neither. A
two-call version could log the service and lose the reset, which would keep
telling the user an item is overdue after they logged it.

## Testing

`./gradlew testDebugUnitTest` is the gate. New JVM tests under
`app/src/test/java/com/example/vehiclemaintenance/servicelog/`:

- `ServiceLogEntrySerializationTest` - round-trip, absent optional keys, and a
  stored entry decoding into the typed model.
- `JsonServiceLogRepositoryTest` - follows `JsonMaintenanceItemRepositoryTest`,
  driving a real `MaintenanceStoreHolder` over a temp file.
- `ServiceLogFormValidatorTest` - follows `MaintenanceItemFormValidatorTest`,
  including the cost parsing table above.

No new instrumented tests are required. The project has only scaffold plus two
screen tests, and `verification.uiEvidence` is `when-available`, so the UI gate
is on-device evidence: screenshots of the empty form, a validation error, and
the detail screen after a save, in both light and dark mode.

## Notes for the AI

- **Both open decisions are already answered.** The row tap opens a three-action
  bottom sheet, and `cost` is optional. Do not re-litigate either.
- **Typing `serviceLogEntries` is safe for existing installs.** No code has ever
  written an entry, so no user file contains one. The placeholder comment in
  `MaintenanceStore.kt` says this field stays raw "until features 3 and 4 land";
  this is that landing.
- **One consequence to accept deliberately.** Once typed, unknown fields inside
  an entry are dropped on rewrite rather than preserved. That is fine because
  this app owns the schema and `schemaVersion` covers real changes, but update
  the affected assertions on purpose rather than making a red test green.
- Reuse the form patterns from the fix archived at
  `blueprint/history/fixes/pin-item-form-labels-and-add-placeholders.md`: pinned
  labels, placeholders, and full-width fields in one column. A half-width field
  cannot hold a long label at 360dp.
- Money never touches a `Double`, in parsing, storage, or tests.
- Strings live in `res/values/strings.xml`, never inline in a composable.
- No em dashes in code, comments, or commit messages.
