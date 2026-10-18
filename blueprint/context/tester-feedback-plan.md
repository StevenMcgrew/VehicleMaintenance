# Tester Feedback Plan

Source: `blueprint/context/vehicle_feedback.pdf` (Testers Community report,
received 2026-10-18). The testers found no crashes, bugs, or broken features.
Every item is an enhancement. Each code change runs as its own `/fix` (or
`/feature` where noted), one at a time: `/fix` -> `/implement` -> `/complete`.

## Order

| # | Change | Report item | Kind | Run as |
| --- | --- | --- | --- | --- |
| 1 | Three-dot menu on the vehicle list with a System / Light / Dark theme switch; Backup data and Privacy move into it | 5) Dark mode | Code | Done (`fix/vehicle-list-menu-with-a-theme-choice`) |
| 2 | Share app menu item, with a pre-written message and the Play link | 3) Share app | Code | Done (`fix/share-app-menu-item`) |
| 3 | Rate app menu item, opening the Play listing | 4) In-app rating | Code | `/fix` |
| 4 | Help menu item opening a Help screen: short how-to for items, reminders, mileage, backup | Additional: help | Code | `/fix` |
| 5 | Rewrite the store listing for search keywords | 1) ASO | Store text | `/fix` (edits `play-store/listing.md` only) |
| 6 | Annotated feature screenshots | 2) Screenshots | Store assets | `/fix` (after 1 to 4, so captures show the new menu) |

Menu order on the vehicle list (top right): Theme, Backup data, Privacy, Share
app, Rate app, Help. Change 1 builds the menu, so it comes first; 2 to 4 each
add one item.

## Notes per item

1. **Dark mode.** The app already has a full dark theme and follows the phone's
   system setting (`screenshot-5-history-dark.png` shows it). What is missing is
   an in-app toggle, so testers on a light-mode phone never saw it. The launch
   window theme is also light-only, so a dark-mode phone flashes white on start.
   The switch is a segmented button row inside the dropdown, with radio rows as
   the fallback if it clips at large font sizes.
2. **Share app.** A plain `ACTION_SEND` chooser with a string resource message
   and the Play URL. No dependency, no internet permission, nothing collected.
3. **Rate app.** Open `market://details?id=...` with a web URL fallback. The
   Play In-App Review API is not used: it adds a Play Core dependency for a
   prompt Google may silently suppress, and an unprompted pop-up after logging a
   service would interrupt the one task the app exists for. Revisit only if the
   simple link proves too low-traffic.
4. **Help.** A Help screen of static string resources, same pattern as
   `PrivacyScreen`.
5. **ASO.** Work in the report's phrases ("car maintenance", "service
   reminder", "oil change tracker", "auto repair log", "mileage log") without
   stuffing. Recount characters against Play's limits.
6. **Screenshots.** Recapture where the vehicle list changed, then overlay a
   short caption on each (reminders, due status, history, cost totals, PDF
   export, dark theme). Keep 1080 x 1920 and the fictional sample data.

## Not planned as fixes

| Report item | Decision | Why |
| --- | --- | --- |
| Onboarding walkthrough | Candidate `/feature` | Multi-screen flow with first-run state; too big for a fix. Add to the build plan if wanted |
| Data visualization (charts) | Candidate `/feature` | Needs a chart approach and design; cost totals already exist as numbers |
| Backup and restore | Already shipped | Export and import to a user-chosen file exists (Backup button). Cloud backup is the planned v2 paid upgrade, a v1 non-goal |
| Regular updates | Process, no code | Ship these fixes as the first update |
