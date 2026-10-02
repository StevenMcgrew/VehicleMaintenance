package com.example.vehiclemaintenance.servicelog

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import androidx.core.graphics.withClip
import androidx.core.graphics.withTranslation
import com.example.vehiclemaintenance.R
import java.io.OutputStream

/** The PDF's wording, read from string resources so it follows the app's language. */
class HistoryPdfLabels(context: Context) {
    private val resources = context.resources

    val title: String = resources.getString(R.string.service_history)
    val allTime: String = resources.getString(R.string.cost_all_time)
    val averagePerYear: String = resources.getString(R.string.cost_average_per_year)
    val noCosts: String = resources.getString(R.string.cost_totals_none)
    val notSet: String = resources.getString(R.string.value_not_set)
    val dateColumn: String = resources.getString(R.string.pdf_column_date)
    val odometerColumn: String = resources.getString(R.string.pdf_column_odometer)
    val serviceColumn: String = resources.getString(R.string.pdf_column_service)
    val costColumn: String = resources.getString(R.string.pdf_column_cost)

    fun generated(date: String): String = resources.getString(R.string.pdf_generated, date)
    fun year(year: Int): String = resources.getString(R.string.cost_year, year)
    fun yearContinued(year: Int): String = resources.getString(R.string.pdf_year_continued, year)
    fun odometer(miles: String): String = resources.getString(R.string.pdf_odometer, miles)
    fun page(number: Int, count: Int): String = resources.getString(R.string.pdf_page, number, count)
}

/**
 * Draws [report] as a US Letter PDF and writes it to [out], returning the page count. Text is laid
 * out with [StaticLayout], so it is slow enough to keep off the main thread.
 */
fun writeHistoryPdf(report: HistoryReport, labels: HistoryPdfLabels, out: OutputStream): Int {
    val pages = HistoryPdfLayout(report, labels).pages()
    val document = PdfDocument()
    try {
        pages.forEachIndexed { index, operations ->
            val info = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, index + 1).create()
            val page = document.startPage(info)
            operations.forEach { it(page.canvas) }
            drawFooter(page.canvas, report.vehicleName, labels.page(index + 1, pages.size))
            document.finishPage(page)
        }
        document.writeTo(out)
    } finally {
        document.close()
    }
    return pages.size
}

private typealias DrawOperation = (Canvas) -> Unit

/**
 * Measures every block and assigns it to a page. Pages are built in full before anything is
 * drawn, so each footer can say how many pages there are.
 */
private class HistoryPdfLayout(
    private val report: HistoryReport,
    private val labels: HistoryPdfLabels,
) {
    private val pages = mutableListOf<MutableList<DrawOperation>>()
    private var y = MARGIN

    private val remaining get() = CONTENT_BOTTOM - y

    fun pages(): List<List<DrawOperation>> {
        newPage()
        header()
        summary()
        report.years.forEach { year(it) }
        return pages
    }

    private fun newPage() {
        pages += mutableListOf<DrawOperation>()
        y = MARGIN
    }

    /** Queues [draw] at the current position on the current page, then moves down by [height]. */
    private fun place(height: Float, draw: Canvas.(top: Float) -> Unit) {
        val top = y
        pages.last() += { canvas -> canvas.draw(top) }
        y += height
    }

    private fun header() {
        listOf(
            textLayout(labels.title, TITLE_PAINT, CONTENT_WIDTH),
            textLayout(report.vehicleName, VEHICLE_PAINT, CONTENT_WIDTH),
            textLayout(labels.generated(report.generatedOn), SMALL_GREY_PAINT, CONTENT_WIDTH),
        ).forEach { layout ->
            place(layout.height + HEADER_LINE_GAP) { top -> drawLayout(layout, MARGIN, top) }
        }
        y += SECTION_GAP
    }

    /** A vehicle with nothing costed says so rather than claiming it has been free to run. */
    private fun summary() {
        val allTime = report.allTime
        if (allTime == null) {
            amountRow(labels.allTime, labels.noCosts, SUMMARY_TOTAL_PAINT, BODY_GREY_PAINT)
        } else {
            amountRow(labels.allTime, allTime, SUMMARY_TOTAL_PAINT, SUMMARY_TOTAL_PAINT)
            val average = report.averagePerYear ?: labels.notSet
            amountRow(labels.averagePerYear, average, SUMMARY_PAINT, SUMMARY_PAINT)
        }
        place(RULE_GAP) { top -> drawRule(top + RULE_GAP / 2) }
        y += SECTION_GAP
    }

    private fun amountRow(label: String, value: String, labelPaint: TextPaint, valuePaint: TextPaint) {
        val labelLayout = textLayout(label, labelPaint, AMOUNT_LABEL_WIDTH)
        val valueLayout = textLayout(
            value, valuePaint, CONTENT_WIDTH - AMOUNT_LABEL_WIDTH, Layout.Alignment.ALIGN_OPPOSITE,
        )
        val height = maxOf(labelLayout.height, valueLayout.height) + 2 * AMOUNT_ROW_PADDING
        place(height) { top ->
            drawLayout(labelLayout, MARGIN, top + AMOUNT_ROW_PADDING)
            drawLayout(valueLayout, MARGIN + AMOUNT_LABEL_WIDTH, top + AMOUNT_ROW_PADDING)
        }
    }

    private fun year(year: ReportYear) {
        val heading = YearHeading(year, continued = false)
        val rows = year.rows.map { MeasuredRow(it) }
        // A heading never sits alone at the bottom of a page: it needs its first row, or at least
        // the first lines of one too tall for any page, beneath it.
        val firstRowNeeds = minOf(rows.first().height, MIN_SPLIT_HEIGHT)
        if (heading.height + firstRowNeeds > remaining) newPage()
        placeHeading(heading)

        val continuedHeading = YearHeading(year, continued = true)
        val pageCapacity = CONTENT_BOTTOM - MARGIN - continuedHeading.height
        rows.forEach { row ->
            when {
                row.height <= remaining -> placeRow(row)
                row.height <= pageCapacity -> {
                    newPage()
                    placeHeading(continuedHeading)
                    placeRow(row)
                }
                else -> placeSplitRow(row, continuedHeading)
            }
        }
        y += SECTION_GAP
    }

    private fun placeHeading(heading: YearHeading) {
        place(heading.height) { top -> heading.draw(this, top) }
    }

    private fun placeRow(row: MeasuredRow) {
        place(row.height) { top -> row.draw(this, top, 0, row.service.lineCount - 1) }
    }

    /**
     * A row taller than a whole page is the one block allowed to break: its Service text continues
     * line by line onto following pages, each under a continued heading.
     */
    private fun placeSplitRow(row: MeasuredRow, continuedHeading: YearHeading) {
        val service = row.service
        var startLine = 0
        while (startLine < service.lineCount) {
            val available = remaining - 2 * ROW_PADDING
            val firstLineHeight = service.getLineBottom(startLine) - service.getLineTop(startLine)
            if (firstLineHeight > available) {
                newPage()
                placeHeading(continuedHeading)
                continue
            }
            val sliceTop = service.getLineTop(startLine)
            var endLine = startLine
            while (endLine + 1 < service.lineCount &&
                service.getLineBottom(endLine + 1) - sliceTop <= available
            ) {
                endLine++
            }
            val sliceHeight = (service.getLineBottom(endLine) - sliceTop).toFloat()
            val isFirstSlice = startLine == 0
            val contentHeight = if (isFirstSlice) maxOf(sliceHeight, row.otherColumnsHeight) else sliceHeight
            val sliceStart = startLine
            val sliceEnd = endLine
            place(contentHeight + 2 * ROW_PADDING) { top -> row.draw(this, top, sliceStart, sliceEnd) }
            startLine = endLine + 1
            if (startLine < service.lineCount) {
                newPage()
                placeHeading(continuedHeading)
            }
        }
    }

    /** The year and its total, the column names, and a rule beneath them. */
    private inner class YearHeading(year: ReportYear, continued: Boolean) {
        private val title = textLayout(
            if (continued) labels.yearContinued(year.year) else labels.year(year.year),
            YEAR_PAINT,
            AMOUNT_LABEL_WIDTH,
        )
        private val total = textLayout(
            year.total ?: labels.notSet,
            YEAR_PAINT,
            CONTENT_WIDTH - AMOUNT_LABEL_WIDTH,
            Layout.Alignment.ALIGN_OPPOSITE,
        )
        private val columns = listOf(
            DATE_COLUMN to labels.dateColumn,
            ODOMETER_COLUMN to labels.odometerColumn,
            SERVICE_COLUMN to labels.serviceColumn,
            COST_COLUMN to labels.costColumn,
        ).map { (column, name) -> column to column.layout(name, COLUMN_HEADER_PAINT) }

        private val titleHeight = maxOf(title.height, total.height).toFloat()
        private val columnsHeight = columns.maxOf { it.second.height }.toFloat()

        val height = titleHeight + HEADING_GAP + columnsHeight + RULE_GAP

        fun draw(canvas: Canvas, top: Float) {
            canvas.drawLayout(title, MARGIN, top)
            canvas.drawLayout(total, MARGIN + AMOUNT_LABEL_WIDTH, top)
            val columnsTop = top + titleHeight + HEADING_GAP
            columns.forEach { (column, layout) -> canvas.drawLayout(layout, MARGIN + column.x, columnsTop) }
            canvas.drawRule(columnsTop + columnsHeight + RULE_GAP / 2)
        }
    }

    /** One logged entry: Date, Odometer, Service (description, then notes in grey), and Cost. */
    private inner class MeasuredRow(row: ReportRow) {
        private val date = DATE_COLUMN.layout(row.date, BODY_PAINT)
        private val odometer = ODOMETER_COLUMN.layout(labels.odometer(row.odometer), BODY_PAINT)
        private val cost = row.cost?.let { COST_COLUMN.layout(it, BODY_PAINT) }

        val service: StaticLayout = SERVICE_COLUMN.layout(serviceText(row), BODY_PAINT)

        val otherColumnsHeight = maxOf(date.height, odometer.height, cost?.height ?: 0).toFloat()

        val height = maxOf(service.height.toFloat(), otherColumnsHeight) + 2 * ROW_PADDING

        /** Draws the Service lines [startLine]..[endLine]; the other columns go with the first slice. */
        fun draw(canvas: Canvas, top: Float, startLine: Int, endLine: Int) {
            val contentTop = top + ROW_PADDING
            if (startLine == 0) {
                canvas.drawLayout(date, MARGIN + DATE_COLUMN.x, contentTop)
                canvas.drawLayout(odometer, MARGIN + ODOMETER_COLUMN.x, contentTop)
                cost?.let { canvas.drawLayout(it, MARGIN + COST_COLUMN.x, contentTop) }
            }
            val sliceTop = service.getLineTop(startLine).toFloat()
            val sliceBottom = service.getLineBottom(endLine).toFloat()
            canvas.withTranslation(MARGIN + SERVICE_COLUMN.x, contentTop - sliceTop) {
                withClip(0f, sliceTop, SERVICE_COLUMN.width, sliceBottom) { service.draw(this) }
            }
        }
    }
}

private fun serviceText(row: ReportRow): CharSequence {
    val notes = row.notes ?: return row.description
    return SpannableStringBuilder(row.description).apply {
        append('\n')
        val start = length
        append(notes)
        setSpan(ForegroundColorSpan(GREY), start, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        setSpan(AbsoluteSizeSpan(NOTES_TEXT_SIZE), start, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }
}

private fun drawFooter(canvas: Canvas, vehicleName: String, pageLabel: String) {
    val pageWidth = FOOTER_PAINT.measureText(pageLabel)
    val nameWidth = CONTENT_WIDTH - pageWidth - FOOTER_GAP
    val name = TextUtils.ellipsize(vehicleName, FOOTER_PAINT, nameWidth, TextUtils.TruncateAt.END)
    canvas.drawText(name, 0, name.length, MARGIN, FOOTER_BASELINE, FOOTER_PAINT)
    canvas.drawText(pageLabel, MARGIN + CONTENT_WIDTH - pageWidth, FOOTER_BASELINE, FOOTER_PAINT)
}

/** Where a table column's text sits within the content block, inside its allotted width. */
private class TableColumn(
    val x: Float,
    val width: Float,
    val alignment: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL,
) {
    fun layout(text: CharSequence, paint: TextPaint) = textLayout(text, paint, width, alignment)
}

private fun textLayout(
    text: CharSequence,
    paint: TextPaint,
    width: Float,
    alignment: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL,
): StaticLayout = StaticLayout.Builder.obtain(text, 0, text.length, paint, width.toInt())
    .setAlignment(alignment)
    .setIncludePad(false)
    .build()

private fun Canvas.drawLayout(layout: StaticLayout, x: Float, top: Float) {
    withTranslation(x, top) { layout.draw(this) }
}

private fun Canvas.drawRule(y: Float) {
    drawLine(MARGIN, y, MARGIN + CONTENT_WIDTH, y, RULE_PAINT)
}

// US Letter in PDF points, 72 to the inch, with 0.75 inch margins.
private const val PAGE_WIDTH = 612
private const val PAGE_HEIGHT = 792
private const val MARGIN = 54f
private const val CONTENT_WIDTH = PAGE_WIDTH - 2 * MARGIN
private const val CONTENT_BOTTOM = PAGE_HEIGHT - MARGIN
private const val FOOTER_BASELINE = PAGE_HEIGHT - 32f
private const val FOOTER_GAP = 24f

// Columns of the 504 point block: Date 84, Odometer 80, Service 260, Cost 80. Text sits inside each
// allotment with room to spare, so a right aligned mileage never touches the Service text.
private val DATE_COLUMN = TableColumn(x = 0f, width = 76f)
private val ODOMETER_COLUMN = TableColumn(x = 84f, width = 68f, Layout.Alignment.ALIGN_OPPOSITE)
private val SERVICE_COLUMN = TableColumn(x = 176f, width = 236f)
private val COST_COLUMN = TableColumn(x = 424f, width = 80f, Layout.Alignment.ALIGN_OPPOSITE)

private const val AMOUNT_LABEL_WIDTH = 300f
private const val AMOUNT_ROW_PADDING = 4f
private const val ROW_PADDING = 6f
private const val HEADER_LINE_GAP = 4f
private const val HEADING_GAP = 8f
private const val RULE_GAP = 8f
private const val SECTION_GAP = 18f

/** About three lines of body text: the least of a too-tall row worth starting on a page. */
private const val MIN_SPLIT_HEIGHT = 40f

private const val BLACK = 0xFF000000.toInt()
private const val GREY = 0xFF5F6368.toInt()
private const val RULE_GREY = 0xFFBDBDBD.toInt()
private const val NOTES_TEXT_SIZE = 9

private fun textPaint(size: Float, bold: Boolean = false, color: Int = BLACK) =
    TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = size
        typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        this.color = color
    }

private val TITLE_PAINT = textPaint(20f, bold = true)
private val VEHICLE_PAINT = textPaint(14f)
private val SMALL_GREY_PAINT = textPaint(10f, color = GREY)
private val SUMMARY_TOTAL_PAINT = textPaint(12f, bold = true)
private val SUMMARY_PAINT = textPaint(11f)
private val YEAR_PAINT = textPaint(13f, bold = true)
private val COLUMN_HEADER_PAINT = textPaint(9f, bold = true, color = GREY)
private val BODY_PAINT = textPaint(10f)
private val BODY_GREY_PAINT = textPaint(11f, color = GREY)
private val FOOTER_PAINT = textPaint(8f, color = GREY)
private val RULE_PAINT = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    color = RULE_GREY
    strokeWidth = 0.5f
}
