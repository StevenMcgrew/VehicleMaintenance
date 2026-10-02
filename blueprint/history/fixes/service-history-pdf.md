# Current Feature

## Service history PDF

**Type:** Fix
**Status:** verified
**Branch:** fix/service-history-pdf

### The problem

The service history screen (`servicelog/ServiceHistoryScreen.kt`) is the record
that makes a vehicle worth more at sale, but it only lives on the phone. The
only way out is the JSON backup, which is for restoring the app, not for a
buyer, a shop, or a printer. There is no document of the history that can be
saved, printed, or shared.

### The fix

Add a PDF of one vehicle's service history, laid out for US Letter
(8.5 x 11 in), with the same information as the screen. The app stays offline:
the PDF is drawn on the device with Android's built-in `PdfDocument`, and no
library or permission is added.

**Entry point.** A `PDF` text button in the service history top bar, using
`brandTextButtonColors()` like Save. It opens a menu (popup color
`LocalBrandColors.current.popupContainer`) with three actions:

| Action | What it does |
|--------|--------------|
| Save PDF | Opens the system file picker (`CreateDocument("application/pdf")`, the same pattern as the JSON backup) and writes the PDF where the user chooses |
| Print | Hands the PDF to Android's print framework (`PrintManager`), which also offers its own Save as PDF |
| Share PDF | Writes the PDF to a cache folder and opens the system share sheet through a `FileProvider` |

The button shows only when the history has at least one entry. While loading,
failed, vehicle missing, or empty, it is hidden.

**File name.** `service-history-<year>-<make>-<model>-<YYYY-MM-DD>.pdf`, lower
case, each run of characters other than letters and digits replaced with one
hyphen, for example `service-history-2020-ford-ranger-2026-10-02.pdf`. The
date is the day it was generated. Save offers this name and the user may change
it; Share and Print use it as is.

**Page layout.** Letter is 612 x 792 points. Margins are 0.75 in (54 pt) all
round, leaving a 504 pt wide text block. Black text on white with grey
secondary text and thin grey rules, so it prints cleanly; no app brand colors.

| Part | Contents |
|------|----------|
| Header (page 1 only) | `Service history` title; vehicle as `2020 Ford Ranger 3.0L` (the detail screen's `vehicle_summary_with_engine`); `Generated Oct 2, 2026` in grey |
| Cost summary (page 1 only) | `All time` and its total, then `Average per year` and its value or `-`. With nothing costed, a single `All time` row reading `No costs recorded yet`, as on screen. The screen's info button is left out. |
| Year section | Year heading with the year's total, or `-`, right aligned; a column header row; then one row per entry, newest first |
| Entry row | Columns Date, Odometer, Service, Cost (see below) |
| Footer (every page) | Vehicle name on the left, `Page 1 of 3` on the right, in small grey text |

Entry columns, widths in points of the 504 pt block:

| Column | Width | Contents |
|--------|-------|----------|
| Date | 84 | `formatMediumDate` (`Sep 5, 2026`) |
| Odometer | 80 | `formatMileage` plus ` mi`, right aligned |
| Service | 260 | The description, then the notes beneath it in grey when present |
| Cost | 80 | `formatCost`, right aligned, or blank when not recorded |

The Service column wraps instead of cutting off: unlike the screen's three line
limit, the PDF shows every description and note in full.

**Pagination.**

- Rows are never split across pages. A row taller than a whole page (a very
  long note) is the only exception: it continues onto the next page.
- A year heading never sits alone at the bottom of a page; it moves to the next
  page with at least its first entry.
- A year that continues onto a new page repeats its heading as
  `2026 (continued)` and its column header row.

**Code shape.**

- `servicelog/HistoryReport.kt`: pure Kotlin. Turns the vehicle, its
  `ServiceHistory`, and the generated date into the formatted lines the PDF
  shows, plus the file name. Unit tested.
- `servicelog/HistoryPdf.kt`: draws a report into a `PdfDocument` with
  `StaticLayout` text, measures rows, paginates, and writes to an
  `OutputStream`. Runs off the main thread.
- `servicelog/HistoryPdfFiles.kt`: writes the PDF to a picked document or the
  share folder off the main thread, and holds the print adapter and share
  intent.
- The screen and `ServiceHistoryViewModel` gain the three actions. A failure
  to write shows a snackbar, `The PDF could not be created.` Writing to the
  share folder catches only `IOException`. Writing to a picked document also
  catches `SecurityException` and `IllegalArgumentException`, which a storage
  provider throws when it refuses the write, matching the JSON backup's
  `BackupFiles.write`. Anything else reaches the normal crash path.
- Share writes to `cacheDir/reports/`, clearing older files there first, so
  shared PDFs do not pile up. The manifest gains a `FileProvider` limited to
  that folder (`res/xml/file_paths.xml`).

Must not break:

- The service history screen itself, its empty and error states, and cost
  totals.
- Offline use: no network, no new permission, no new dependency.
- The JSON backup and its file picker.
- Data on the emulator: nothing here writes to the app store.

### Build steps

1. [x] **Report model.** Add `HistoryReport.kt`: the header, summary, year
   sections, and entry rows as formatted text, and the file name rule. Unit
   tests cover a costed history, an uncosted one, an entry with notes, a
   missing cost, and the file name with punctuation and spaces in make and
   model.
   *Done when* `./gradlew testDebugUnitTest` passes with the new tests.
2. [x] **PDF drawing.** Add `HistoryPdf.kt` with the Letter page, layout,
   columns, footer, and pagination above. An instrumented test writes a long
   history to a temp file in `cacheDir`, then checks with `PdfRenderer` that
   every page is 612 x 792 and that the page count matches the pagination.
   *Done when* that instrumented test passes and a rendered page image shows
   the layout above.
3. [x] **Save, print, and share.** Add the `PDF` button and menu, the Save
   picker, the print job, the share sheet with its `FileProvider`, the
   snackbar on failure, and the new strings.
   *Done when* `./gradlew assembleDebug lintDebug testDebugUnitTest` passes and
   each action works on the emulator.

### Verify

- `./gradlew assembleDebug lintDebug testDebugUnitTest` passes.
- Back up the emulator's `files/vehicle-maintenance.json`, run the new
  instrumented test with
  `-Pandroid.injected.androidTest.leaveApksInstalledAfterRun=true`, and confirm
  the store is unchanged afterwards.
- On the emulator, open a vehicle with logged services, then Service history:
  - `PDF` shows in the top bar; on a vehicle with nothing logged it does not.
  - Save PDF opens the file picker with the suggested name; the saved file
    opens in a PDF viewer.
  - Print opens the print preview on Letter paper with every page.
  - Share PDF opens the share sheet with the PDF attached.
  - The PDF has the header, cost summary, every year and entry, full notes,
    and `Page N of M` footers.

Verified on the emulator on 2026-10-02:

- `HistoryReportTest`: 7 unit tests pass.
- `HistoryPdfTest`: 4 instrumented tests pass (one page, uncosted, a 100 entry
  history over 6 pages, and a note taller than a page continuing over 3).
  Rendered with `pdftoppm`: header, summary, year headings, table columns,
  wrapped notes, `2026 (continued)` headings, and `Page N of M` footers all
  match the layout; the long note continues line by line with no cut lines.
- `ServiceHistoryScreenTest`: 11 tests pass, including two new ones: the PDF
  button offers Save PDF, Print, and Share PDF, and is absent with nothing
  logged.
- Every instrumented run left the APKs installed, and the emulator store's
  SHA-256 matched before and after.
- On the user's data: the Ranger (nothing logged) shows no PDF button. On the
  Focus, Share PDF opened the share sheet with
  `service-history-2020-ford-focus-2026-10-02.pdf`; its one page matches the
  screen. Print opened the preview on Letter paper. Save PDF offered the same
  name in the picker and wrote a byte-identical file to Downloads, which passed
  `qpdf --check`; that test file was then deleted. Nothing was shared or
  printed.
- `./gradlew assembleDebug lintDebug testDebugUnitTest` passes; lint reports
  the same 16 existing issues and none from this change.
