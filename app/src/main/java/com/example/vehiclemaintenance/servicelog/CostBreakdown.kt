package com.example.vehiclemaintenance.servicelog

import java.util.Locale

/** One slice of a vehicle's all-time spend, for the cost breakdown pie chart. */
data class CostSlice(
    /** The newest grouped entry's description, trimmed; null for the Additional repairs slice. */
    val name: String?,
    /** Minor units, always greater than zero. */
    val total: Long,
    /** Whole percent of the breakdown's sum, rounded half up; 0 means more than 0 but under 1%. */
    val percent: Int,
)

/**
 * Splits one vehicle's spend into a slice per tracked maintenance item and one Additional repairs
 * slice, largest first with Additional repairs last.
 *
 * Tracked entries are grouped by description, ignoring case and extra spaces, so every oil change
 * lands in one slice. Entries with no item link are ad-hoc repairs, which includes the history of a
 * deleted item: deleting one clears the link and keeps its entries as repairs. A null cost was
 * never recorded and a zero cost has no area to draw, so both are left out.
 */
fun costBreakdownOf(entries: List<ServiceLogEntry>): List<CostSlice> {
    val costed = entries.filter { (it.cost ?: 0) > 0 }
    val (tracked, additional) = costed.partition { it.maintenanceItemId != null }
    val trackedTotals = tracked
        .groupBy { groupKey(it.description) }
        .values
        .map { group ->
            // Stable, so entries sharing a date keep the caller's order and the first one names the slice.
            val newest = group.sortedByDescending { it.date }.first()
            newest.description.trim() to group.sumOf { it.cost?.toLong() ?: 0L }
        }
        .sortedWith(
            compareByDescending<Pair<String, Long>> { it.second }
                .thenBy(String.CASE_INSENSITIVE_ORDER) { it.first },
        )
    val additionalTotal = additional.sumOf { it.cost?.toLong() ?: 0L }
    val totals: List<Pair<String?, Long>> =
        if (additionalTotal > 0) trackedTotals + (null to additionalTotal) else trackedTotals
    val sum = totals.sumOf { it.second }
    return totals.map { (name, total) ->
        CostSlice(name, total, ((total * 100 + sum / 2) / sum).toInt())
    }
}

/**
 * Each slice's color, in breakdown order: tracked slices cycle through [palette], and the
 * Additional repairs slice (the null name, always last) takes [additionalRepairs]. Shared by the
 * screen and the PDF so both color a vehicle's chart the same way.
 */
fun <C> sliceColors(names: List<String?>, palette: List<C>, additionalRepairs: C): List<C> {
    val hasAdditionalRepairs = names.isNotEmpty() && names.last() == null
    val trackedCount = if (hasAdditionalRepairs) names.size - 1 else names.size
    return sliceColorIndices(trackedCount, hasAdditionalRepairs, palette.size)
        .map { if (it == ADDITIONAL_REPAIRS_SLOT) additionalRepairs else palette[it] }
}

/**
 * The palette slot for each slice in breakdown order: tracked slices cycle through [paletteSize]
 * slots, and the Additional repairs slice, when there is one, takes [ADDITIONAL_REPAIRS_SLOT]. The
 * last tracked slice moves to slot 1 when it would otherwise share slot 0 with the first slice it
 * touches around the circle.
 */
fun sliceColorIndices(trackedCount: Int, hasAdditionalRepairs: Boolean, paletteSize: Int): List<Int> {
    val tracked = List(trackedCount) { it % paletteSize }.toMutableList()
    val lastTouchesFirst = !hasAdditionalRepairs && trackedCount > 1
    if (lastTouchesFirst && tracked.last() == 0) tracked[tracked.lastIndex] = 1
    return if (hasAdditionalRepairs) tracked + ADDITIONAL_REPAIRS_SLOT else tracked
}

/** Stands for the neutral Additional repairs color, which is outside the cycling palette. */
const val ADDITIONAL_REPAIRS_SLOT = -1

private val WHITESPACE = Regex("\\s+")

private fun groupKey(description: String): String =
    description.trim().replace(WHITESPACE, " ").lowercase(Locale.ROOT)
