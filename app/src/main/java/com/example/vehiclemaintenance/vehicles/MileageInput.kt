package com.example.vehiclemaintenance.vehicles

/** What the owner typed into a mileage field. */
sealed interface MileageInput {
    data object Blank : MileageInput
    data object Invalid : MileageInput
    data class Miles(val value: Int) : MileageInput
}

fun parseMileage(text: String): MileageInput {
    val trimmed = text.trim()
    if (trimmed.isEmpty()) return MileageInput.Blank
    val miles = trimmed.toIntOrNull()
    return if (miles == null || miles < 0) MileageInput.Invalid else MileageInput.Miles(miles)
}

/**
 * A lower reading is allowed, since it is how a typo or a replaced odometer gets corrected, but it
 * walks the mileage check backwards, so the owner confirms it first.
 */
fun needsLowerMileageWarning(miles: Int, highestKnown: Int?): Boolean =
    highestKnown != null && miles < highestKnown
