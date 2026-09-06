package com.example.vehiclemaintenance.maintenance

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/** Short enough for a narrow table column: "3/15/27" in en-US. */
fun formatShortDate(date: LocalDate, locale: Locale = Locale.getDefault()): String =
    DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).withLocale(locale).format(date)

/** Unambiguous, for the detail sheet where there is room: "Mar 15, 2027" in en-US. */
fun formatMediumDate(date: LocalDate, locale: Locale = Locale.getDefault()): String =
    DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale).format(date)
