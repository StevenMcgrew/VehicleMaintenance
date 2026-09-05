package com.example.vehiclemaintenance.maintenance

import kotlinx.serialization.Serializable

@Serializable
enum class IntervalUnit { DAYS, WEEKS, MONTHS, YEARS }

/** A span of time, used by both the recurrence and the reminder on a maintenance item. */
@Serializable
data class Interval(
    val value: Int,
    val unit: IntervalUnit,
)
