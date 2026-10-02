package com.example.vehiclemaintenance.servicelog

import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.LocalDate
import java.util.Locale

/**
 * Writes PDFs to temp files in the app's cache, never the store. Pass the instrumentation argument
 * `keepPdf=true` to keep the long history and the long note as `cache/history-pdf-*.pdf` for a
 * visual check.
 */
@RunWith(AndroidJUnit4::class)
class HistoryPdfTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val labels = HistoryPdfLabels(context)
    private val written = mutableListOf<File>()
    private var nextId = 0

    @After
    fun deleteWrittenFiles() {
        written.forEach { it.delete() }
    }

    private fun entry(date: LocalDate, cost: Int?, notes: String? = null) = ServiceLogEntry(
        id = "e-${nextId++}",
        vehicleId = "v-1",
        description = if (nextId % 3 == 0) "Replaced the front brake pads and resurfaced the rotors" else "Oil change",
        date = date,
        odometer = 30_000 + nextId * 1_500,
        cost = cost,
        notes = notes,
    )

    private fun reportOf(entries: List<ServiceLogEntry>) = historyReportOf(
        vehicleName = "2020 Ford Ranger 3.0L",
        history = serviceHistoryOf(entries),
        generatedOn = LocalDate.of(2026, 10, 2),
        locale = Locale.US,
    )

    /** Writes [report], then checks every page is US Letter and returns the page count. */
    private fun writeAndCheck(report: HistoryReport, file: File = tempFile()): Int {
        val pageCount = file.outputStream().use { writeHistoryPdf(report, labels, it) }
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
            PdfRenderer(descriptor).use { renderer ->
                assertEquals(pageCount, renderer.pageCount)
                repeat(renderer.pageCount) { index ->
                    renderer.openPage(index).use { page ->
                        assertEquals(612, page.width)
                        assertEquals(792, page.height)
                    }
                }
            }
        }
        return pageCount
    }

    private fun keptOrTempFile(name: String): File {
        val keep = InstrumentationRegistry.getArguments().getString("keepPdf") == "true"
        return if (keep) File(context.cacheDir, name) else tempFile()
    }

    private fun tempFile() = File(context.cacheDir, "history-pdf-test-${System.nanoTime()}.pdf")
        .also { written += it }

    @Test
    fun aSingleEntryFitsOnOnePage() {
        val pages = writeAndCheck(reportOf(listOf(entry(LocalDate.of(2026, 9, 5), cost = 6_499))))

        assertEquals(1, pages)
    }

    @Test
    fun anUncostedHistoryStillWrites() {
        val pages = writeAndCheck(reportOf(listOf(entry(LocalDate.of(2026, 9, 5), cost = null))))

        assertEquals(1, pages)
    }

    @Test
    fun aLongHistoryRunsOntoSeveralLetterPages() {
        val entries = (2023..2026).flatMap { year ->
            (1..25).map { day ->
                entry(
                    date = LocalDate.of(year, 1 + day % 12, day),
                    cost = if (day % 4 == 0) null else 4_000 + day * 1_250,
                    notes = if (day % 5 == 0) "Shop said the belts and hoses look fine; check again next visit." else null,
                )
            }
        }
        val pages = writeAndCheck(reportOf(entries), keptOrTempFile("history-pdf-sample.pdf"))

        assertTrue("expected several pages, got $pages", pages >= 4)
    }

    @Test
    fun aNoteTallerThanAPageContinuesOntoTheNext() {
        val longNote = (1..150).joinToString(" ") { "Line $it of a very long note about the repair." }

        val pages = writeAndCheck(
            reportOf(listOf(entry(LocalDate.of(2026, 9, 5), cost = 6_499, notes = longNote))),
            keptOrTempFile("history-pdf-long-note.pdf"),
        )

        assertTrue("expected the note to continue, got $pages page(s)", pages >= 2)
    }
}
