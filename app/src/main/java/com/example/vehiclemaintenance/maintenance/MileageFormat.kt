package com.example.vehiclemaintenance.maintenance

import java.text.NumberFormat
import java.util.Locale

/** Grouped so a five figure interval is readable at a glance in a narrow column: 5000 -> "5,000". */
fun formatMileage(miles: Int, locale: Locale = Locale.getDefault()): String =
    NumberFormat.getIntegerInstance(locale).format(miles)
