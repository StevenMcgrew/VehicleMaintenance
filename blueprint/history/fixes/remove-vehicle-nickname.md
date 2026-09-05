# Current Feature

**Remove the vehicle nickname field**

**Type:** Fix

**Status:** verified

**Branch:** `fix/remove-vehicle-nickname`

## Provenance

This spec was written after the work was built, not before. The change was
requested directly in chat and implemented there, which the workflow allows, but
that left `/complete` with no record to archive. This file is that record, and
the Status reflects the real state: built, and verified by the commands under
Verify below. The normal order is `/fix` first, then `/implement`.

## The problem

Two separate things, landed on one branch because the second was found while
verifying the first.

**1. Vehicles should not have nicknames.** The owner does not want the field. It
was carried through the whole vertical slice built in feature 1: the `Vehicle`
model and JSON contract, `VehicleDraft`, `VehicleFormFields` and its validation,
the form screen's first text field, and the list row's two-branch display logic
that used the nickname as the primary line when present.

**2. The "Add vehicle" FAB had no accessible label.** Feature 1 step 6 wrote it
as `ExtendedFloatingActionButton(onClick, text = { ... }, icon = {})`. That
Material 3 overload wraps its label in `Row(Modifier.clearAndSetSemantics {})`
and documents that the content description comes from the *icon*. With an empty
icon lambda there was no label source at all, so the on-device semantics tree
showed the FAB as `Role = Button`, `MergeDescendants = true`, with no `Text` and
no `ContentDescription`. TalkBack would announce an unlabeled button.

This contradicts the accessibility requirement in the feature 1 spec. It was
invisible to `assembleDebug`, `lintDebug`, and all unit tests, and surfaced only
when `connectedDebugAndroidTest` first ran on a real device.

## The fix

**Nickname.** Remove the field from `Vehicle`, `VehicleDraft`,
`VehicleFormFields`, and `VehicleFormValidator`. Drop the form's nickname text
field and the `vehicle_nickname` string. On the list, the primary line becomes
`year make model` unconditionally and the supporting line is always the engine,
which deletes `secondaryLabel` and the nickname branch in `primaryLabel`.

**FAB.** Switch to the content-slot overload,
`ExtendedFloatingActionButton(onClick) { Text(...) }`, which Material 3
documents as the overload "for FABs without an icon". The label becomes an
ordinary merged descendant, so it reaches both TalkBack and the semantics tree.

**Must not break.**

- Stored files that still contain `"nickname"` must keep loading.
  `ignoreUnknownKeys = true` already covers this: the key decodes without
  throwing and is dropped on the next write. No migration, and `schemaVersion`
  stays `1`.
- The rest of the JSON root contract, the atomic write, the delete cascade, and
  the load-failure-blocks-writes rule are untouched.
- The archived feature 1 spec is history and is not edited.

## Build steps

- [x] **1. Remove nickname from the model, repository, and validation.** Update
  `Vehicle`, `VehicleDraft`, `VehicleFormFields`, `VehicleFormValidator`, and
  `VehicleFormViewModel`, and update the unit tests that used the field.
  - **Done when:** `./gradlew testDebugUnitTest` passes, including a new test
    proving an older file carrying `"nickname"` decodes and is rewritten
    without it.

- [x] **2. Remove nickname from the UI.** Drop the form field, the
  `vehicle_nickname` string, and the list's nickname display branch.
  - **Done when:** `./gradlew assembleDebug` and `./gradlew lintDebug` succeed
    with no new warnings and no `nickname` references left in `app/src/main`.

- [x] **3. Give the FAB an accessible label.** Switch to the content-slot
  ExtendedFAB overload.
  - **Done when:** `./gradlew connectedDebugAndroidTest` passes on a device,
    including the empty-state test that locates the FAB by its label.

- [x] **4. Update the planning docs.** The two user-owned plans and the
  generated overview still described the field.
  - **Done when:** `project-plan.md`, `build-plan.md` item 1, and
    `project-overview.md` no longer describe a nickname, and the overview's
    `blueprint:source-hash` is recomputed under the checkbox-normalized hash
    contract.

## Verify

Ran and passing:

| Command | Result |
| --- | --- |
| `./gradlew assembleDebug testDebugUnitTest lintDebug` | exit 0, 29 unit tests, 0 failures |
| `./gradlew connectedDebugAndroidTest` | exit 0, 3 tests, 0 failures, on `T790W` (Android 11, API 30) |

Manual path: open the app, tap **Add vehicle**, and confirm the form shows four
fields (Year, Make, Model, Engine) with no nickname. Save one and confirm the
list row reads `year make model` with the engine beneath it. With TalkBack on,
the add button announces as "Add vehicle" rather than an unlabeled button.

## Notes for the AI

- `explicitNulls = false` stays in the shared `Json` config. It has no optional
  field to act on right now, but it is part of the documented file contract that
  features 2 through 5 will rely on.
- The overview edit was a targeted two-line change rather than a regeneration,
  to avoid rewriting a reviewed document. Run `/overview` if a full regenerate
  is wanted.
