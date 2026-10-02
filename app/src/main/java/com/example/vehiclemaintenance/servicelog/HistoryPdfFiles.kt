package com.example.vehiclemaintenance.servicelog

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import androidx.core.content.FileProvider
import com.example.vehiclemaintenance.R
import com.example.vehiclemaintenance.vehicles.Vehicle
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.time.LocalDate

const val PDF_MIME_TYPE = "application/pdf"

/** Writes service history PDFs off the main thread: to a file the user picked, or for sharing. */
class HistoryPdfFiles(
    context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val context = context.applicationContext

    /** Returns false when the picked document could not be written. */
    suspend fun writeTo(
        uri: Uri,
        vehicle: Vehicle,
        history: ServiceHistory,
        generatedOn: LocalDate,
    ): Boolean = withContext(ioDispatcher) {
        val report = reportOf(vehicle, history, generatedOn)
        try {
            // "wt" truncates, so overwriting a longer file cannot leave its tail behind.
            val stream = context.contentResolver.openOutputStream(uri, "wt") ?: return@withContext false
            stream.use { writeHistoryPdf(report, HistoryPdfLabels(context), it) }
            true
        } catch (e: IOException) {
            false
        } catch (e: SecurityException) {
            false
        } catch (e: IllegalArgumentException) {
            false
        }
    }

    /**
     * Writes the PDF to the folder the file provider shares, clearing earlier PDFs first so they do
     * not pile up. Returns null when it could not be written.
     */
    suspend fun writeForSharing(
        vehicle: Vehicle,
        history: ServiceHistory,
        generatedOn: LocalDate,
    ): File? = withContext(ioDispatcher) {
        val report = reportOf(vehicle, history, generatedOn)
        try {
            val folder = File(context.cacheDir, SHARE_FOLDER)
            folder.listFiles()?.forEach { it.delete() }
            if (!folder.isDirectory && !folder.mkdirs()) return@withContext null
            val file = File(folder, historyPdfFileName(vehicle, generatedOn))
            file.outputStream().use { writeHistoryPdf(report, HistoryPdfLabels(context), it) }
            file
        } catch (e: IOException) {
            null
        }
    }

    private fun reportOf(vehicle: Vehicle, history: ServiceHistory, generatedOn: LocalDate): HistoryReport {
        val vehicleName = context.getString(
            R.string.vehicle_summary_with_engine,
            vehicle.year,
            vehicle.make,
            vehicle.model,
            vehicle.engine,
        ).trim()
        return historyReportOf(vehicleName, history, generatedOn)
    }
}

/** Matches `res/xml/file_paths.xml`, the only folder the file provider exposes. */
private const val SHARE_FOLDER = "reports"

/** Opens the share sheet for a PDF from [HistoryPdfFiles.writeForSharing]. */
fun shareHistoryPdf(context: Context, file: File) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val send = Intent(Intent.ACTION_SEND).apply {
        type = PDF_MIME_TYPE
        putExtra(Intent.EXTRA_STREAM, uri)
        // The chooser grants read access through the clip, so the picked app can open the file.
        clipData = ClipData.newRawUri(file.name, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(send, null))
}

/** Opens the print dialog for a PDF from [HistoryPdfFiles.writeForSharing], on Letter paper. */
fun printHistoryPdf(context: Context, file: File) {
    val printManager = context.getSystemService(PrintManager::class.java) ?: return
    val attributes = PrintAttributes.Builder()
        .setMediaSize(PrintAttributes.MediaSize.NA_LETTER)
        .build()
    printManager.print(file.nameWithoutExtension, PdfFilePrintAdapter(file), attributes)
}

/** Hands an already drawn PDF to the print framework, which lays nothing out itself. */
private class PdfFilePrintAdapter(private val file: File) : PrintDocumentAdapter() {

    override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes,
        cancellationSignal: CancellationSignal?,
        callback: LayoutResultCallback,
        extras: Bundle?,
    ) {
        if (cancellationSignal?.isCanceled == true) {
            callback.onLayoutCancelled()
            return
        }
        val info = PrintDocumentInfo.Builder(file.name)
            .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
            .build()
        callback.onLayoutFinished(info, newAttributes != oldAttributes)
    }

    override fun onWrite(
        pages: Array<out PageRange>,
        destination: ParcelFileDescriptor,
        cancellationSignal: CancellationSignal?,
        callback: WriteResultCallback,
    ) {
        try {
            file.inputStream().use { input ->
                FileOutputStream(destination.fileDescriptor).use { input.copyTo(it) }
            }
            callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
        } catch (e: IOException) {
            callback.onWriteFailed(e.message)
        }
    }
}
