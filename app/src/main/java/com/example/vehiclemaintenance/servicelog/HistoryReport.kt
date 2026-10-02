package com.example.vehiclemaintenance.servicelog

import com.example.vehiclemaintenance.maintenance.formatMediumDate
import com.example.vehiclemaintenance.maintenance.formatMileage
import com.example.vehiclemaintenance.vehicles.Vehicle
import java.time.LocalDate
import java.util.Locale

/**
 * The values a service history PDF prints, already formatted for [Locale]. Labels such as
 * "All time" stay out of it, so this holds only what varies by vehicle and the PDF supplies the
 * localized wording.
 */
data class HistoryReport(
    /** "2020 Ford Ranger 3.0L", formatted by the caller from string resources. */
    val vehicleName: String,
    val generatedOn: String,
    /** Null when nothing is costed, which the PDF words as "No costs recorded yet". */
    val allTime: String?,
    /** Null when there is no average to show, which the PDF prints as "-". */
    val averagePerYear: String?,
    /** Newest year first. */
    val years: List<ReportYear>,
)

data class ReportYear(
    val year: Int,
    /** Null when nothing logged that year carries a cost. */
    val total: String?,
    /** Newest first. */
    val rows: List<ReportRow>,
)

data class ReportRow(
    val date: String,
    /** Grouped digits only; the PDF adds the unit. */
    val odometer: String,
    val description: String,
    /** Null when blank, so the PDF never draws an empty notes line. */
    val notes: String?,
    /** Null when the cost was never recorded. */
    val cost: String?,
)

fun historyReportOf(
    vehicleName: String,
    history: ServiceHistory,
    generatedOn: LocalDate,
    locale: Locale = Locale.getDefault(),
): HistoryReport = HistoryReport(
    vehicleName = vehicleName,
    generatedOn = formatMediumDate(generatedOn, locale),
    allTime = history.allTime?.let { formatCost(it, locale) },
    averagePerYear = history.averagePerYear?.let { formatCost(it, locale) },
    years = history.years.map { year ->
        ReportYear(
            year = year.year,
            total = year.total?.let { formatCost(it, locale) },
            rows = year.entries.map { entry ->
                ReportRow(
                    date = formatMediumDate(entry.date, locale),
                    odometer = formatMileage(entry.odometer, locale),
                    description = entry.description,
                    notes = entry.notes?.takeIf { it.isNotBlank() },
                    cost = entry.cost?.let { formatCost(it, locale) },
                )
            },
        )
    },
)

/**
 * The name a saved or shared PDF gets: "service-history-2020-ford-ranger-2026-10-02.pdf". Every
 * run of characters other than ASCII letters and digits becomes one hyphen, so the name is safe
 * on any file system and in a share target's upload.
 */
fun historyPdfFileName(vehicle: Vehicle, generatedOn: LocalDate): String {
    val slug = "service history ${vehicle.year} ${vehicle.make} ${vehicle.model} $generatedOn"
        .lowercase(Locale.ROOT)
        .replace(NON_ALPHANUMERIC, "-")
        .trim('-')
    return "$slug.pdf"
}

private val NON_ALPHANUMERIC = Regex("[^a-z0-9]+")
