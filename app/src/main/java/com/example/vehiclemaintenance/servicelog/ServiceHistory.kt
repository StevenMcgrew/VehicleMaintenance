package com.example.vehiclemaintenance.servicelog

import java.time.temporal.ChronoUnit

/** Below this span a single bill projected across a whole year would badly overstate the cost. */
const val MIN_AVERAGE_SPAN_DAYS = 30L

/** 365.25 days a year as a whole-number ratio, so the average stays in integer arithmetic. */
private const val DAYS_IN_FOUR_YEARS = 1461L

/** One calendar year of a vehicle's log. */
data class HistoryYear(
    val year: Int,
    /** Minor units, or null when nothing logged that year carries a cost. */
    val total: Long?,
    /** Every entry logged that year, costed or not, newest first. */
    val entries: List<ServiceLogEntry>,
)

data class ServiceHistory(
    /**
     * Minor units, or null when no entry carries a cost. Accumulated as [Long] because a long
     * history can outgrow the [Int] one cost uses.
     */
    val allTime: Long?,
    /**
     * Minor units, or null when nothing is costed or the log spans fewer than
     * [MIN_AVERAGE_SPAN_DAYS].
     */
    val averagePerYear: Long?,
    /** Newest year first. */
    val years: List<HistoryYear>,
)

/**
 * Groups and sums one vehicle's log. The caller passes a single vehicle's entries, which is what
 * [ServiceLogRepository.entriesFor] already returns, so this does no filtering of its own.
 *
 * A null cost means the amount was never recorded, not that the work was free, so the entry is
 * listed but left out of every sum.
 *
 * The average is the cost per month between the first and last logged entry, costed or not, times
 * 12. Months are fractional (a year of 365.25 days over 12), which reduces to
 * `total x 365.25 / days`.
 */
fun serviceHistoryOf(entries: List<ServiceLogEntry>): ServiceHistory {
    val years = entries
        // Stable, so entries sharing a date keep the caller's order, which puts the latest logged first.
        .sortedByDescending { it.date }
        .groupBy { it.date.year }
        .map { (year, logged) -> HistoryYear(year, totalOf(logged), logged) }
    val costedYears = years.filter { it.total != null }
    if (costedYears.isEmpty()) {
        return ServiceHistory(allTime = null, averagePerYear = null, years = years)
    }
    val allTime = costedYears.sumOf { it.total ?: 0L }
    val spanDays = ChronoUnit.DAYS.between(entries.minOf { it.date }, entries.maxOf { it.date })
    val average = if (spanDays < MIN_AVERAGE_SPAN_DAYS) {
        null
    } else {
        roundedHalfUp(allTime * DAYS_IN_FOUR_YEARS, 4 * spanDays)
    }
    return ServiceHistory(allTime = allTime, averagePerYear = average, years = years)
}

private fun totalOf(entries: List<ServiceLogEntry>): Long? {
    val costs = entries.mapNotNull { it.cost }
    return if (costs.isEmpty()) null else costs.sumOf { it.toLong() }
}

/** Costs are never negative, so adding half the divisor rounds half up in integer arithmetic. */
private fun roundedHalfUp(dividend: Long, divisor: Long): Long = (dividend + divisor / 2) / divisor
