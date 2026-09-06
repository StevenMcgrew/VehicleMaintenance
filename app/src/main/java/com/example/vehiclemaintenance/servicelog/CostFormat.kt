package com.example.vehiclemaintenance.servicelog

import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

/**
 * Renders the stored minor units as money without ever passing through a floating point type, which
 * would reintroduce the rounding the integer storage exists to avoid.
 */
fun formatCost(minorUnits: Long, locale: Locale = Locale.getDefault()): String =
    NumberFormat.getCurrencyInstance(locale).format(BigDecimal.valueOf(minorUnits, 2))

fun formatCost(minorUnits: Int, locale: Locale = Locale.getDefault()): String =
    formatCost(minorUnits.toLong(), locale)
