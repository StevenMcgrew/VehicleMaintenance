# Keyboard covers form fields on small screens

**Type:** Fix
**Status:** verified
**Branch:** fix/keyboard-covers-form-fields-on-small-screens

## The problem

On a small phone, focusing **Mileage last done** on the add or edit item form
opens the on-screen keyboard over the field. The user can't see what they're
typing, and scrolling doesn't help.

Cause: the app targets SDK 37 and calls `enableEdgeToEdge()`. That makes
edge-to-edge mandatory, so `android:windowSoftInputMode="adjustResize"` no
longer shrinks the window when the keyboard opens. The app has to handle the
IME inset itself. The form's `Scaffold` uses the default `contentWindowInsets`,
which covers only the system bars, not the IME. The scrolling column
(`MaintenanceItemFormScreen.kt:255-259`) keeps its full height under the
keyboard. The last field sits in the covered area, and the column doesn't
think it needs to scroll, so the field never comes into view.

The service log form (`ServiceLogFormScreen.kt:176-180`) and vehicle form
(`VehicleFormScreen.kt:155-159`) use the same modifier chain. Their lower
fields (cost, notes, odometer) most likely have the same bug.

## The fix

On each form's scroll column, apply the IME inset between the Scaffold padding
and the scroll:

```kotlin
Modifier
    .fillMaxSize()
    .padding(innerPadding)
    .consumeWindowInsets(innerPadding)
    .imePadding()
    .verticalScroll(rememberScrollState())
    .padding(16.dp)
```

- `consumeWindowInsets(innerPadding)` keeps the navigation bar inset from being
  counted twice when the keyboard is open.
- `imePadding()` shrinks the scroll viewport above the keyboard.

A text field on its own scrolls only its cursor line into view. On the emulator,
that left the field's bottom outline and its error text under the keyboard. So
the shared `OutlineLabelTextField` and the vehicle form's `VehicleField` also
use a new `rememberKeepAboveKeyboard(error)` helper in `ui/FormRow.kt`. While a
field has focus, the helper scrolls the whole field into view, error text
included. It does this again as the keyboard slides in and when an error
appears.

Must not break:

- Layout with the keyboard closed. The bottom padding stays the same as today.
- Edge-to-edge drawing, the top app bar, and the snackbar.
- The two-column `FormRow` layout, and the date picker and unit dropdowns.
- Light and dark mode.

## Build steps

1. [x] **Item form.** Apply the change to the `ItemForm` modifier in
   `MaintenanceItemFormScreen.kt`.
   *Done when:* on a small-screen emulator, tapping Mileage last done scrolls
   the field above the keyboard, and the layout with the keyboard closed looks
   the same as before.
2. [x] **Sibling forms.** Apply the same change to the scroll columns in
   `ServiceLogFormScreen.kt` and `VehicleFormScreen.kt`.
   *Done when:* the bottom field on each form stays visible above the keyboard
   on the same emulator.
   (Drop this step if you want the fix limited to the item form.)
3. [x] **Keep the whole field in view.** This step was added during
   verification. Add `rememberKeepAboveKeyboard` and apply it to
   `OutlineLabelTextField` and `VehicleField`.
   *Done when:* a focused field's outline and error text both sit above the
   keyboard, including after Save shows a new error.

## Verify

- `./gradlew assembleDebug lintDebug testDebugUnitTest` passes.
- On a small-screen emulator (for example 360×640 dp, or a Pixel 4a at its
  largest display size), open a vehicle, then **Add item**. Tap Mileage last
  done. The field and its label stay visible above the keyboard, and you can
  type into it while seeing it.
- Do the same on **Edit item** with a validation error showing under the field.
  The error text should also stay visible.
- With the keyboard closed, the form's bottom spacing hasn't changed, and
  nothing is hidden behind the navigation bar (check gesture nav and 3-button
  nav).
- Step 2: tap the last field on the service log form (Notes) and on the vehicle
  form. Each one stays above the keyboard.
- Check light and dark mode.
