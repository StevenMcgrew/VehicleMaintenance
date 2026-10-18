# Help menu item

**Type:** Fix
**Status:** verified
**Branch:** fix/help-menu-item

Change 4 of 6 from `blueprint/context/tester-feedback-plan.md`.

## The problem

Play testers suggested a help section so users can answer common questions
inside the app. The app is published for people who install it cold, and a few
behaviors are not obvious from the screens alone: the difference between "Due
every" and "Remind me every", why mileage only updates when entered, and how
reminders repeat.

## The fix

- New `help/HelpScreen.kt`, built like `PrivacyScreen`: a back arrow, a title,
  and a scrolling column of sections, each a heading (marked as a heading for
  TalkBack) and a body paragraph. All text is static string resources, so it
  works offline with nothing to load or fail. `@Preview` in light and dark.
- `Routes.HELP` in `VehicleMaintenanceApp`, wired like Privacy.
- **Help** menu item after Rate app in `VehicleListMenu`, brand blue. It needs
  navigation, so `onOpenHelp` is threaded through `VehicleListMenu` and
  `VehicleListScreen` the same way `onOpenPrivacy` is. The menu then reads, in
  order: Theme, Backup data, Privacy, Share app, Rate app, Help.
- Help content, using the app's real button and field labels (the bold part
  of each entry is the section heading):

  1. **Getting started** - Tap Add vehicle on the Vehicles screen. Then tap a
     vehicle to open its maintenance list, and tap Add item for each service
     you want to track, such as an oil change or tire rotation.
  2. **Due every and Remind me every** - Due every is how often the service
     should be done, in miles, time, or both. It sets the status you see on
     screen. Remind me every is when you get a notification, counted from the
     date it was last done. A service tracked only by miles still needs a
     reminder time, so you are nudged to check it.
  3. **Date and mileage last done** - Enter these when you add an item if you
     know them. If you skip them, the clock starts today and the mileage check
     starts the first time you log the service.
  4. **OK, Due, and Overdue** - Due means the reminder time has arrived.
     Overdue means the due date has passed or the vehicle has reached the
     mileage it was due at. Miles left uses the vehicle's last recorded
     mileage.
  5. **Keeping mileage up to date** - The app cannot read your odometer. It
     learns the mileage when you log a service or repair, enter it on the
     vehicle form, or tap Update on the vehicle's screen. Every item is checked
     against the new reading right away.
  6. **Logging work** - Tap a service in the maintenance list and choose Log
     item as completed. This records the date, odometer, cost, and notes, and
     restarts that service's clock. For one-off work that is not a tracked
     item, use Log additional repairs.
  7. **Reminders** - The app checks once a day, even when it is closed. When a
     reminder comes due you get a notification, repeated every two weeks until
     you log the service. If notifications are off, the status on the vehicle
     screen still shows what is due.
  8. **Service history and costs** - Service history lists everything logged
     for a vehicle by year, with totals for all time, each year, and an average
     per year. The PDF button saves, prints, or shares it, which is handy when
     you sell the vehicle.
  9. **Backing up your data** - Your records live only on this phone. Use
     Backup data to export them to a file, and import that file on a new phone.
     Importing replaces everything currently in the app.

- No new permission or dependency. The privacy policy is unchanged.

Must not break: the other five menu items and their order, Privacy and Backup
navigation, the reminder deep link into vehicle detail.

## Build steps

- [x] 1. **Help screen and menu item.** `HelpScreen` with the strings above,
   the `help` route, `onOpenHelp` through the menu and list screen, and the
   menu item. Add a `HelpScreenTest` device test (temp-file store, like
   `PrivacyScreenTest`) that opens Help from the menu, finds a section heading,
   and returns with Back, plus a menu-order test that Help follows Rate app.
   Done when: tapping the three dots, then Help, opens the Help screen with all
   nine sections readable in light and dark and at the largest font, and Back
   returns to the vehicle list.

## Verify

- `./gradlew testDebugUnitTest lintDebug` pass with no new lint warnings.
- Menu order is Theme, Backup data, Privacy, Share app, Rate app, Help; Help is
  brand blue.
- Help opens, scrolls through all nine sections, and Back returns to the list.
- TalkBack announces each section title as a heading.
- Help is still reachable from the menu when the vehicle list fails to load.
