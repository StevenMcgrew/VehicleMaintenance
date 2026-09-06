package com.example.vehiclemaintenance.servicelog

/** One calendar year of spending, derived at read time from that year's log entries. */
data class YearCost(val year: Int, val total: Long)

data class VehicleCostTotals(
    /** Minor units. Accumulated as [Long] because a long history can outgrow the [Int] one cost uses. */
    val allTime: Long,
    /** Newest year first, matching the order the service history already presents. */
    val byYear: List<YearCost>,
)

/**
 * Sums one vehicle's spending. The caller passes a single vehicle's entries, which is what
 * [ServiceLogRepository.entriesFor] already returns, so this does no filtering of its own.
 *
 * A null cost means the amount was never recorded, not that the work was free, so it is left out of
 * every sum and cannot on its own create a year row.
 */
fun costTotalsOf(entries: List<ServiceLogEntry>): VehicleCostTotals {
    val byYear = entries
        .mapNotNull { entry -> entry.cost?.let { entry.date.year to it.toLong() } }
        .groupBy({ it.first }, { it.second })
        .map { (year, costs) -> YearCost(year, costs.sum()) }
        .sortedByDescending { it.year }
    return VehicleCostTotals(allTime = byYear.sumOf { it.total }, byYear = byYear)
}
